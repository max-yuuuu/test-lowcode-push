package com.scaffold.framework.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Minimal fail-closed runtime for packaged data-only manifests. Database nodes await project policy adapters. */
public final class PackagedFlowRuntime {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_NODES = 100;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_PAYLOAD_BYTES = 1_048_576;
    private static final int MAX_VALUE_DEPTH = 64;
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);
    private final FlowManifestLoader loader;

    public PackagedFlowRuntime(FlowManifestLoader loader) { this.loader = Objects.requireNonNull(loader); }

    /** Executes a packaged snapshot only when it matches the server-derived operation context. */
    public JsonNode execute(FlowManifestLoader.Reference reference, FlowExecutionContext context,
                            BooleanSupplier cancelled) {
        Objects.requireNonNull(reference);
        Objects.requireNonNull(context);
        Objects.requireNonNull(cancelled);
        if (context.subject() == null || context.subject().userId() == null
                || context.subject().tenantId() == null || context.subject().tenantId() != context.tenantId()
                || context.operationPermission() == null || context.operationPermission().isBlank()
                || !context.permissions().contains(context.operationPermission()) || context.dataScope() == null
                || context.traceId() == null || context.traceId().isBlank()
                || reference.projectId() != context.projectId() || reference.modelId() != context.modelId()
                || reference.operationId() != context.operationId()
                || reference.operationVersion() != context.operationVersion()) {
            fail("FLOW_BINDING_MISMATCH");
        }
        Instant now = Instant.now();
        if (context.deadline() == null || !context.deadline().isAfter(now)) fail("FLOW_DEADLINE");
        Duration remaining = Duration.between(now, context.deadline());
        return execute(reference, JSON.valueToTree(context.input()), remaining, cancelled);
    }

    public JsonNode execute(FlowManifestLoader.Reference reference, JsonNode input, Duration timeout, BooleanSupplier cancelled) {
        Objects.requireNonNull(input); Objects.requireNonNull(timeout); Objects.requireNonNull(cancelled);
        if (timeout.compareTo(MAX_TIMEOUT) > 0) fail("FLOW_BUDGET");
        Instant deadline = Instant.now().plus(timeout);
        if (input.toString().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) fail("FLOW_BUDGET");
        validateJsonDepth(input);
        JsonNode graph = loader.load(reference);
        validateJsonDepth(graph);
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Set<String>> predecessors = new HashMap<>();
        for (JsonNode node : graph.path("nodes")) {
            String id = node.path("id").asText();
            if (nodes.putIfAbsent(id, node) != null) fail("FLOW_INVALID_GRAPH");
            outgoing.put(id, new ArrayList<>()); incoming.put(id, 0); predecessors.put(id, new LinkedHashSet<>());
            validateNode(node);
        }
        for (JsonNode edge : graph.path("edges")) {
            String from = edge.path("from").asText(), to = edge.path("to").asText();
            if (!nodes.containsKey(from) || !nodes.containsKey(to)) fail("FLOW_INVALID_GRAPH");
            outgoing.get(from).add(to); predecessors.get(to).add(from); incoming.merge(to, 1, Integer::sum);
        }
        List<String> roots = incoming.entrySet().stream().filter(e -> e.getValue() == 0).map(Map.Entry::getKey).toList();
        if (roots.size() != 1 || !"start".equals(nodes.get(roots.get(0)).path("kind").asText())) fail("FLOW_INVALID_GRAPH");
        List<String> order = topological(nodes.keySet(), outgoing, incoming);
        if (order.size() != nodes.size()) fail("FLOW_INVALID_GRAPH");
        Map<String, Set<String>> dominators = dominators(order, predecessors, roots.get(0));
        long ends = nodes.values().stream().filter(n -> "end".equals(n.path("kind").asText())).count();
        if (ends == 0) fail("FLOW_INVALID_GRAPH");
        for (JsonNode node : nodes.values()) {
            String id = node.path("id").asText();
            validateEdges(node, outgoing.get(id));
            validateNodeReferences(node.path("config"), id, nodes.keySet(), dominators);
        }

        Map<String, Object> results = new LinkedHashMap<>();
        Set<String> active = new HashSet<>(roots);
        Map<String, Integer> activeDepth = new HashMap<>();
        activeDepth.put(roots.get(0), 1);
        int executed = 0;
        for (String id : order) {
            check(cancelled, deadline);
            if (!active.contains(id)) continue;
            if (++executed > MAX_NODES) fail("FLOW_BUDGET");
            JsonNode node = nodes.get(id);
            String kind = node.path("kind").asText();
            Map<String, Object> output = switch (kind) {
                case "start" -> Map.of("accepted", true);
                case "transform" -> mapValues(node.path("config").path("fields"), input, results);
                case "validate" -> validate(node.path("config").path("rules"), input, results);
                case "condition" -> Map.of("matched", compare(node.path("config"), input, results));
                case "end" -> mapValues(node.path("config").path("output"), input, results);
                default -> throw new FlowExecutionException("FLOW_UNSUPPORTED_NODE");
            };
            results.put(id, output);
            if (payloadBytes(results) > MAX_PAYLOAD_BYTES) fail("FLOW_BUDGET");
            List<String> next = outgoing.get(id);
            if ("condition".equals(kind)) {
                String target = node.path("config").path(Boolean.TRUE.equals(output.get("matched")) ? "then" : "otherwise").asText();
                if (!next.contains(target)) fail("FLOW_INVALID_GRAPH");
                next = List.of(target);
            }
            active.addAll(next);
            int nextDepth = activeDepth.get(id) + 1;
            if (nextDepth > MAX_DEPTH) fail("FLOW_BUDGET");
            for (String target : next) activeDepth.merge(target, nextDepth, Math::max);
        }
        check(cancelled, deadline);
        ObjectNode result = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        for (String id : order) if (active.contains(id) && "end".equals(nodes.get(id).path("kind").asText())) {
            JsonNode out = nodes.get(id).path("config").path("output");
            out.fields().forEachRemaining(field -> result.set(field.getKey(), JSON.valueToTree(resolve(field.getValue(), input, results))));
        }
        if (payloadBytes(result) > MAX_PAYLOAD_BYTES) fail("FLOW_BUDGET");
        return result;
    }

    private record DepthNode(JsonNode node, int depth) {}

    private static void validateJsonDepth(JsonNode root) {
        ArrayDeque<DepthNode> pending = new ArrayDeque<>();
        pending.push(new DepthNode(root, 0));
        while (!pending.isEmpty()) {
            DepthNode current = pending.pop();
            if (current.depth() > MAX_VALUE_DEPTH) fail("FLOW_BUDGET");
            if (current.node().isContainerNode()) {
                current.node().elements().forEachRemaining(child -> pending.push(new DepthNode(child, current.depth() + 1)));
            }
        }
    }

    private static int payloadBytes(Object value) {
        try { return JSON.writeValueAsBytes(value).length; }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { fail("FLOW_INVALID_VALUE"); return 0; }
    }

    private static void validateNode(JsonNode n) {
        String kind = n.path("kind").asText(); int version = n.path("version").asInt(-1); JsonNode c = n.path("config");
        if (version != 1 || !c.isObject()) fail("FLOW_INVALID_GRAPH");
        switch (kind) {
            case "start" -> { if (!c.isEmpty()) fail("FLOW_INVALID_GRAPH"); }
            case "end" -> {
                if (c.size() > 1 || (c.has("output") && !c.path("output").isObject())) fail("FLOW_INVALID_GRAPH");
                if (c.has("output")) validateMappings(c.path("output"));
            }
            case "transform" -> { if (!(c.isEmpty() || only(c, "fields")) || (c.has("fields") && !c.path("fields").isObject())) fail("FLOW_INVALID_GRAPH"); if (c.has("fields")) validateMappings(c.path("fields")); }
            case "validate" -> {
                if (!only(c, "rules") || !c.path("rules").isArray() || c.path("rules").isEmpty()) fail("FLOW_INVALID_GRAPH");
                validateRules(c.path("rules"));
            }
            case "condition" -> { if (!only(c,"left","operator","right","then","otherwise") || !Set.of("eq","ne","gt","gte","lt","lte","in").contains(c.path("operator").asText()) || c.path("then").asText().equals(c.path("otherwise").asText())) fail("FLOW_INVALID_GRAPH"); validateReference(c.path("left")); validateReference(c.path("right")); }
            case "query", "write", "operation", "audit", "error" -> fail("FLOW_UNSUPPORTED_NODE");
            default -> fail("FLOW_UNSUPPORTED_NODE");
        }
    }

    private static void validateRules(JsonNode rules) {
        for (JsonNode rule : rules) {
            if (!rule.isObject() || (rule.size() != 3 && rule.size() != 4)
                    || !rule.has("value") || !rule.has("operator") || !rule.has("message")
                    || !rule.path("message").isTextual() || rule.path("message").asText().length() > 256) {
                fail("FLOW_INVALID_GRAPH");
            }
            String operator = rule.path("operator").asText();
            if (!Set.of("required", "eq", "ne", "gt", "gte", "lt", "lte").contains(operator)) {
                fail("FLOW_INVALID_GRAPH");
            }
            validateReference(rule.path("value"));
            if (rule.has("compare")) validateReference(rule.path("compare"));
            if (!"required".equals(operator) && !rule.has("compare")) fail("FLOW_INVALID_GRAPH");
        }
    }

    private static void validateEdges(JsonNode node, List<String> edges) {
        String kind = node.path("kind").asText();
        if ("condition".equals(kind)) {
            if (edges.size()!=2 || !edges.contains(node.path("config").path("then").asText()) || !edges.contains(node.path("config").path("otherwise").asText())) fail("FLOW_INVALID_GRAPH");
        } else if ("end".equals(kind) ? !edges.isEmpty() : edges.size()!=1) fail("FLOW_INVALID_GRAPH");
    }

    private static Map<String, Set<String>> dominators(List<String> order, Map<String, Set<String>> predecessors, String root) {
        Set<String> all = new LinkedHashSet<>(order);
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String id : order) result.put(id, id.equals(root) ? new LinkedHashSet<>(Set.of(root)) : new LinkedHashSet<>(all));
        boolean changed;
        do {
            changed = false;
            for (String id : order) {
                if (id.equals(root)) continue;
                Set<String> next = null;
                for (String predecessor : predecessors.get(id)) {
                    if (next == null) next = new LinkedHashSet<>(result.get(predecessor));
                    else next.retainAll(result.get(predecessor));
                }
                if (next == null) fail("FLOW_INVALID_GRAPH");
                next.add(id);
                if (!next.equals(result.get(id))) { result.put(id, next); changed = true; }
            }
        } while (changed);
        return result;
    }

    private static void validateNodeReferences(JsonNode value, String target, Set<String> nodeIds,
                                               Map<String, Set<String>> dominators) {
        if (value.isObject()) {
            if (value.has("source")) {
                if ("node".equals(value.path("source").asText())) {
                    String source = value.path("nodeId").asText();
                    if (!nodeIds.contains(source) || source.equals(target) || !dominators.get(target).contains(source)) {
                        fail("FLOW_INVALID_GRAPH");
                    }
                }
                return;
            }
            value.elements().forEachRemaining(child -> validateNodeReferences(child, target, nodeIds, dominators));
        } else if (value.isArray()) {
            value.elements().forEachRemaining(child -> validateNodeReferences(child, target, nodeIds, dominators));
        }
    }

    private static List<String> topological(Set<String> ids, Map<String,List<String>> out, Map<String,Integer> indegree) {
        Map<String,Integer> d = new HashMap<>(indegree); ArrayDeque<String> q = new ArrayDeque<>();
        d.forEach((id,n)->{if(n==0)q.add(id);}); List<String> order = new ArrayList<>();
        while(!q.isEmpty()){String id=q.remove();order.add(id);for(String to:out.get(id))if(d.compute(to,(k,v)->v-1)==0)q.add(to);}
        return order;
    }

    private static Map<String,Object> validate(JsonNode rules, JsonNode input, Map<String,Object> results) {
        for(JsonNode rule:rules){if(!only(rule,"value","operator","compare","message"))fail("FLOW_INVALID_GRAPH");Object v=resolve(rule.path("value"),input,results);String op=rule.path("operator").asText();boolean ok;
            if("required".equals(op))ok=v!=null&&(!(v instanceof String s)||!s.isBlank());else{Object b=resolve(rule.path("compare"),input,results);ok=compareValues(v,b,op);}
            if(!ok)throw new FlowExecutionException("FLOW_VALIDATION_FAILED");}return Map.of("valid",true);
    }
    private static boolean compare(JsonNode c,JsonNode input,Map<String,Object> results){return compareValues(resolve(c.path("left"),input,results),resolve(c.path("right"),input,results),c.path("operator").asText());}
    private static boolean compareValues(Object a,Object b,String op){return switch(op){case "eq"->Objects.equals(a,b);case "ne"->!Objects.equals(a,b);case "gt","gte","lt","lte"->{if(!(a instanceof Number)||!(b instanceof Number))fail("FLOW_INVALID_VALUE");int n=new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()));yield switch(op){case "gt"->n>0;case "gte"->n>=0;case "lt"->n<0;default->n<=0;};}case "in"->b instanceof Collection<?> c&&c.contains(a);default->false;};}
    private static Map<String,Object> mapValues(JsonNode fields,JsonNode input,Map<String,Object> results){Map<String,Object> out=new LinkedHashMap<>();fields.fields().forEachRemaining(e->out.put(e.getKey(),resolve(e.getValue(),input,results)));return out;}
    private static void validateMappings(JsonNode fields){fields.fields().forEachRemaining(e->validateReference(e.getValue()));}
    private static void validateReference(JsonNode r){String source=r.path("source").asText();if("input".equals(source)){if(!only(r,"source","path")||!r.path("path").asText().matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*"))fail("FLOW_INVALID_GRAPH");}else if("literal".equals(source)){if(!only(r,"source","value")||r.path("value").isMissingNode())fail("FLOW_INVALID_GRAPH");}else if("node".equals(source)){if(!only(r,"source","nodeId","path")||r.path("nodeId").asText().isBlank()||!r.path("path").asText().matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*"))fail("FLOW_INVALID_GRAPH");}else fail("FLOW_INVALID_GRAPH");}
    private static Object resolve(JsonNode r,JsonNode input,Map<String,Object> results){validateReference(r);JsonNode root=input;if("literal".equals(r.path("source").asText()))return plain(r.path("value"));if("node".equals(r.path("source").asText())){Object value=results.get(r.path("nodeId").asText());for(String part:r.path("path").asText().split("\\.")){if(!(value instanceof Map<?,?> m)||!m.containsKey(part))fail("FLOW_INVALID_GRAPH");value=((Map<?,?>)value).get(part);}return value;}for(String part:r.path("path").asText().split("\\.")){if(!root.isObject()||!root.has(part))fail("FLOW_INVALID_GRAPH");root=root.path(part);}return plain(root);}
    private static Object plain(JsonNode n){if(n.isNull())return null;if(n.isTextual())return n.asText();if(n.isBoolean())return n.asBoolean();if(n.isNumber())return n.numberValue();if(n.isArray()){List<Object> l=new ArrayList<>();n.forEach(x->l.add(plain(x)));return l;}if(n.isObject())return jsonMap(n);return null;}
    private static Map<String,Object> jsonMap(JsonNode n){Map<String,Object> m=new LinkedHashMap<>();n.fields().forEachRemaining(e->m.put(e.getKey(),plain(e.getValue())));return m;}
    private static boolean only(JsonNode n,String...keys){if(!n.isObject()||n.size()!=keys.length)return false;for(String k:keys)if(!n.has(k))return false;return true;}
    private static void check(BooleanSupplier cancel,Instant deadline){if(cancel.getAsBoolean())fail("FLOW_CANCELLED");if(!Instant.now().isBefore(deadline))fail("FLOW_DEADLINE");}
    private static void fail(String code){throw new FlowExecutionException(code);}
    public static final class FlowExecutionException extends IllegalStateException {public FlowExecutionException(String code){super(code);}}
}

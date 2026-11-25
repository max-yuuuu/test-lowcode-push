package com.scaffold.flow.runtime;

import java.util.*;

/** Canonical allowlist for the Flow protocol node kinds and node-owned fields. */
public final class FlowNodeRegistry {
    private static final Set<String> COMMON_FIELDS = Set.of("id", "kind", "next");
    private static final Map<String, NodeDefinition> DEFINITIONS;

    static {
        Map<String, NodeDefinition> definitions = new LinkedHashMap<>();
        register(definitions, "input");
        register(definitions, "validate");
        register(definitions, "map", "assignments");
        register(definitions, "condition", "when", "then", "else");
        register(definitions, "query", "modelId", "fields", "filters", "limit");
        register(definitions, "write", "modelId", "mode", "values");
        register(definitions, "operation", "operationCode", "version", "modelId", "inputBindings", "valueBindings");
        register(definitions, "audit");
        register(definitions, "error", "code", "message");
        register(definitions, "end", "valueBindings");
        DEFINITIONS = Collections.unmodifiableMap(definitions);
    }

    private FlowNodeRegistry() { }

    public static Set<String> kinds() { return DEFINITIONS.keySet(); }

    /** Fields common to every node; kind-specific fields are exposed by allowsField. */
    public static Set<String> allowedNodeFields() { return COMMON_FIELDS; }

    public static boolean supports(String kind) { return DEFINITIONS.containsKey(kind); }

    public static boolean allowsField(String kind, String field) {
        NodeDefinition definition = DEFINITIONS.get(kind);
        return definition != null && definition.allowedFields().contains(field);
    }

    private static void register(Map<String, NodeDefinition> definitions, String kind, String... extras) {
        Set<String> fields = new LinkedHashSet<>(COMMON_FIELDS);
        fields.addAll(List.of(extras));
        definitions.put(kind, new NodeDefinition(Set.copyOf(fields)));
    }

    private record NodeDefinition(Set<String> allowedFields) { }
}

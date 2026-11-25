package com.scaffold.flow.runtime;

import java.util.*;

/** 首版受限 Flow 文档的协议与图结构校验器；未知字段/节点一律 fail-closed。 */
public final class FlowDocumentValidator {
    public static final int SCHEMA_VERSION = 1, MAX_NODES = 100, MAX_NODE_ID_LENGTH = 256, MAX_DOCUMENT_CHARS = 256_000, MAX_DEPTH = 32;
    private FlowDocumentValidator() {}

    public static void validate(Map<String, Object> document) {
        if (document == null || !Objects.equals(document.get("schemaVersion"), SCHEMA_VERSION)) fail("不支持的 Flow schema 版本");
        enforceDocumentBudget(document);
        if (!document.keySet().containsAll(Set.of("schemaVersion", "start", "nodes")) || !Set.of("schemaVersion", "start", "nodes", "variables").containsAll(document.keySet())) fail("Flow 文档含未知或缺失字段");
        Object startValue = document.get("start");
        Object nodesValue = document.get("nodes");
        if (!(startValue instanceof String) || !(nodesValue instanceof List<?>)) fail("Flow 起始节点或节点列表无效");
        String start = (String) startValue;
        List<?> nodes = (List<?>) nodesValue;
        if (nodes.isEmpty() || nodes.size() > MAX_NODES) fail("Flow 节点为空或超过上限");
        Map<String, Map<String,Object>> byId = new LinkedHashMap<>();
        for (Object raw : nodes) {
            if (!(raw instanceof Map<?,?> map)) fail("节点格式无效");
            Map<String,Object> node = (Map<String,Object>) raw;
            Object idValue = node.get("id");
            String id = idValue instanceof String value && validNodeId(value) ? value : null;
            if (id == null || !(node.get("kind") instanceof String kind) || !FlowNodeRegistry.supports(kind)) fail("节点标识或类型无效", id);
            String kind = (String) node.get("kind");
            if (node.keySet().stream().anyMatch(field -> !FlowNodeRegistry.allowsField(kind, field))) fail("节点含未登记字段", id);
            validateNodeFields(node, kind, id);
            if (byId.putIfAbsent(id, node) != null) fail("节点标识重复", id);
        }
        FlowVariables.validateNodes(nodes, FlowVariables.definitions(document));
        if (!byId.containsKey(start)) fail("起始节点不存在");
        for (Map<String,Object> node : byId.values()) {
            for (String edge : List.of("next", "then", "else")) {
                if (node.containsKey(edge) && (!(node.get(edge) instanceof String target) || !byId.containsKey(target))) fail("节点引用不存在", (String) node.get("id"));
            }
        }
        // 分支图遍历：next 与 condition 的 then/else 都是出边；仍禁止环路并要求全部可达
        Map<String, Integer> color = new HashMap<>();
        dfs(start, byId, color);
        if (color.size() != byId.size()) {
            String unreachable = byId.keySet().stream().filter(id -> !Integer.valueOf(2).equals(color.get(id))).findFirst().orElse(null);
            fail("Flow 含不可达节点", unreachable);
        }
        if (byId.values().stream().noneMatch(n -> "end".equals(n.get("kind")) && !n.containsKey("next"))) fail("Flow 缺少可达结束节点");
    }
    private static void dfs(String id, Map<String, Map<String,Object>> byId, Map<String, Integer> color) {
        Integer state = color.get(id);
        if (state != null && state == 1) fail("Flow 不允许环路", id);
        if (state != null) return;
        color.put(id, 1);
        Map<String,Object> node = byId.get(id);
        if (node != null) {
            for (String edge : List.of("next", "then", "else")) {
                if (node.get(edge) instanceof String target && byId.containsKey(target)) dfs(target, byId, color);
            }
        }
        color.put(id, 2);
    }

    private static void validateNodeFields(Map<String, Object> node, String kind, String id) {
        if ("error".equals(kind)) {
            Object message = node.get("message");
            if (!(message instanceof String text) || text.isBlank() || text.length() > 200) fail("error 节点 message 必填且不超过 200 字", id);
            if (node.containsKey("code")) {
                Object code = node.get("code");
                if (!(code instanceof String value) || !value.matches("[A-Z][A-Z0-9_]{0,63}")) fail("error 节点 code 必须是大写字母开头的稳定标识", id);
            }
            if (node.containsKey("next") || node.containsKey("then") || node.containsKey("else")) fail("error 节点是终止节点，不接受出边", id);
            return;
        }
        if ("condition".equals(kind)) {
            Object then = node.get("then");
            Object otherwise = node.get("else");
            if (!(then instanceof String thenId) || thenId.isBlank() || thenId.length() > MAX_NODE_ID_LENGTH) fail("condition 节点缺少 then 分支", id);
            if (!(otherwise instanceof String elseId) || elseId.isBlank() || elseId.length() > MAX_NODE_ID_LENGTH) fail("condition 节点缺少 else 分支", id);
            if (node.containsKey("next")) fail("condition 节点使用 then/else，不接受 next", id);
            Object whenValue = node.get("when");
            if (!(whenValue instanceof Map<?, ?> when) || !Set.of("left", "operator", "right").equals(when.keySet())) fail("condition 节点 when 形状无效", id);
            Object operator = ((Map<?, ?>) whenValue).get("operator");
            if (!(operator instanceof String op) || !Set.of("eq", "neq", "gt", "gte", "lt", "lte").contains(op)) fail("condition 节点 operator 只允许 eq/neq/gt/gte/lt/lte", id);
            return;
        }
        if ("query".equals(kind)) {
            validateModelId(node, id);
            validateFields(node.get("fields"), id);
            validateFilters(node.get("filters"), id);
            if (node.containsKey("limit")) {
                Object limit = node.get("limit");
                if (!(limit instanceof Number number) || number.intValue() < 1 || number.intValue() > 100
                        || number.intValue() != number.doubleValue()) fail("query 节点 limit 必须是 1 到 100 的整数", id);
            }
            return;
        }
        if ("write".equals(kind)) {
            validateModelId(node, id);
            Object mode = node.get("mode");
            if (!(mode instanceof String text) || !(text.equals("insert") || text.equals("update"))) {
                fail("write 节点 mode 只允许 insert 或 update", id);
            }
            validateValues(node.get("values"), id);
            return;
        }
        if (!"operation".equals(kind)) return;
        if (node.containsKey("modelId")) validateModelId(node, id);
        Object code = node.get("operationCode");
        if (!(code instanceof String text) || text.isBlank() || text.length() > 128
                || !text.matches("[A-Za-z_][A-Za-z0-9_.:-]*")) {
            fail("operation 节点 operationCode 无效", id);
        }
        if (node.containsKey("version")) {
            Object version = node.get("version");
            if (!(version instanceof Number number) || number.longValue() <= 0
                    || number.longValue() > Integer.MAX_VALUE || number.longValue() != number.doubleValue()) {
                fail("operation 节点 version 无效", id);
            }
        }
        if (node.containsKey("inputBindings")) {
            Object bindings = node.get("inputBindings");
            if (!(bindings instanceof Map<?, ?>)) fail("operation 节点 inputBindings 无效", id);
            Map<?, ?> map = (Map<?, ?>) bindings;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String target) || target.isBlank()
                        || !(entry.getValue() instanceof String source) || source.isBlank()) {
                    fail("operation 节点 inputBindings 必须是字符串到字符串", id);
                }
            }
        }
    }

    private static void validateModelId(Map<String, Object> node, String id) {
        Object modelId = node.get("modelId");
        if (!(modelId instanceof Number number) || number.longValue() <= 0 || number.longValue() != number.doubleValue()) {
            fail("数据节点 modelId 必须是正整数", id);
        }
    }

    private static void validateFields(Object raw, String id) {
        if (!(raw instanceof List<?> fields) || fields.isEmpty() || fields.size() > 64
                || fields.stream().anyMatch(field -> !(field instanceof String text) || !isIdentifier(text))) {
            fail("query 节点 fields 必须是安全字段名列表", id);
        }
    }

    private static void validateFilters(Object raw, String id) {
        if (!(raw instanceof Map<?, ?> filters) || filters.size() > 32
                || filters.keySet().stream().anyMatch(key -> !(key instanceof String text) || !isIdentifier(text))) {
            fail("query 节点 filters 必须是安全字段名对象", id);
        }
    }

    private static void validateValues(Object raw, String id) {
        if (!(raw instanceof Map<?, ?> values) || values.isEmpty() || values.size() > 64
                || values.keySet().stream().anyMatch(key -> !(key instanceof String text) || !isIdentifier(text))) {
            fail("write 节点 values 必须是安全字段名对象", id);
        }
    }

    private static boolean isIdentifier(String text) {
        return text != null && text.length() <= 64 && text.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    public static void enforceDocumentBudget(Object value) {
        record Entry(Object value, int depth, boolean variableDefinitionsList, boolean variableDefinition, boolean allowNull) { }
        Deque<Entry> pending = new ArrayDeque<>();
        pending.push(new Entry(value, 1, false, false, false));
        int chars = 0;
        int visited = 0;
        while (!pending.isEmpty()) {
            Entry entry = pending.pop();
            Object current = entry.value();
            if (++visited > MAX_NODES * MAX_DEPTH) fail("Flow 文档元素数量超过上限");
            if (current == null) {
                if (!entry.allowNull()) fail("Flow 文档不允许 null 值");
                chars += 4;
                continue;
            }
            if (current instanceof Boolean || current instanceof Number) { chars += String.valueOf(current).length(); }
            else if (current instanceof String text) { chars += text.length(); }
            else if (current instanceof Map<?,?> map) {
                chars += 2;
                for (var item : map.entrySet()) {
                    if (!(item.getKey() instanceof String)) fail("Flow 对象键必须是字符串");
                    String key = (String) item.getKey();
                    chars += key.length() + 2;
                    int childDepth = entry.depth() + 1;
                    if (childDepth > MAX_DEPTH) fail("Flow 文档嵌套深度超过上限");
                    boolean variableDefinitionsList = entry.depth() == 1 && "variables".equals(key);
                    boolean variableDefault = entry.variableDefinition() && "defaultValue".equals(key)
                            && (Boolean.TRUE.equals(map.get("nullable"))
                            || map.get("type") instanceof Map<?, ?> type && "null".equals(type.get("type")));
                    pending.push(new Entry(item.getValue(), childDepth, variableDefinitionsList, entry.variableDefinition(), variableDefault));
                }
            } else if (current instanceof List<?> list) {
                chars += 2;
                for (Object item : list) {
                    if (item == null || entry.variableDefinitionsList() && !(item instanceof Map<?, ?>)) fail("Flow 文档不允许 null 值或无效变量定义");
                    int childDepth = entry.depth() + 1;
                    if (childDepth > MAX_DEPTH) fail("Flow 文档嵌套深度超过上限");
                    pending.push(new Entry(item, childDepth, false, entry.variableDefinitionsList(), false));
                }
            } else fail("Flow 文档包含不支持的数据类型");
            if (chars > MAX_DOCUMENT_CHARS) fail("Flow 文档超过大小上限");
        }
    }
    private static boolean validNodeId(String id) {
        return id != null && !id.isBlank() && id.length() <= MAX_NODE_ID_LENGTH;
    }

    private static void fail(String message) { throw new ValidationException(message); }
    private static void fail(String message, String nodeId) { throw new ValidationException(message, nodeId); }

    public static final class ValidationException extends IllegalArgumentException {
        private final List<String> nodePath;

        public ValidationException(String message) {
            this(message, null);
        }

        public ValidationException(String message, String nodeId) {
            super(message);
            this.nodePath = validNodeId(nodeId) ? List.of(nodeId) : List.of();
        }

        public List<String> nodePath() { return nodePath; }
    }
}

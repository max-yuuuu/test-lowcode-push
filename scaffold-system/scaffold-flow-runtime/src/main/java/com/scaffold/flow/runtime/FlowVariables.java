package com.scaffold.flow.runtime;

import java.util.*;

/** 请求局部标量变量与安全取值；不执行表达式，不把局部状态混入业务输出。 */
final class FlowVariables {
    private FlowVariables() {}
    static Map<String, Map<String, Object>> definitions(Map<String, Object> document) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (!document.containsKey("variables")) return result;
        if (!(document.get("variables") instanceof List<?> list) || list.size() > 64) invalid("流程变量必须是列表且最多 64 个");
        Set<String> codes = new HashSet<>();
        for (Object raw : (List<?>) document.get("variables")) {
            if (!(raw instanceof Map<?, ?>)) invalid("变量格式无效");
            Map<String, Object> v = (Map<String, Object>) raw;
            if (!v.keySet().containsAll(Set.of("id", "code", "label", "type", "nullable", "defaultValue")) || !Set.of("id", "code", "label", "description", "type", "nullable", "defaultValue").containsAll(v.keySet())) invalid("变量含未知或缺失字段");
            if (!(v.get("id") instanceof String id) || id.isBlank() || id.length() > 256) invalid("变量身份无效");
            if (!(v.get("code") instanceof String code) || code.length() > 64 || !code.matches("[A-Za-z_][A-Za-z0-9_]*") || !codes.add(code)) invalid("变量编码无效或重复");
            if (!(v.get("label") instanceof String label) || label.isBlank() || label.length() > 256 || (v.containsKey("description") && !(v.get("description") instanceof String))) invalid("变量显示名或说明无效");
            if (!(v.get("nullable") instanceof Boolean) || !(v.get("type") instanceof Map<?, ?> type) || !Set.of("type", "nullable").containsAll(type.keySet()) || !(type.get("type") instanceof String) || !Set.of("string", "number", "boolean", "null").contains(type.get("type")) || (type.containsKey("nullable") && !Objects.equals(type.get("nullable"), v.get("nullable")))) invalid("变量只支持标量类型");
            if (result.putIfAbsent((String) v.get("id"), v) != null) invalid("变量身份重复");
            if (!accepts(v, v.get("defaultValue"))) invalid("变量默认值与类型不符");
        }
        return result;
    }
    static boolean accepts(Map<String, Object> definition, Object value) {
        String type = String.valueOf(((Map<?, ?>) definition.get("type")).get("type"));
        if (value == null) return Boolean.TRUE.equals(definition.get("nullable")) || "null".equals(type);
        return switch (type) {
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number n && Double.isFinite(n.doubleValue());
            case "boolean" -> value instanceof Boolean;
            default -> false;
        };
    }
    static void validateNodes(List<?> nodes, Map<String, Map<String, Object>> definitions) {
        for (Object raw : nodes) {
            Map<String, Object> node = (Map<String, Object>) raw;
            try {
                if (node.containsKey("assignments")) {
                    if (!(node.get("assignments") instanceof List<?> list) || list.size() > 64) invalid("赋值列表无效");
                    for (Object assignment : (List<?>) node.get("assignments")) {
                        if (!(assignment instanceof Map<?, ?> map) || !map.keySet().equals(Set.of("variableId", "value")) || !definitions.containsKey(map.get("variableId"))) invalid("赋值目标变量不存在");
                        validateRef(((Map<?, ?>) assignment).get("value"), definitions);
                    }
                }
                if ("condition".equals(node.get("kind")) && node.get("when") instanceof Map<?, ?> when) {
                    validateRef(when.get("left"), definitions);
                    validateRef(when.get("right"), definitions);
                }
                if (node.containsKey("valueBindings")) {
                    if (node.containsKey("inputBindings")) invalid("接口参数不能同时使用两种绑定");
                    if (!(node.get("valueBindings") instanceof Map<?, ?> bindings) || bindings.size() > 64) invalid("取值绑定无效");
                    for (var entry : ((Map<?, ?>) node.get("valueBindings")).entrySet()) {
                        if (!(entry.getKey() instanceof String key) || !key.matches("[A-Za-z_][A-Za-z0-9_]{0,63}")) invalid("绑定字段名无效");
                        validateRef(entry.getValue(), definitions);
                    }
                }
            } catch (IllegalArgumentException error) {
                throw new FlowDocumentValidator.ValidationException(error.getMessage(), (String) node.get("id"));
            }
        }
    }
    static void validateRef(Object raw, Map<String, Map<String, Object>> definitions) {
        if (!(raw instanceof Map<?, ?>)) invalid("取值引用无效");
        Map<?, ?> ref = (Map<?, ?>) raw;
        Object source = ref.get("source");
        if ("literal".equals(source)) {
            if (!ref.keySet().equals(Set.of("source", "value"))) invalid("固定值引用无效");
            return;
        }
        Set<String> fields;
        if ("variable".equals(source)) {
            fields = Set.of("source", "variableId", "path");
            if (!definitions.containsKey(ref.get("variableId"))) invalid("引用变量不存在");
        } else if ("context".equals(source)) {
            fields = Set.of("source", "key", "path");
            if (!(ref.get("key") instanceof String) || !Set.of("input", "output").contains(ref.get("key"))) invalid("仅允许引用请求输入或当前输出");
        } else { invalid("不支持的取值来源"); return; }
        if (!fields.containsAll(ref.keySet())) invalid("取值引用含未知字段");
        if (ref.containsKey("path")) {
            if (!(ref.get("path") instanceof List<?> path) || path.size() > 16) invalid("取值路径无效");
            for (Object part : (List<?>) ref.get("path")) {
                if (part instanceof String text && text.matches("[A-Za-z_][A-Za-z0-9_]{0,63}") && !Set.of("__proto__", "prototype", "constructor").contains(text)) continue;
                if (part instanceof Number number && number.intValue() >= 0 && number.intValue() == number.doubleValue()) continue;
                invalid("取值路径无效");
            }
        }
    }
    static Object resolve(Map<?, ?> ref, Map<String, Object> variables, Map<String, Object> input, Map<String, Object> output) {
        Object value = switch (String.valueOf(ref.get("source"))) {
            case "literal" -> ref.get("value");
            case "variable" -> variables.get(ref.get("variableId"));
            default -> "input".equals(ref.get("key")) ? input : output;
        };
        if (ref.get("path") instanceof List<?> path) for (Object part : path) {
            if (part instanceof String key && value instanceof Map<?, ?> map && map.containsKey(key)) value = map.get(key);
            else if (part instanceof Number i && value instanceof List<?> list && i.intValue() < list.size()) value = list.get(i.intValue());
            else throw new FlowRuntimeException("FLOW_VALUE_MISSING", "取值路径不存在");
        }
        return value;
    }
    static Map<String, Object> bind(Map<?, ?> bindings, Map<String, Object> variables, Map<String, Object> input, Map<String, Object> output) {
        Map<String, Object> result = new LinkedHashMap<>();
        bindings.forEach((key, ref) -> result.put((String) key, resolve((Map<?, ?>) ref, variables, input, output)));
        return result;
    }
    private static void invalid(String message) { throw new IllegalArgumentException(message); }
}

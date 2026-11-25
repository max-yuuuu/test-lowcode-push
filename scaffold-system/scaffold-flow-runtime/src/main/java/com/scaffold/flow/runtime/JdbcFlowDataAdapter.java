package com.scaffold.flow.runtime;

import com.scaffold.framework.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the data subset of a published Flow without accepting SQL or table names from a document.
 * Model and column identifiers are resolved from the tenant-scoped low-code metadata tables only.
 */
public class JdbcFlowDataAdapter implements FlowDataAdapter {
    private static final int MAX_LIMIT = 100;
    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    public static final List<String> SERVER_CONTROLLED_FIELDS =
            List.of("id", "tenantId", "createBy", "updateBy", "createTime", "updateTime", "deleted");
    private static final List<String> SERVER_CONTROLLED_COLUMNS =
            List.of("id", "tenant_id", "create_by", "update_by", "create_time", "update_time", "deleted");

    private final JdbcOperations jdbc;
    private final ModelMetadataProvider metadata;

    public JdbcFlowDataAdapter(JdbcOperations jdbc, ModelMetadataProvider metadata) {
        this.jdbc = jdbc;
        this.metadata = metadata;
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> context, Map<String, Object> node,
                                      Map<String, Object> input) {
        long operationProjectId = positiveLong(context.get("projectId"), "Flow projectId 无效");
        long modelId = positiveLong(node.get("modelId"), "Flow modelId 无效");
        Map<String, Object> model = metadata.selectRuntimeModel(modelId);
        if (model == null || !sameId(model.get("projectId"), operationProjectId)
                || !sameId(model.get("status"), 1) || truthy(model.get("deleted"))) {
            throw failure("FLOW_MODEL_FORBIDDEN", "Flow 数据节点模型不可访问");
        }
        if (truthy(model.get("enableDataScope"))) {
            throw failure("FLOW_DATA_SCOPE_UNSUPPORTED", "启用数据权限的模型暂不支持 Flow 数据节点");
        }
        String table = identifier(model.get("tableName"), "模型物理表名无效");
        SqlIdentifiers.requireTableName(table);
        Map<String, Map<String, Object>> metadata = fieldMetadata(modelId);
        String kind = String.valueOf(node.get("kind"));
        return switch (kind) {
            case "query" -> query(model, table, metadata, tenantId(context, model, metadata), node, input);
            case "write" -> write(context, model, table, metadata, node, input);
            default -> throw failure("FLOW_NODE_UNSUPPORTED", "Flow 数据节点类型不支持：" + kind);
        };
    }

    private long tenantId(Map<String, Object> context, Map<String, Object> model,
                          Map<String, Map<String, Object>> metadata) {
        boolean scoped = truthy(model.get("tenantScoped")) || metadata.values().stream()
                .anyMatch(field -> "tenant_id".equalsIgnoreCase(String.valueOf(field.get("columnName"))));
        if (!scoped) return 0L;
        Object value = context.get("tenantId");
        if (!(value instanceof Number number) || number.longValue() <= 0 || number.longValue() != number.doubleValue()) {
            throw failure("FLOW_TENANT_FORBIDDEN", "Flow 数据节点缺少可信租户上下文");
        }
        if (metadata.values().stream().noneMatch(field -> "tenant_id".equalsIgnoreCase(String.valueOf(field.get("columnName"))))) {
            metadata.put("tenantId", Map.of("fieldCode", "tenantId", "columnName", "tenant_id",
                    "isPrimary", false, "isAutoIncrement", false, "isInsert", true, "isEdit", true));
        }
        return number.longValue();
    }

    private Map<String, Object> query(Map<String, Object> model, String table,
                                      Map<String, Map<String, Object>> metadata, long tenantId,
                                      Map<String, Object> node, Map<String, Object> input) {
        List<?> rawFields = list(node.get("fields"));
        List<String> select = new ArrayList<>();
        for (Object raw : rawFields) {
            String code = identifier(raw, "查询字段无效");
            Map<String, Object> field = requiredField(metadata, code);
            select.add(column(field, code) + " AS " + code);
        }
        Map<?, ?> filters = map(node.get("filters"));
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(String.join(", ", select))
                .append(" FROM ").append(table).append(" WHERE 1 = 1");
        if (truthy(model.get("enableLogicDelete"))) sql.append(" AND deleted = 0");
        if (tenantId > 0) {
            sql.append(" AND tenant_id = ?");
            args.add(tenantId);
        }
        for (Map.Entry<?, ?> entry : filters.entrySet()) {
            String code = identifier(entry.getKey(), "查询过滤字段无效");
            Map<String, Object> field = requiredField(metadata, code);
            sql.append(" AND ").append(column(field, code)).append(" = ?");
            args.add(resolveValue(entry.getValue(), input));
        }
        int limit = integer(node.getOrDefault("limit", 20), "查询 limit 无效");
        if (limit < 1 || limit > MAX_LIMIT) throw failure("FLOW_LIMIT_INVALID", "Flow 查询 limit 超出上限");
        sql.append(" LIMIT ?");
        args.add(limit);
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        return Map.of("rows", rows, "count", rows.size());
    }

    private Map<String, Object> write(Map<String, Object> context, Map<String, Object> model, String table,
                                      Map<String, Map<String, Object>> metadata,
                                      Map<String, Object> node, Map<String, Object> input) {
        String mode = String.valueOf(node.get("mode"));
        if (!"insert".equals(mode) && !"update".equals(mode)) {
            throw failure("FLOW_WRITE_INVALID", "Flow 写入模式不支持：" + mode);
        }
        Map<?, ?> rawValues = map(node.get("values"));
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawValues.entrySet()) {
            String code = identifier(entry.getKey(), "写入字段无效");
            Map<String, Object> field = requiredField(metadata, code);
            boolean primaryKey = "update".equals(mode) && truthy(field.get("isPrimary"));
            String physicalColumn = column(field, code);
            boolean controlled = SERVER_CONTROLLED_FIELDS.contains(code)
                    || SERVER_CONTROLLED_COLUMNS.stream().anyMatch(physicalColumn::equalsIgnoreCase);
            if (controlled && !(primaryKey && "id".equalsIgnoreCase(physicalColumn))) {
                throw failure("FLOW_FIELD_FORBIDDEN", "服务端控制字段不可写入：" + code);
            }
            boolean allowed = "insert".equals(mode) ? truthy(field.get("isInsert")) : truthy(field.get("isEdit"));
            if (!primaryKey && (!allowed || truthy(field.get("isAutoIncrement")))) {
                throw failure("FLOW_FIELD_FORBIDDEN", "字段不可写入：" + code);
            }
            values.put(code, resolveValue(entry.getValue(), input));
        }
        if (truthy(model.get("enableAuditField"))) addAuditValues(mode, metadata, values);
        long tenantId = tenantId(context, model, metadata);
        if (tenantId > 0 && "insert".equals(mode)) {
            String tenantField = metadata.entrySet().stream()
                    .filter(entry -> "tenant_id".equalsIgnoreCase(String.valueOf(entry.getValue().get("columnName"))))
                    .map(Map.Entry::getKey).findFirst().orElseThrow();
            values.put(tenantField, tenantId);
        }
        if (values.isEmpty()) throw failure("FLOW_WRITE_INVALID", "Flow 写入字段不能为空");
        if ("insert".equals(mode)) return insert(model, table, metadata, values);
        return update(model, table, metadata, tenantId, values, input);
    }

    private void addAuditValues(String mode, Map<String, Map<String, Object>> metadata,
                                Map<String, Object> values) {
        String operator = SecurityUtils.getUsername();
        LocalDateTime now = LocalDateTime.now();
        if ("insert".equals(mode)) {
            setAuditValue(metadata, values, "createBy", "create_by", operator);
            setAuditValue(metadata, values, "createTime", "create_time", now);
        }
        setAuditValue(metadata, values, "updateBy", "update_by", operator);
        setAuditValue(metadata, values, "updateTime", "update_time", now);
    }

    private void setAuditValue(Map<String, Map<String, Object>> metadata, Map<String, Object> values,
                               String code, String physicalColumn, Object value) {
        String target = metadata.entrySet().stream()
                .filter(entry -> physicalColumn.equalsIgnoreCase(String.valueOf(entry.getValue().get("columnName"))))
                .map(Map.Entry::getKey).findFirst().orElseGet(() -> {
                    metadata.put(code, Map.of("fieldCode", code, "columnName", physicalColumn,
                            "isPrimary", false, "isAutoIncrement", false, "isInsert", true, "isEdit", true));
                    return code;
                });
        values.put(target, value);
    }

    private Map<String, Object> insert(Map<String, Object> model, String table,
                                       Map<String, Map<String, Object>> metadata, Map<String, Object> values) {
        List<String> columns = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            columns.add(column(metadata.get(entry.getKey()), entry.getKey()));
            args.add(entry.getValue());
        }
        String placeholders = "?, ".repeat(Math.max(0, args.size() - 1)) + "?";
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        int affected = jdbc.update(sql, args.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "insert");
        result.put("affected", affected);
        return result;
    }

    private Map<String, Object> update(Map<String, Object> model, String table,
                                       Map<String, Map<String, Object>> metadata, long tenantId,
                                       Map<String, Object> values, Map<String, Object> input) {
        String primary = metadata.values().stream().filter(f -> truthy(f.get("isPrimary")))
                .map(f -> String.valueOf(f.get("fieldCode"))).findFirst()
                .orElseThrow(() -> failure("FLOW_MODEL_INVALID", "模型缺少主键字段"));
        Object id = values.remove(primary);
        if (id == null && input != null) id = input.get(primary);
        if (id == null) throw failure("FLOW_WRITE_INVALID", "更新操作缺少主键：" + primary);
        if (values.isEmpty()) throw failure("FLOW_WRITE_INVALID", "更新操作没有可写字段");
        List<Object> args = new ArrayList<>();
        List<String> assignments = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            assignments.add(column(metadata.get(entry.getKey()), entry.getKey()) + " = ?");
            args.add(entry.getValue());
        }
        String sql = "UPDATE " + table + " SET " + String.join(", ", assignments) + " WHERE ";
        if (tenantId > 0) {
            sql += "tenant_id = ? AND ";
            args.add(tenantId);
        }
        sql += column(metadata.get(primary), primary) + " = ?";
        args.add(id);
        if (truthy(model.get("enableLogicDelete"))) sql += " AND deleted = 0";
        int affected = jdbc.update(sql, args.toArray());
        return Map.of("mode", "update", "affected", affected, "id", id);
    }

    private Object resolveValue(Object value, Map<String, Object> input) {
        if (!(value instanceof String text) || !text.startsWith("$input.")) return value;
        Object current = input;
        for (String part : text.substring("$input.".length()).split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                throw failure("FLOW_INPUT_BINDING_INVALID", "Flow 查询参数路径不存在：" + text);
            }
            current = map.get(part);
        }
        return current;
    }

    private Map<String, Map<String, Object>> fieldMetadata(long modelId) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : metadata.selectFields(modelId)) {
            String code = identifier(row.get("fieldCode"), "模型字段编码无效");
            String column = identifier(row.get("columnName"), "模型列名无效");
            SqlIdentifiers.requireColumnName(column);
            result.put(code, row);
        }
        return result;
    }

    private Map<String, Object> requiredField(Map<String, Map<String, Object>> metadata, Object code) {
        Map<String, Object> field = metadata.get(String.valueOf(code));
        if (field == null) throw failure("FLOW_FIELD_NOT_FOUND", "模型字段不存在：" + code);
        return field;
    }

    private String column(Map<String, Object> field, String code) {
        return identifier(field.get("columnName"), "字段列名无效");
    }

    @SuppressWarnings("unchecked") private List<?> list(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) throw failure("FLOW_QUERY_INVALID", "查询字段不能为空");
        return list;
    }
    @SuppressWarnings("unchecked") private Map<?, ?> map(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw failure("FLOW_NODE_INVALID", "Flow 数据节点对象无效");
        return map;
    }
    private int integer(Object value, String message) {
        if (!(value instanceof Number n) || n.longValue() != n.doubleValue()) throw failure("FLOW_QUERY_INVALID", message);
        return n.intValue();
    }
    private long positiveLong(Object value, String message) {
        if (!(value instanceof Number n) || n.longValue() <= 0 || n.longValue() != n.doubleValue()) throw failure("FLOW_MODEL_FORBIDDEN", message);
        return n.longValue();
    }
    private String identifier(Object value, String message) {
        String text = String.valueOf(value);
        if (!text.matches(IDENTIFIER) || text.length() > 64) throw failure("FLOW_IDENTIFIER_INVALID", message);
        return text;
    }
    private boolean sameId(Object value, long expected) { return value instanceof Number n && n.longValue() == expected; }
    private boolean truthy(Object value) { return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0; }
    private FlowRuntimeException failure(String code, String message) { return new FlowRuntimeException(code, message); }
}

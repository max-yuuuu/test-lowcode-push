package com.scaffold.flow.runtime;

import com.scaffold.framework.datascope.DataScopeCriteria;
import com.scaffold.framework.flow.FlowExecutionContext;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcOperations;

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

    private final JdbcOperations jdbc;
    private final ModelMetadataProvider metadata;

    public JdbcFlowDataAdapter(JdbcOperations jdbc, ModelMetadataProvider metadata) {
        this.jdbc = jdbc;
        this.metadata = metadata;
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> context, Map<String, Object> node,
                                      Map<String, Object> input) {
        return invoke(null, context, node, input, null);
    }

    @Override
    public Map<String, Object> invoke(FlowExecutionContext trusted, Map<String, Object> node,
                                      Map<String, Object> input) {
        return invoke(trusted, Map.of("projectId", trusted.projectId()), node, input,
                trusted.subject() == null ? null : trusted.subject().username());
    }

    private Map<String, Object> invoke(FlowExecutionContext trusted, Map<String, Object> context,
                                       Map<String, Object> node, Map<String, Object> input,
                                       String trustedOperator) {
        long operationProjectId = positiveLong(context.get("projectId"), "Flow projectId 无效");
        if (trusted != null && (trusted.projectId() != operationProjectId
                || trusted.subject() == null || trusted.subject().tenantId() == null
                || trusted.subject().tenantId() != trusted.tenantId()
                || trusted.operationPermission() == null
                || !trusted.operationPermission().isBlank()
                    && !trusted.permissions().contains(trusted.operationPermission()))) {
            throw failure("FLOW_CONTEXT_FORBIDDEN", "Flow 可信执行上下文不一致");
        }
        long modelId = positiveLong(node.get("modelId"), "Flow modelId 无效");
        Map<String, Object> model = metadata.selectRuntimeModel(modelId);
        if (model == null || !sameId(model.get("projectId"), operationProjectId)
                || !sameId(model.get("status"), 1) || truthy(model.get("deleted"))) {
            throw failure("FLOW_MODEL_FORBIDDEN", "Flow 数据节点模型不可访问");
        }
        String table = identifier(model.get("tableName"), "模型物理表名无效");
        SqlIdentifiers.requireTableName(table);
        Map<String, Map<String, Object>> metadata = fieldMetadata(modelId);
        RowScope rowScope = truthy(model.get("enableDataScope"))
                ? rowScope(trusted, metadata) : RowScope.disabled();
        String kind = String.valueOf(node.get("kind"));
        return switch (kind) {
            case "query" -> query(model, table, metadata, rowScope, node, input);
            case "write" -> write(model, table, metadata, rowScope, node, input, trustedOperator);
            default -> throw failure("FLOW_NODE_UNSUPPORTED", "Flow 数据节点类型不支持：" + kind);
        };
    }

    private RowScope rowScope(FlowExecutionContext trusted, Map<String, Map<String, Object>> metadata) {
        if (trusted == null || trusted.dataScope() == null) {
            throw failure("FLOW_CONTEXT_FORBIDDEN", "数据权限模型需要可信执行上下文");
        }
        DataScopeCriteria scope = trusted.dataScope();
        if (!scope.isAllScope() && !scope.getDeptIds().isEmpty()) {
            throw failure("FLOW_DATA_SCOPE_UNSUPPORTED", "部门数据范围尚无 Flow 运行时列契约");
        }
        if (!scope.isAllScope() && (scope.getUserId() == null
                || trusted.subject() == null || !scope.getUserId().equals(trusted.subject().userId()))) {
            throw failure("FLOW_CONTEXT_FORBIDDEN", "Flow 数据范围与可信用户不一致");
        }
        Map<String, Object> owner = metadata.get("createBy");
        if (owner == null || !"create_by".equalsIgnoreCase(String.valueOf(owner.get("columnName")))) {
            throw failure("FLOW_MODEL_INVALID", "数据权限模型缺少 create_by 所有者字段");
        }
        return new RowScope(true, !scope.isAllScope(), String.valueOf(owner.get("fieldCode")),
                column(owner, "createBy"), trusted.subject().username());
    }

    private Map<String, Object> query(Map<String, Object> model, String table,
                                      Map<String, Map<String, Object>> metadata, RowScope rowScope,
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
        for (Map.Entry<?, ?> entry : filters.entrySet()) {
            String code = identifier(entry.getKey(), "查询过滤字段无效");
            Map<String, Object> field = requiredField(metadata, code);
            sql.append(" AND ").append(column(field, code)).append(" = ?");
            args.add(resolveValue(entry.getValue(), input));
        }
        if (rowScope.selfOnly()) {
            sql.append(" AND ").append(rowScope.ownerColumn()).append(" = ?");
            args.add(rowScope.owner());
        }
        int limit = integer(node.getOrDefault("limit", 20), "查询 limit 无效");
        if (limit < 1 || limit > MAX_LIMIT) throw failure("FLOW_LIMIT_INVALID", "Flow 查询 limit 超出上限");
        sql.append(" LIMIT ?");
        args.add(limit);
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        return Map.of("rows", rows, "count", rows.size());
    }

    private Map<String, Object> write(Map<String, Object> model, String table,
                                      Map<String, Map<String, Object>> metadata, RowScope rowScope,
                                      Map<String, Object> node, Map<String, Object> input,
                                      String trustedOperator) {
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
            boolean allowed = "insert".equals(mode) ? truthy(field.get("isInsert")) : truthy(field.get("isEdit"));
            if (!primaryKey && (!allowed || truthy(field.get("isAutoIncrement")))) {
                throw failure("FLOW_FIELD_FORBIDDEN", "字段不可写入：" + code);
            }
            values.put(code, resolveValue(entry.getValue(), input));
        }
        if (truthy(model.get("enableAuditField"))) addAuditValues(mode, metadata, values, trustedOperator);
        if (rowScope.enabled() && "insert".equals(mode)) values.put(rowScope.ownerCode(), rowScope.owner());
        if (values.isEmpty()) throw failure("FLOW_WRITE_INVALID", "Flow 写入字段不能为空");
        if ("insert".equals(mode)) return insert(model, table, metadata, values);
        return update(model, table, metadata, rowScope, values, input);
    }

    private void addAuditValues(String mode, Map<String, Map<String, Object>> metadata,
                                Map<String, Object> values, String trustedOperator) {
        boolean insert = "insert".equals(mode);
        boolean createAllowed = insert && writable(metadata.get("createBy"), true);
        boolean updateAllowed = writable(metadata.get("updateBy"), insert);
        if (!createAllowed && !updateAllowed) return;
        String operator = trustedOperator == null ? SecurityUtils.getUsername() : trustedOperator;
        if (createAllowed) values.put("createBy", operator);
        if (updateAllowed) values.put("updateBy", operator);
    }

    private boolean writable(Map<String, Object> field, boolean insert) {
        if (field == null) return false;
        return insert ? truthy(field.get("isInsert")) : truthy(field.get("isEdit"));
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
                                       Map<String, Map<String, Object>> metadata, RowScope rowScope,
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
        args.add(id);
        String sql = "UPDATE " + table + " SET " + String.join(", ", assignments)
                + " WHERE " + column(metadata.get(primary), primary) + " = ?";
        if (truthy(model.get("enableLogicDelete"))) sql += " AND deleted = 0";
        if (rowScope.selfOnly()) {
            sql += " AND " + rowScope.ownerColumn() + " = ?";
            args.add(rowScope.owner());
        }
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

    private record RowScope(boolean enabled, boolean selfOnly, String ownerCode,
                            String ownerColumn, String owner) {
        private static RowScope disabled() {
            return new RowScope(false, false, null, null, null);
        }
    }
}

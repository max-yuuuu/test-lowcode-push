package com.scaffold.business.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.flow.runtime.FlowDataAdapter;
import com.scaffold.flow.runtime.FlowRuntimeException;
import com.scaffold.flow.runtime.FlowRuntimeService;
import com.scaffold.flow.runtime.JdbcFlowDataAdapter;
import com.scaffold.flow.runtime.ModelMetadataProvider;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 交付侧受控执行入口：按模型、operationCode 与版本读不可变快照，递归执行接口节点。 */
@Component
public class FlowSnapshotExecutor {
    private static final int MAX_OPERATION_DEPTH = 8;
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);
    private final JdbcOperations jdbc;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper json;
    private final PermissionResolver permissionResolver;

    public FlowSnapshotExecutor(JdbcOperations jdbc, ResourceLoader resourceLoader, ObjectMapper json,
                                PermissionResolver permissionResolver) {
        this.jdbc = jdbc;
        this.resourceLoader = resourceLoader;
        this.json = json;
        this.permissionResolver = permissionResolver;
    }

    public Map<String, Object> execute(long modelId, String operationCode, int version, Map<String, Object> input) {
        return executeSnapshot(load("classpath:flow/" + modelId + "/" + operationCode + ".v" + version + ".json"),
                input, 0, new HashSet<>(), Instant.now().plus(EXECUTION_TIMEOUT));
    }

    /** 兼容旧版未按模型分目录的快照。 */
    public Map<String, Object> execute(String operationCode, int version, Map<String, Object> input) {
        return executeSnapshot(load("classpath:flow/" + operationCode + ".v" + version + ".json"),
                input, 0, new HashSet<>(), Instant.now().plus(EXECUTION_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> executeSnapshot(Map<String, Object> snapshot, Map<String, Object> input) {
        return executeSnapshot(snapshot, input, 0, new HashSet<>(), Instant.now().plus(EXECUTION_TIMEOUT));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeSnapshot(Map<String, Object> snapshot, Map<String, Object> input, int depth,
                                                Set<String> stack, Instant deadline) {
        if (depth > MAX_OPERATION_DEPTH) throw new FlowRuntimeException("FLOW_RECURSION_LIMIT", "Flow operation 调用深度超过上限");
        Object document = snapshot.get("document");
        if (!(document instanceof Map<?, ?> doc)) throw new FlowRuntimeException("FLOW_SNAPSHOT_INVALID", "Flow 快照缺少流程文档");
        long modelId = number(snapshot.get("modelId"));
        String identity = modelId + ":" + snapshot.get("operationCode") + ".v" + snapshot.get("version");
        if (!stack.add(identity)) throw new FlowRuntimeException("FLOW_RECURSION_LIMIT", "Flow operation 存在循环调用");
        try {
            Map<String, Object> models = snapshot.get("models") instanceof Map<?, ?> raw
                    ? (Map<String, Object>) raw : Map.of();
            ModelMetadataProvider provider = new ResourceModelMetadataProvider(models);
            FlowDataAdapter adapter = new JdbcFlowDataAdapter(jdbc, provider);
            Map<String, Object> context = new java.util.LinkedHashMap<>(
                    Map.of("projectId", number(snapshot.get("projectId")), "modelId", modelId));
            if (SecurityUtils.isAuthenticated()) context.put("tenantId", SecurityUtils.getTenantId());
            FlowRuntimeService.ExecutionOptions options = new FlowRuntimeService.ExecutionOptions(
                    Thread.currentThread()::isInterrupted, deadline);
            FlowRuntimeService.ExecutionResult result;
            try {
                result = new FlowRuntimeService().execute((Map<String, Object>) doc,
                        input == null ? Map.of() : input,
                        (node, current) -> adapter.invoke(context, node, current),
                        (node, current) -> invokeOperation(context, node, current, depth, stack, deadline), options);
            } catch (IllegalArgumentException error) {
                throw new FlowRuntimeException("FLOW_DOCUMENT_INVALID", "Flow 快照文档无效");
            }
            if ("cancelled".equalsIgnoreCase(result.status())) {
                throw new FlowRuntimeException("FLOW_CANCELLED", "Flow 执行已取消", result.nodePath());
            }
            if (!"success".equalsIgnoreCase(result.status())) {
                throw new FlowRuntimeException("FLOW_EXECUTION_FAILED", "Flow 执行失败", result.nodePath());
            }
            return result.output();
        } finally {
            stack.remove(identity);
        }
    }

    private Map<String, Object> invokeOperation(Map<String, Object> context, Map<String, Object> node,
                                                Map<String, Object> input, int depth, Set<String> stack, Instant deadline) {
        long modelId = node.get("modelId") instanceof Number id ? id.longValue() : ((Number) context.get("modelId")).longValue();
        String code = String.valueOf(node.get("operationCode"));
        if (!(node.get("version") instanceof Number version)) {
            throw new FlowRuntimeException("FLOW_OPERATION_VERSION_MISSING", "交付快照的被调用接口版本缺失");
        }
        Map<String, Object> snapshot = load("classpath:flow/" + modelId + "/" + code + ".v" + version.intValue() + ".json");
        requireNestedPermission(snapshot);
        return executeSnapshot(snapshot, input, depth + 1, stack, deadline);
    }

    private void requireNestedPermission(Map<String, Object> snapshot) {
        if (!(snapshot.get("permissionCode") instanceof String permission)) {
            throw new FlowRuntimeException("FLOW_SNAPSHOT_INVALID", "被调用接口快照缺少权限契约");
        }
        if (!SecurityUtils.isAuthenticated()) {
            throw new FlowRuntimeException("FLOW_OPERATION_FORBIDDEN", "无权调用该接口");
        }
        String required = permission.trim();
        if (!required.isEmpty() && !permissionResolver.hasPermission(SecurityUtils.getLoginUser(), required)) {
            throw new FlowRuntimeException("FLOW_OPERATION_FORBIDDEN", "无权调用该接口");
        }
    }

    private Map<String, Object> load(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (resource == null || !resource.exists()) {
            throw new FlowRuntimeException("FLOW_SNAPSHOT_NOT_FOUND", "Flow 快照不存在");
        }
        try (var stream = resource.getInputStream()) {
            Map<String, Object> snapshot = json.readValue(stream, new TypeReference<Map<String, Object>>() {});
            if (snapshot == null) throw new FlowRuntimeException("FLOW_SNAPSHOT_INVALID", "Flow 快照为空");
            return snapshot;
        } catch (java.io.IOException error) {
            throw new FlowRuntimeException("FLOW_SNAPSHOT_INVALID", "Flow 快照读取失败");
        }
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}

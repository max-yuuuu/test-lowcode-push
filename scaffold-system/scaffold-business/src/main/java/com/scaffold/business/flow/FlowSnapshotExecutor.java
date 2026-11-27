package com.scaffold.business.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.framework.flow.FlowExecutionContext;
import com.scaffold.framework.flow.FlowExecutionContextFactory;
import com.scaffold.flow.runtime.FlowRuntimeException;
import com.scaffold.flow.runtime.FlowRuntimeService;
import com.scaffold.flow.runtime.JdbcFlowDataAdapter;
import com.scaffold.flow.runtime.ModelMetadataProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 交付侧受控执行入口：快照只提供绑定与数据，身份、权限、数据范围和事务由服务端建立。 */
@Service
public class FlowSnapshotExecutor {
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);
    private final JdbcOperations jdbc;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper json;
    private final FlowExecutionContextFactory contextFactory;
    private final FlowRuntimeService runtime;

    public FlowSnapshotExecutor(JdbcOperations jdbc, ResourceLoader resourceLoader, ObjectMapper json,
                                FlowExecutionContextFactory contextFactory, FlowRuntimeService runtime) {
        this.jdbc = jdbc;
        this.resourceLoader = resourceLoader;
        this.json = json;
        this.contextFactory = contextFactory;
        this.runtime = runtime;
    }

    @Transactional
    public Map<String, Object> executeResult(String operationCode, int version, Map<String, Object> input) {
        String location = "classpath:flow/" + operationCode + ".v" + version + ".json";
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) throw new BusinessException("flow 快照不存在：" + operationCode + ".v" + version);
        try (var stream = resource.getInputStream()) {
            return executeSnapshotResult(json.readValue(stream, new TypeReference<Map<String, Object>>() {}),
                    input, operationCode, version);
        } catch (java.io.IOException error) {
            throw new BusinessException("flow 快照读取失败：" + location);
        }
    }

    @Transactional
    Map<String, Object> executeSnapshotResult(Map<String, Object> snapshot, Map<String, Object> input) {
        return executeSnapshotResult(snapshot, input, text(snapshot.get("operationCode")), integer(snapshot.get("version")));
    }

    @Transactional
    Map<String, Object> executeSnapshotResult(Map<String, Object> snapshot, Map<String, Object> input,
                                               String operationCode, int version) {
        if (snapshot == null || !operationCode.equals(text(snapshot.get("operationCode")))
                || version != integer(snapshot.get("version"))) {
            throw new BusinessException("flow 快照与请求的接口版本不一致");
        }
        long operationId = positiveLong(snapshot.get("operationId"), "operationId");
        long projectId = positiveLong(snapshot.get("projectId"), "projectId");
        long modelId = positiveLong(snapshot.get("modelId"), "modelId");
        int revision = positiveInt(snapshot.get("flowRevision"), "flowRevision");
        String permission = text(snapshot.get("permissionCode"));
        Object document = snapshot.get("document");
        if (!(document instanceof Map<?, ?>)) throw new BusinessException("flow 快照缺少流程文档");
        Map<String, Object> models = snapshot.get("models") instanceof Map<?, ?> raw
                ? cast(raw) : Map.of();
        FlowExecutionContext trusted = contextFactory.create(input, projectId, modelId, operationId, version,
                permission, EXECUTION_TIMEOUT, !permission.isEmpty())
                .withExecution(operationCode, revision, null, true);
        ModelMetadataProvider provider = new ResourceModelMetadataProvider(models);
        JdbcFlowDataAdapter adapter = new JdbcFlowDataAdapter(jdbc, provider);
        FlowRuntimeService.ExecutionResult result = runtime.execute(cast(document), trusted.input(),
                (FlowRuntimeService.ContextDataInvoker) adapter::invoke,
                (node, current) -> { throw new FlowRuntimeException("FLOW_NODE_UNSUPPORTED", "交付侧暂不支持接口节点调用"); },
                FlowRuntimeService.ExecutionOptions.forContext(trusted));
        if ("cancelled".equalsIgnoreCase(result.status())) {
            throw new FlowRuntimeException("FLOW_CANCELLED", "Flow 已取消", result.nodePath());
        }
        if (!"success".equalsIgnoreCase(result.status())) {
            throw new FlowRuntimeException("FLOW_EXECUTION_FAILED", "Flow 执行失败", result.nodePath());
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", result.status());
        response.put("output", result.output());
        response.put("traceId", result.traceId());
        response.put("operationCode", operationCode);
        response.put("operationId", operationId);
        response.put("projectId", projectId);
        response.put("modelId", modelId);
        response.put("version", version);
        response.put("flowRevision", revision);
        return response;
    }

    private static long positiveLong(Object value, String name) {
        if (!(value instanceof Number number) || number.longValue() <= 0 || number.longValue() != number.doubleValue()) {
            throw new BusinessException("flow 快照绑定缺少有效的 " + name);
        }
        return number.longValue();
    }

    private static int positiveInt(Object value, String name) {
        long result = positiveLong(value, name);
        if (result > Integer.MAX_VALUE) throw new BusinessException("flow 快照绑定缺少有效的 " + name);
        return (int) result;
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}

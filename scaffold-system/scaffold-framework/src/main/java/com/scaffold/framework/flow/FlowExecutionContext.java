package com.scaffold.framework.flow;

import com.scaffold.framework.datascope.DataScopeCriteria;
import com.scaffold.framework.security.LoginUser;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

/** Server-derived identity and authorization data for one packaged Flow invocation. */
public record FlowExecutionContext(
        Map<String, Object> input,
        LoginUser subject,
        long tenantId,
        long projectId,
        long modelId,
        long operationId,
        int operationVersion,
        String operationPermission,
        String traceId,
        Instant deadline,
        Set<String> permissions,
        DataScopeCriteria dataScope,
        String operationCode,
        int flowRevision,
        String idempotencyKey,
        boolean transactional) {

    public FlowExecutionContext(Map<String, Object> input, LoginUser subject, long tenantId,
                                 long projectId, long modelId, long operationId, int operationVersion,
                                 String operationPermission, String traceId, Instant deadline,
                                 Set<String> permissions, DataScopeCriteria dataScope) {
        this(input, subject, tenantId, projectId, modelId, operationId, operationVersion,
                operationPermission, traceId, deadline, permissions, dataScope, null, 0, null, false);
    }

    public FlowExecutionContext {
        input = immutableMap(input);
        permissions = Set.copyOf(permissions);
    }

    public FlowExecutionContext withInput(Map<String, Object> input) {
        return new FlowExecutionContext(input, subject, tenantId, projectId, modelId, operationId,
                operationVersion, operationPermission, traceId, deadline, permissions, dataScope,
                operationCode, flowRevision, idempotencyKey, transactional);
    }

    public FlowExecutionContext withExecution(String operationCode, int flowRevision,
                                               String idempotencyKey, boolean transactional) {
        return new FlowExecutionContext(input, subject, tenantId, projectId, modelId, operationId,
                operationVersion, operationPermission, traceId, deadline, permissions, dataScope,
                operationCode, flowRevision, idempotencyKey, transactional);
    }

    public FlowExecutionContext withRoot(FlowExecutionContext root, boolean transaction) {
        if (!subject.equals(root.subject()) || tenantId != root.tenantId() || projectId != root.projectId()) {
            throw new IllegalArgumentException("Nested Flow execution binding differs from root");
        }
        return new FlowExecutionContext(input, subject, tenantId, projectId, modelId, operationId,
                operationVersion, operationPermission, root.traceId(), root.deadline(), permissions,
                dataScope, operationCode, flowRevision, null, transaction);
    }

    private static Map<String, Object> immutableMap(Map<String, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) source.forEach((key, value) -> copy.put(key, immutableValue(value, 1)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value, int depth) {
        if (depth > 64) throw new IllegalArgumentException("Flow input nesting exceeds limit");
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (!(key instanceof String text)) throw new IllegalArgumentException("Flow input object keys must be strings");
                copy.put(text, immutableValue(nested, depth + 1));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(immutableValue(item, depth + 1)));
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Set<?> set) {
            java.util.LinkedHashSet<Object> copy = new java.util.LinkedHashSet<>();
            set.forEach(item -> copy.add(immutableValue(item, depth + 1)));
            return Collections.unmodifiableSet(copy);
        }
        return value;
    }
}

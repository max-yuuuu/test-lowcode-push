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
import java.util.Set;

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
        DataScopeCriteria dataScope) {

    public FlowExecutionContext {
        input = immutableMap(input);
        permissions = Set.copyOf(permissions);
    }

    private static Map<String, Object> immutableMap(Map<String, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, immutableValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (!(key instanceof String text)) throw new IllegalArgumentException("Flow input object keys must be strings");
                copy.put(text, immutableValue(nested));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(item -> copy.add(immutableValue(item)));
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Set<?> set) {
            java.util.LinkedHashSet<Object> copy = new java.util.LinkedHashSet<>();
            set.forEach(item -> copy.add(immutableValue(item)));
            return Collections.unmodifiableSet(copy);
        }
        return value;
    }
}

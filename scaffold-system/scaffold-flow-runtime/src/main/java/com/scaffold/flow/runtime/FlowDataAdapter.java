package com.scaffold.flow.runtime;

import com.scaffold.framework.flow.FlowExecutionContext;

import java.util.Map;

/** Server-owned data boundary for published Flow query/write nodes. */
@FunctionalInterface
public interface FlowDataAdapter {
    Map<String, Object> invoke(Map<String, Object> operationContext,
                               Map<String, Object> node,
                               Map<String, Object> input);

    default Map<String, Object> invoke(FlowExecutionContext context, Map<String, Object> node,
                                       Map<String, Object> input) {
        return invoke(Map.of("projectId", context.projectId()), node, input);
    }
}

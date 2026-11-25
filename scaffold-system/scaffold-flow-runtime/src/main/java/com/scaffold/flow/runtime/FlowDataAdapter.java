package com.scaffold.flow.runtime;

import java.util.Map;

/** Server-owned data boundary for published Flow query/write nodes. */
@FunctionalInterface
public interface FlowDataAdapter {
    Map<String, Object> invoke(Map<String, Object> operationContext,
                               Map<String, Object> node,
                               Map<String, Object> input);
}

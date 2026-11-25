package com.scaffold.flow.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRuntimeDiagnosticsTest {
    @Test
    void missingConditionValueReportsTheCurrentNodePath() {
        Map<String, Object> document = Map.of("schemaVersion", 1, "start", "start", "nodes", List.of(
                Map.of("id", "start", "kind", "input", "next", "check"),
                Map.of("id", "check", "kind", "condition", "then", "end", "else", "end",
                        "when", Map.of("left", Map.of("source", "context", "key", "input", "path", List.of("missing")),
                                "operator", "eq", "right", Map.of("source", "literal", "value", true))),
                Map.of("id", "end", "kind", "end")));

        FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                () -> new FlowRuntimeService().execute(document, Map.of()));

        assertEquals("FLOW_VALUE_MISSING", error.code());
        assertEquals(List.of("start", "check"), error.nodePath());
        assertFalse(error.business());
    }

    @Test
    void attachingAPathDoesNotTurnABusinessFailureIntoATechnicalFailure() {
        Map<String, Object> document = Map.of("schemaVersion", 1, "start", "start", "nodes", List.of(
                Map.of("id", "start", "kind", "input", "next", "call"),
                Map.of("id", "call", "kind", "operation", "operationCode", "order.reject", "next", "end"),
                Map.of("id", "end", "kind", "end")));

        FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                () -> new FlowRuntimeService().execute(document, Map.of(),
                        (node, input) -> input,
                        (node, input) -> { throw FlowRuntimeException.business(
                                "ORDER_REJECTED", "订单已拒绝", List.of()); }));

        assertEquals("ORDER_REJECTED", error.code());
        assertEquals(List.of("start", "call"), error.nodePath());
        assertTrue(error.business());
    }
}

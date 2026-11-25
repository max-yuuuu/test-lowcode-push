package com.scaffold.business.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.framework.security.LoginUser;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.flow.runtime.FlowRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 交付侧按快照内嵌元数据执行 flow：不依赖平台元数据表，SQL 由内嵌元数据拼出。 */
class FlowSnapshotExecutorTest {
    private final PermissionResolver permissions = mock(PermissionResolver.class);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executesQueryFromEmbeddedSnapshotMetadata() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("id", 1L, "name", "Ada")));

        Map<String, Object> model = Map.of("tableName", "biz_order", "projectId", 1L, "status", 1, "deleted", 0,
                "enableLogicDelete", 1, "enableAuditField", 0, "enableDataScope", 0);
        Map<String, Object> fieldId = Map.of("fieldCode", "id", "columnName", "id",
                "isPrimary", 1, "isInsert", 0, "isEdit", 0, "isAutoIncrement", 1);
        Map<String, Object> fieldName = Map.of("fieldCode", "name", "columnName", "name",
                "isPrimary", 0, "isInsert", 1, "isEdit", 1, "isAutoIncrement", 0);
        Map<String, Object> models = Map.of("9", Map.of("model", model, "fields", List.of(fieldId, fieldName)));
        Map<String, Object> document = Map.of("schemaVersion", 1, "start", "q", "nodes", List.of(
                Map.of("id", "q", "kind", "query", "modelId", 9, "fields", List.of("id", "name"),
                        "filters", Map.of("name", "$input.name"), "limit", 20, "next", "end"),
                Map.of("id", "end", "kind", "end")));
        Map<String, Object> snapshot = Map.of("operationCode", "archive_order", "version", 1,
                "modelId", 9L, "projectId", 1L, "document", document, "models", models);

        var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), permissions);
        Map<String, Object> output = executor.executeSnapshot(snapshot, Map.of("name", "Ada"));

        var rows = (List<?>) output.get("rows");
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).toString().contains("Ada"));
        verify(jdbc).queryForList(eq("SELECT id AS id, name AS name FROM biz_order WHERE 1 = 1 AND deleted = 0 AND name = ? LIMIT ?"),
                any(Object[].class));
    }

    @Test
    void snapshotDataNodesUseTheAuthenticatedTenantInsteadOfSnapshotOrInputValues() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        Map<String, Object> model = Map.of("tableName", "sys_user", "projectId", 1L, "status", 1, "deleted", 0,
                "enableLogicDelete", 0, "enableAuditField", 0, "enableDataScope", 0);
        Map<String, Object> tenant = Map.of("fieldCode", "tenantId", "columnName", "tenant_id",
                "isPrimary", 0, "isInsert", 0, "isEdit", 0, "isAutoIncrement", 0);
        Map<String, Object> name = Map.of("fieldCode", "name", "columnName", "name",
                "isPrimary", 0, "isInsert", 1, "isEdit", 1, "isAutoIncrement", 0);
        Map<String, Object> snapshot = Map.of("operationCode", "list_users", "version", 1,
                "modelId", 9L, "projectId", 1L, "tenantId", 999L,
                "models", Map.of("9", Map.of("model", model, "fields", List.of(tenant, name))),
                "document", Map.of("schemaVersion", 1, "start", "q", "nodes", List.of(
                        Map.of("id", "q", "kind", "query", "modelId", 9, "fields", List.of("name"),
                                "filters", Map.of(), "limit", 20, "next", "end"),
                        Map.of("id", "end", "kind", "end"))));
        authenticate();
        var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), permissions);

        executor.executeSnapshot(snapshot, Map.of("tenantId", 888L));

        verify(jdbc).queryForList(eq("SELECT name AS name FROM sys_user WHERE 1 = 1 AND tenant_id = ? LIMIT ?"),
                any(Object[].class));
        org.mockito.ArgumentCaptor<Object[]> args = org.mockito.ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForList(anyString(), args.capture());
        assertEquals(1L, args.getValue()[0]);
    }

    @Test
    void executesConditionBranchesFromSnapshotDocument() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), permissions);
        Map<String, Object> document = Map.of("schemaVersion", 1, "start", "start", "nodes", List.of(
                Map.of("id", "start", "kind", "input", "next", "c"),
                Map.of("id", "c", "kind", "condition",
                        "when", Map.of("left", Map.of("source", "context", "key", "input", "path", List.of("mode")),
                                "operator", "eq", "right", Map.of("source", "literal", "value", "a")),
                        "then", "endA", "else", "endB"),
                Map.of("id", "endA", "kind", "end", "valueBindings", Map.of("branch", Map.of("source", "literal", "value", "A"))),
                Map.of("id", "endB", "kind", "end", "valueBindings", Map.of("branch", Map.of("source", "literal", "value", "B")))));
        Map<String, Object> snapshot = Map.of("operationCode", "branch_flow", "version", 1, "document", document, "models", Map.of());

        assertEquals("A", executor.executeSnapshot(snapshot, Map.of("mode", "a")).get("branch"));
        assertEquals("B", executor.executeSnapshot(snapshot, Map.of("mode", "b")).get("branch"));
    }

    @Test
    void errorNodeFailsSnapshotExecutionAsBusinessError() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), permissions);
        // error 是终止节点：用条件分流让 end 也可达（未命中分支正常结束）
        Map<String, Object> document = Map.of("schemaVersion", 1, "start", "start", "nodes", List.of(
                Map.of("id", "start", "kind", "input", "next", "c"),
                Map.of("id", "c", "kind", "condition",
                        "when", Map.of("left", Map.of("source", "context", "key", "input", "path", List.of("mode")),
                                "operator", "eq", "right", Map.of("source", "literal", "value", "halt")),
                        "then", "e", "else", "end"),
                Map.of("id", "e", "kind", "error", "code", "STOCK_SHORTAGE", "message", "库存不足，无法出库"),
                Map.of("id", "end", "kind", "end")));
        Map<String, Object> snapshot = Map.of("operationCode", "reject_flow", "version", 1, "document", document, "models", Map.of());

        FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                () -> executor.executeSnapshot(snapshot, Map.of("mode", "halt")));
        assertEquals("ok", executor.executeSnapshot(snapshot, Map.of("mode", "ok")).get("mode"));
        assertTrue(error.business());
        assertEquals("STOCK_SHORTAGE", error.code());
        assertEquals("库存不足，无法出库", error.getMessage());
    }

    @Test
    void operationNodeLoadsModelScopedNestedSnapshotAndReturnsItsOutput() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Map<String, Object> nested = Map.of("operationCode", "finance_post", "version", 2,
                "modelId", 10L, "projectId", 1L, "permissionCode", "", "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "end", "nodes", List.of(
                        Map.of("id", "end", "kind", "end", "valueBindings", Map.of(
                                "posted", Map.of("source", "literal", "value", true))))));
        Map<String, Object> parent = Map.of("operationCode", "inventory_issue", "version", 1,
                "modelId", 9L, "projectId", 1L, "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "call", "nodes", List.of(
                        Map.of("id", "call", "kind", "operation", "modelId", 10L,
                                "operationCode", "finance_post", "version", 2, "next", "end"),
                        Map.of("id", "end", "kind", "end", "valueBindings", Map.of(
                                "result", Map.of("source", "context", "key", "output", "path", List.of("posted")))))));
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("classpath:flow/9/inventory_issue.v1.json", parent);
        resources.put("classpath:flow/10/finance_post.v2.json", nested);
        ResourceLoader loader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return resource(json, resources.get(location));
            }
        };

        authenticate();
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), loader, json, permissions);

        assertEquals(Map.of("result", true), executor.execute(9L, "inventory_issue", 1, Map.of()));
    }

    @Test
    void operationNodeRechecksTheNestedPublishedPermission() throws Exception {
        ObjectMapper json = new ObjectMapper();
        LoginUser user = authenticate();
        Map<String, Object> nested = Map.of("operationCode", "finance_post", "version", 2,
                "modelId", 10L, "projectId", 1L, "permissionCode", "finance:post", "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "end", "nodes", List.of(
                        Map.of("id", "end", "kind", "end"))));
        Map<String, Object> parent = Map.of("operationCode", "inventory_issue", "version", 1,
                "modelId", 9L, "projectId", 1L, "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "call", "nodes", List.of(
                        Map.of("id", "call", "kind", "operation", "modelId", 10L,
                                "operationCode", "finance_post", "version", 2, "next", "end"),
                        Map.of("id", "end", "kind", "end"))));
        Map<String, Object> resources = Map.of("classpath:flow/9/inventory_issue.v1.json", parent,
                "classpath:flow/10/finance_post.v2.json", nested);
        ResourceLoader loader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return resource(json, resources.get(location));
            }
        };
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), loader, json, permissions);

        FlowRuntimeException denied = assertThrows(FlowRuntimeException.class,
                () -> executor.execute(9L, "inventory_issue", 1, Map.of()));
        assertEquals("FLOW_OPERATION_FORBIDDEN", denied.code());
        when(permissions.hasPermission(user, "finance:post")).thenReturn(true);
        assertEquals(Map.of(), executor.execute(9L, "inventory_issue", 1, Map.of()));
        verify(permissions, org.mockito.Mockito.times(2)).hasPermission(user, "finance:post");
    }

    @Test
    void operationNodeRequiresAuthenticationEvenWithoutAnExtraPermission() {
        ObjectMapper json = new ObjectMapper();
        Map<String, Object> child = Map.of("operationCode", "child", "version", 1,
                "modelId", 9L, "projectId", 1L, "permissionCode", "", "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "end", "nodes", List.of(
                        Map.of("id", "end", "kind", "end"))));
        Map<String, Object> snapshot = Map.of("operationCode", "parent", "version", 1,
                "modelId", 9L, "projectId", 1L, "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "call", "nodes", List.of(
                        Map.of("id", "call", "kind", "operation", "operationCode", "child", "version", 1, "next", "end"),
                        Map.of("id", "end", "kind", "end"))));
        ResourceLoader loader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return resource(json, child);
            }
        };
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), loader, json, permissions);

        FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                () -> executor.executeSnapshot(snapshot, Map.of()));
        assertEquals("FLOW_OPERATION_FORBIDDEN", error.code());
    }

    @Test
    void operationNodeRejectsCyclicSnapshotDependencies() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Map<String, Object> snapshot = Map.of("operationCode", "loop", "version", 1,
                "modelId", 9L, "projectId", 1L, "permissionCode", "", "models", Map.of(),
                "document", Map.of("schemaVersion", 1, "start", "call", "nodes", List.of(
                        Map.of("id", "call", "kind", "operation", "modelId", 9L,
                                "operationCode", "loop", "version", 1, "next", "end"),
                        Map.of("id", "end", "kind", "end"))));
        ResourceLoader loader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return resource(json, snapshot);
            }
        };
        authenticate();
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), loader, json, permissions);

        FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                () -> executor.execute(9L, "loop", 1, Map.of()));
        assertEquals("FLOW_RECURSION_LIMIT", error.code());
    }

    @Test
    void cancellationKeepsItsStableErrorCode() {
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), new DefaultResourceLoader(), new ObjectMapper(), permissions);
        Map<String, Object> snapshot = Map.of("operationCode", "cancel_flow", "version", 1, "document",
                Map.of("schemaVersion", 1, "start", "end", "nodes", List.of(Map.of("id", "end", "kind", "end"))));
        Thread.currentThread().interrupt();
        try {
            FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                    () -> executor.executeSnapshot(snapshot, Map.of()));
            assertEquals("FLOW_CANCELLED", error.code());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void missingAndMalformedSnapshotsAreSafeRuntimeFailures() {
        var executor = new FlowSnapshotExecutor(mock(JdbcOperations.class), new DefaultResourceLoader(), new ObjectMapper(), permissions);
        FlowRuntimeException missing = assertThrows(FlowRuntimeException.class,
                () -> executor.execute("definitely_missing", 1, Map.of()));
        assertEquals("FLOW_SNAPSHOT_NOT_FOUND", missing.code());

        ResourceLoader brokenLoader = new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return new ByteArrayResource("{broken".getBytes());
            }
        };
        var brokenExecutor = new FlowSnapshotExecutor(mock(JdbcOperations.class), brokenLoader, new ObjectMapper(), permissions);
        FlowRuntimeException broken = assertThrows(FlowRuntimeException.class,
                () -> brokenExecutor.execute("broken", 1, Map.of()));
        assertEquals("FLOW_SNAPSHOT_INVALID", broken.code());

        Map<String, Object> invalid = Map.of("operationCode", "invalid", "version", 1, "document",
                Map.of("schemaVersion", 1, "start", "start", "nodes", List.of(
                        Map.of("id", "start", "kind", "input", "next", "missing"))));
        FlowRuntimeException invalidDocument = assertThrows(FlowRuntimeException.class,
                () -> executor.executeSnapshot(invalid, Map.of()));
        assertEquals("FLOW_DOCUMENT_INVALID", invalidDocument.code());
    }

    private static LoginUser authenticate() {
        LoginUser user = new LoginUser(41L, "operator", 1L);
        var authentication = new UsernamePasswordAuthenticationToken("operator", "", List.of());
        authentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }

    private static Resource resource(ObjectMapper json, Object snapshot) {
        try {
            return new ByteArrayResource(json.writeValueAsBytes(snapshot));
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    void executesSnapshotLoadedFromClasspathResource() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("id", 1L, "name", "Ada")));

        var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), permissions);
        Map<String, Object> output = executor.execute("archive_order", 1, Map.of("name", "Ada"));

        assertEquals(1, ((List<?>) output.get("rows")).size());
        verify(jdbc).queryForList(eq("SELECT id AS id, name AS name FROM biz_order WHERE 1 = 1 AND deleted = 0 AND name = ? LIMIT ?"),
                any(Object[].class));
    }
}

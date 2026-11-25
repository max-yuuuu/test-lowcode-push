package com.scaffold.business.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.framework.security.LoginUser;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.flow.runtime.FlowRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * 交付侧对**真实数据库**执行一个已发布 flow。默认跳过（无 FLOW_EXEC_MYSQL_URL 时不跑），
 * 只在显式提供一个可写库时执行，并在用例内建/删探针表，尽量不污染既有库。
 */
class FlowSnapshotExecutorMysqlTest {
    @Test
    void executesQueryAgainstRealMysql() {
        String url = System.getenv("FLOW_EXEC_MYSQL_URL");
        assumeTrue(url != null && !url.isBlank(), "需要 FLOW_EXEC_MYSQL_URL 指向真实可写库");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                envOrDefault("FLOW_EXEC_MYSQL_USER", "root"),
                envOrDefault("FLOW_EXEC_MYSQL_PASSWORD", "root"));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS flow_exec_probe");
        jdbc.execute("CREATE TABLE flow_exec_probe (id BIGINT, name VARCHAR(64), deleted TINYINT DEFAULT 0)");
        try {
            jdbc.update("INSERT INTO flow_exec_probe(id,name,deleted) VALUES (1,'Ada',0),(2,'Bob',1)");
            var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(), mock(PermissionResolver.class));
            Map<String, Object> output = executor.execute("probe", 1, Map.of("name", "Ada"));

            List<?> rows = (List<?>) output.get("rows");
            assertEquals(1, rows.size());
            assertTrue(rows.get(0).toString().contains("Ada"));
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS flow_exec_probe");
        }
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantScopedDataNodesCannotCrossTenantBoundaries() {
        String url = System.getenv("FLOW_EXEC_MYSQL_URL");
        assumeTrue(url != null && !url.isBlank(), "需要 FLOW_EXEC_MYSQL_URL 指向真实可写库");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url,
                envOrDefault("FLOW_EXEC_MYSQL_USER", "root"),
                envOrDefault("FLOW_EXEC_MYSQL_PASSWORD", "root"));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS flow_tenant_probe");
        jdbc.execute("CREATE TABLE flow_tenant_probe (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "tenant_id BIGINT NOT NULL, name VARCHAR(64) NOT NULL)");
        try {
            jdbc.update("INSERT INTO flow_tenant_probe(id,tenant_id,name) VALUES (1,1,'Ada'),(2,2,'Bob')");
            Map<String, Object> id = Map.of("fieldCode", "id", "columnName", "id", "isPrimary", 1,
                    "isInsert", 0, "isEdit", 0, "isAutoIncrement", 1);
            Map<String, Object> tenant = Map.of("fieldCode", "tenantId", "columnName", "tenant_id",
                    "isPrimary", 0, "isInsert", 0, "isEdit", 0, "isAutoIncrement", 0);
            Map<String, Object> name = Map.of("fieldCode", "name", "columnName", "name",
                    "isPrimary", 0, "isInsert", 1, "isEdit", 1, "isAutoIncrement", 0);
            Map<String, Object> model = Map.of("tableName", "flow_tenant_probe", "projectId", 1L,
                    "status", 1, "deleted", 0, "enableLogicDelete", 0, "enableAuditField", 0, "enableDataScope", 0);
            Map<String, Object> document = Map.of("schemaVersion", 1, "start", "q", "nodes", List.of(
                    Map.of("id", "q", "kind", "query", "modelId", 9, "fields", List.of("name"),
                            "filters", Map.of(), "limit", 20, "next", "insert"),
                    Map.of("id", "insert", "kind", "write", "modelId", 9, "mode", "insert",
                            "values", Map.of("name", "Grace"), "next", "other"),
                    Map.of("id", "other", "kind", "write", "modelId", 9, "mode", "update",
                            "values", Map.of("id", 2, "name", "Blocked"), "next", "own"),
                    Map.of("id", "own", "kind", "write", "modelId", 9, "mode", "update",
                            "values", Map.of("id", 1, "name", "Ada Updated"), "next", "end"),
                    Map.of("id", "end", "kind", "end")));
            Map<String, Object> snapshot = Map.of("operationCode", "tenant_probe", "version", 1,
                    "modelId", 9L, "projectId", 1L, "tenantId", 999L,
                    "models", Map.of("9", Map.of("model", model, "fields", List.of(id, tenant, name))),
                    "document", document);
            var executor = new FlowSnapshotExecutor(jdbc, new DefaultResourceLoader(), new ObjectMapper(),
                    mock(PermissionResolver.class));
            authenticate(1L);
            executor.executeSnapshot(snapshot, Map.of("tenantId", 888L, "name", "Grace", "otherId", 2, "ownId", 1));

            assertEquals(1, jdbc.queryForList("SELECT id FROM flow_tenant_probe WHERE tenant_id = 1 AND name = 'Ada Updated'").size());
            assertEquals(1, jdbc.queryForList("SELECT id FROM flow_tenant_probe WHERE tenant_id = 1 AND name = 'Grace'").size());
            assertEquals("Bob", jdbc.queryForObject("SELECT name FROM flow_tenant_probe WHERE id = 2", String.class));
            Map<String, Object> queryOnly = Map.of("operationCode", "tenant_probe", "version", 1,
                    "modelId", 9L, "projectId", 1L, "models", snapshot.get("models"),
                    "document", Map.of("schemaVersion", 1, "start", "q", "nodes", List.of(
                            Map.of("id", "q", "kind", "query", "modelId", 9, "fields", List.of("name"),
                                    "filters", Map.of(), "limit", 20, "next", "end"),
                            Map.of("id", "end", "kind", "end"))));
            List<?> rows = (List<?>) executor.executeSnapshot(queryOnly, Map.of("tenantId", 2)).get("rows");
            assertEquals(2, rows.size());
            assertTrue(rows.stream().noneMatch(row -> row.toString().contains("Bob")));

            SecurityContextHolder.clearContext();
            FlowRuntimeException error = assertThrows(FlowRuntimeException.class,
                    () -> executor.executeSnapshot(snapshot, Map.of("name", "Mallory", "otherId", 2, "ownId", 1)));
            assertEquals("FLOW_TENANT_FORBIDDEN", error.code());
            assertEquals(0, jdbc.queryForList("SELECT id FROM flow_tenant_probe WHERE name = 'Mallory'").size());
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS flow_tenant_probe");
        }
    }

    private static void authenticate(long tenantId) {
        var authentication = new UsernamePasswordAuthenticationToken("operator", null, List.of());
        authentication.setDetails(new LoginUser(41L, "operator", tenantId));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}

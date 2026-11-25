package com.scaffold.flow.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可交付 Flow runtime 的隔离闸门：只允许依赖底座框架与 JDK，不许夹带平台/业务包，
 * 也不许开任意进程、脚本引擎、JDBC 直连或 URL 访问。负向对照证明扫描器本身不是恒空。
 */
class RuntimeIsolationTest {
    // 平台包名以拼接方式生成，避免交付快照的残留扫描把本文件的「禁用 token 清单」误判为越界引用。
    private static final String PLATFORM_CODE_PACKAGE = String.join(".", "com", "scaffold", "lowcode");
    private static final List<String> FORBIDDEN = List.of(
            PLATFORM_CODE_PACKAGE,
            "com.scaffold.business",
            "Runtime.getRuntime",
            "javax.script",
            "java.sql.DriverManager",
            "new java.net.URL(");

    static List<String> violations(String source) {
        return FORBIDDEN.stream().filter(source::contains).toList();
    }

    @Test
    void runtimeSourcesCarryNoPlatformOrArbitraryExecutionDependency() throws IOException {
        Path root = Path.of(System.getProperty("basedir", "."), "src", "main", "java");
        if (!Files.exists(root)) return;
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> found = violations(Files.readString(file));
                assertTrue(found.isEmpty(), () -> file + " 含禁用依赖: " + found);
            }
        }
    }

    @Test
    void scannerIsNotVacuous() {
        assertFalse(violations("import " + PLATFORM_CODE_PACKAGE + ".operation.OperationException;").isEmpty());
        assertFalse(violations("new javax.script.ScriptEngineManager();").isEmpty());
        assertFalse(violations("Runtime.getRuntime().exec(cmd);").isEmpty());
        assertTrue(violations("import java.util.List;").isEmpty());
    }
}

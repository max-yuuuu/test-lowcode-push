package com.scaffold.admin.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * 本地开发库初始化工具：按版本号顺序导入底座建表脚本。
 * 连接信息与 scaffold-core.yml 的 dev profile 使用同一组环境变量（DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD）。
 * 传入参数 fix 时先删除并重建数据库。
 *
 * <p>这里是底座自己的那半（V1–V3）。平台仓的 SQL 住在 packages/scaffold-app/sql/，交付仓库的住在
 * scaffold-system/sql/，两种摆法相对启动模块的深度差一级，写死 {@code ../sql} 只成立一种，所以向上找
 * 含 V1__init_schema.sql 的目录（与租户拦截器那条用例同一判据）。平台自测库要两段 SQL 全导，
 * 由平台侧的 PlatformDbInit（不在交付项目里）复用这里的 {@link #initialize} 生命周期。
 */
public class DbInit {

    public static void main(String[] args) throws Exception {
        initialize(args, List.of(baseSqlDir()));
    }

    /**
     * 建库、按需重建、按版本顺序导入给定 SQL 目录里的顶层 V*__*.sql（跨目录合并后全局按版本号排序，不递归子目录）。
     * 供底座 {@link #main} 与平台侧 PlatformDbInit 共用同一组 DB_* 口径与幂等写法。
     */
    public static void initialize(String[] args, List<Path> sqlDirs) throws Exception {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "3306");
        String db = env("DB_NAME", "scaffold_system");
        String user = env("DB_USERNAME", "root");
        String pass = env("DB_PASSWORD", "root");
        if (!db.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("DB_NAME 只能包含字母、数字和下划线");
        String zone = env("DB_TIMEZONE", "+08:00");
        String params = "useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true";
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", pass);
        properties.setProperty("connectionTimeZone", zone);
        properties.setProperty("forceConnectionTimeZoneToSession", "true");
        boolean fix = args.length > 0 && "fix".equals(args[0]);

        try (Connection c = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/?%s", host, port, params), properties);
             Statement s = c.createStatement()) {
            if (fix) {
                System.out.println("删除并重建数据库: " + db);
                s.execute("DROP DATABASE IF EXISTS `" + db + "`");
            }
            s.execute("CREATE DATABASE IF NOT EXISTS `" + db + "` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci");
        }

        List<Path> files = new ArrayList<>();
        for (Path sqlDir : sqlDirs) {
            try (var stream = Files.list(sqlDir)) {
                stream.filter(f -> f.getFileName().toString().matches("V\\d+__.*\\.sql")).forEach(files::add);
            }
        }
        files.sort(Comparator.comparingInt(DbInit::version));

        try (Connection c = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?%s", host, port, db, params), properties);
             Statement s = c.createStatement()) {
            for (Path f : files) {
                System.out.println("导入: " + f.getFileName());
                s.execute(Files.readString(f));
            }
        }
        System.out.println("数据库初始化完成");
    }

    /** 向上找含 V1__init_schema.sql 的 sql 目录：底座在平台仓与交付仓库里深度差一级，两种摆法都认 */
    static Path baseSqlDir() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("sql");
            if (Files.isRegularFile(candidate.resolve("V1__init_schema.sql"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到底座建表脚本目录（含 sql/V1__init_schema.sql），请在后端模块目录下运行");
    }

    private static int version(Path file) {
        String name = file.getFileName().toString();
        return Integer.parseInt(name.substring(1, name.indexOf("__")));
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}

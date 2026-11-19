package com.scaffold.framework.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.scaffold.common.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MyBatis-Plus 租户条件注入处理器：为白名单表的 SELECT / INSERT / UPDATE / DELETE 自动补 tenant_id。
 *
 * <p>采用<b>白名单</b>而非「全部表减去少数表」：information_schema、无 tenant_id 的关联中间表等一律不注入，
 * 避免给公共查询凭空加上不存在的列。白名单由两份来源合成：底座自己的 {@link #BASE_TABLES} 与
 * {@link TenantTableContributor} 追加的部分 —— 谁的表谁登记，所以底座源码里不会有平台元数据表的名字。
 * 业务新增带 {@code tenant_id} 的表两条路都通：直接加进 {@code BASE_TABLES}（交付项目里那份文件就在
 * 自己的仓库里），或实现一个贡献者；走后者要把贡献者一并传进 {@code ScaffoldTenantLineHandlerTest}
 * 那条反向校验 —— 它默认读的是无贡献者的名单，否则会得到一格莫名的红。
 *
 * <p>匿名请求（登录、验证码、健康检查）没有租户上下文，此时整体跳过注入：这些链路本身按全局主键或
 * 用户名 + 租户编码定位，由 {@code AuthMapper} 显式写条件，不依赖拦截器。
 */
public class ScaffoldTenantLineHandler implements TenantLineHandler {

    /** 底座建表脚本里带 tenant_id 的业务表（小写表名） */
    public static final Set<String> BASE_TABLES = Set.of(
            "sys_dept",
            "sys_user",
            "sys_post",
            "sys_role",
            "sys_menu",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config",
            "sys_oper_log",
            "sys_login_log",
            "sys_file"
    );

    private final Set<String> tenantTables;

    /** 不带任何贡献者：等价于交付项目里合成后的那份（租户仓库里没有平台元数据表，也就没有贡献者） */
    public ScaffoldTenantLineHandler() {
        this(List.of());
    }

    public ScaffoldTenantLineHandler(List<TenantTableContributor> contributors) {
        Set<String> merged = new LinkedHashSet<>(BASE_TABLES);
        for (TenantTableContributor contributor : contributors) {
            for (String table : contributor.tenantTables()) {
                merged.add(normalize(table));
            }
        }
        this.tenantTables = Set.copyOf(merged);
    }

    /** 当前生效的白名单，供装配检查与测试读取 */
    public Set<String> tenantTables() {
        return tenantTables;
    }

    private static final String TENANT_COLUMN = "tenant_id";

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.getTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantContext.getTenantId() == null || !tenantTables.contains(normalize(tableName));
    }

    /** 去掉 schema 前缀与反引号，统一小写，兼容 `db`.`sys_user` 与 INFORMATION_SCHEMA 写法 */
    private static String normalize(String tableName) {
        if (tableName == null) {
            return "";
        }
        String name = tableName.toLowerCase().replace("`", "").trim();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }
}

package com.scaffold.framework.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.scaffold.framework.tenant.ScaffoldTenantLineHandler;
import com.scaffold.framework.tenant.TenantTableContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MyBatis-Plus 插件注册。当前只挂多租户条件注入；后续加分页等内部插件时，
 * TenantLineInnerInterceptor 必须排在最前，否则分页 count 统计不到租户条件过滤后的行数。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 白名单单独成一个 bean：装配对不对（贡献者有没有被收进来）要能在测试里直接读，
     * 而不是藏在拦截器内部取不出来。没有贡献者时就是交付项目拿到的那半份。
     */
    @Bean
    public ScaffoldTenantLineHandler scaffoldTenantLineHandler(List<TenantTableContributor> contributors) {
        return new ScaffoldTenantLineHandler(contributors);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ScaffoldTenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        return interceptor;
    }
}

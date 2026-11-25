package com.scaffold.framework.flow;

import com.scaffold.common.tenant.TenantContext;
import com.scaffold.framework.datascope.DataScopeCriteria;
import com.scaffold.framework.datascope.DataScopeResolver;
import com.scaffold.framework.security.LoginUser;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Creates Flow identity exclusively from the authenticated server request context. */
@Component
public final class FlowExecutionContextFactory {
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(30);
    private final PermissionResolver permissionResolver;
    private final DataScopeResolver dataScopeResolver;

    public FlowExecutionContextFactory(PermissionResolver permissionResolver, DataScopeResolver dataScopeResolver) {
        this.permissionResolver = permissionResolver;
        this.dataScopeResolver = dataScopeResolver;
    }

    public FlowExecutionContext create(Map<String, Object> input, long projectId, long modelId,
                                       long operationId, int operationVersion, String operationPermission,
                                       Duration timeout) {
        if (projectId <= 0 || modelId <= 0 || operationId <= 0 || operationVersion <= 0
                || timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(MAX_TIMEOUT) > 0) {
            throw new IllegalArgumentException("Invalid Flow execution binding or timeout");
        }
        LoginUser subject = SecurityUtils.getLoginUser();
        Long tenant = TenantContext.getTenantId();
        if (subject.userId() == null || subject.tenantId() == null || !subject.tenantId().equals(tenant)) {
            throw new AccessDeniedException("可信租户上下文不一致");
        }
        if (operationPermission == null || operationPermission.isBlank()
                || !permissionResolver.hasPermission(subject, operationPermission)) {
            throw new AccessDeniedException("无权执行该接口");
        }
        DataScopeCriteria scope = dataScopeResolver.resolve(subject);
        if (scope == null) throw new AccessDeniedException("无法解析数据权限范围");
        return new FlowExecutionContext(input == null ? Map.of() : input, subject, tenant, projectId, modelId,
                operationId, operationVersion, operationPermission, UUID.randomUUID().toString(), Instant.now().plus(timeout),
                Set.of(operationPermission), scope);
    }
}

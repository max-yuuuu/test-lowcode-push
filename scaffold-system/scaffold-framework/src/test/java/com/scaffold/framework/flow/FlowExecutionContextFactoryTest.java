package com.scaffold.framework.flow;

import com.scaffold.common.tenant.TenantContext;
import com.scaffold.framework.datascope.DataScopeCriteria;
import com.scaffold.framework.datascope.DataScopeResolver;
import com.scaffold.framework.security.LoginUser;
import com.scaffold.framework.security.PermissionResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowExecutionContextFactoryTest {
    private final PermissionResolver permissions = mock(PermissionResolver.class);
    private final DataScopeResolver scopes = mock(DataScopeResolver.class);
    private final FlowExecutionContextFactory factory = new FlowExecutionContextFactory(permissions, scopes);

    @AfterEach void clean() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test void createsContextFromAuthenticatedSubjectAndTenantOnly() {
        LoginUser user = authenticate(41L, 7L);
        TenantContext.set(7L);
        when(permissions.hasPermission(user, "order:create")).thenReturn(true);
        when(scopes.resolve(user)).thenReturn(DataScopeCriteria.selfOnly(41L));
        Map<String, Object> input = Map.of("userId", 999L, "tenantId", 999L, "permission", "admin");

        FlowExecutionContext context = factory.create(input, 2, 3, 4, 1, "order:create", Duration.ofSeconds(5));

        assertThat(context.subject()).isEqualTo(user);
        assertThat(context.tenantId()).isEqualTo(7L);
        assertThat(context.input()).isEqualTo(input);
        assertThat(context.permissions()).containsExactly("order:create");
        assertThat(context.dataScope().getUserId()).isEqualTo(41L);
        assertThat(context.deadline()).isAfter(java.time.Instant.now());
    }

    @Test void snapshotsNestedInputSoCallerCannotMutateExecutionContext() {
        LoginUser user = authenticate(41L, 7L);
        TenantContext.set(7L);
        when(permissions.hasPermission(user, "order:create")).thenReturn(true);
        when(scopes.resolve(user)).thenReturn(DataScopeCriteria.selfOnly(41L));
        var nested = new java.util.ArrayList<>(java.util.List.of("before"));
        Map<String, Object> input = new java.util.LinkedHashMap<>();
        input.put("payload", nested);

        FlowExecutionContext context = factory.create(input, 2, 3, 4, 1, "order:create", Duration.ofSeconds(5));
        nested.set(0, "after");

        assertThat(context.input()).containsEntry("payload", java.util.List.of("before"));
        assertThatThrownBy(() -> ((java.util.List<Object>) context.input().get("payload")).add("mutate"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void rejectsTenantContextMismatchBeforeResolvingAuthorization() {
        authenticate(41L, 7L);
        TenantContext.set(8L);

        assertThatThrownBy(() -> factory.create(Map.of(), 2, 3, 4, 1, "order:create", Duration.ofSeconds(5)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void rejectsMissingOperationPermission() {
        LoginUser user = authenticate(41L, 7L);
        TenantContext.set(7L);
        when(permissions.hasPermission(user, "order:create")).thenReturn(false);

        assertThatThrownBy(() -> factory.create(Map.of(), 2, 3, 4, 1, "order:create", Duration.ofSeconds(5)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static LoginUser authenticate(long userId, long tenantId) {
        LoginUser user = new LoginUser(userId, "operator", tenantId);
        var authentication = new UsernamePasswordAuthenticationToken("operator", "", java.util.List.of());
        authentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }
}

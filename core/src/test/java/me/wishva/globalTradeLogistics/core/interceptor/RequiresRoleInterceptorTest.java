package me.wishva.globalTradeLogistics.core.interceptor;

import jakarta.interceptor.InvocationContext;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequiresRoleInterceptorTest {

    private final RequiresRoleInterceptor interceptor = new RequiresRoleInterceptor();

    @RequiresRole(Role.ADMIN)
    static class ClassLevelBean {
        void classLevelMethod() {
        }

        @RequiresRole(Role.COORDINATOR)
        void methodLevelOverride() {
        }
    }

    static class MultiRoleBean {
        @RequiresRole({Role.ADMIN, Role.COORDINATOR})
        void multiRoleMethod() {
        }
    }

    @AfterEach
    void clearPrincipal() {
        CurrentPrincipalHolder.clear();
    }

    private InvocationContext contextFor(Object target, String methodName, Object proceedResult) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        InvocationContext context = mock(InvocationContext.class);
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(target);
        when(context.proceed()).thenReturn(proceedResult);
        return context;
    }

    @Test
    void allowsCall_whenPrincipalRoleMatchesClassLevelAnnotation() throws Exception {
        CurrentPrincipalHolder.set(new CurrentPrincipal("admin@example.com", Role.ADMIN));
        InvocationContext context = contextFor(new ClassLevelBean(), "classLevelMethod", "ok");

        Object result = interceptor.authorize(context);

        assertEquals("ok", result);
    }

    @Test
    void allowsCall_whenPrincipalRoleMatchesOneOfMultipleAllowedRoles() throws Exception {
        CurrentPrincipalHolder.set(new CurrentPrincipal("coordinator@example.com", Role.COORDINATOR));
        InvocationContext context = contextFor(new MultiRoleBean(), "multiRoleMethod", "ok");

        Object result = interceptor.authorize(context);

        assertEquals("ok", result);
    }

    @Test
    void throwsUnauthorized_whenPrincipalRoleDoesNotMatch() throws Exception {
        CurrentPrincipalHolder.set(new CurrentPrincipal("warehouse@example.com", Role.WAREHOUSE_MANAGER));
        InvocationContext context = contextFor(new ClassLevelBean(), "classLevelMethod", "ok");

        assertThrows(UnauthorizedAccessException.class, () -> interceptor.authorize(context));
    }

    @Test
    void throwsUnauthorized_whenNoPrincipalIsSet() throws Exception {
        InvocationContext context = contextFor(new ClassLevelBean(), "classLevelMethod", "ok");

        assertThrows(UnauthorizedAccessException.class, () -> interceptor.authorize(context));
    }

    @Test
    void methodLevelAnnotation_overridesClassLevelRequirement() throws Exception {
        // ClassLevelBean requires ADMIN, but methodLevelOverride requires COORDINATOR instead.
        CurrentPrincipalHolder.set(new CurrentPrincipal("coordinator@example.com", Role.COORDINATOR));
        InvocationContext context = contextFor(new ClassLevelBean(), "methodLevelOverride", "ok");

        Object result = interceptor.authorize(context);

        assertEquals("ok", result);

        // The same COORDINATOR principal is still rejected by the class-level ADMIN requirement.
        CurrentPrincipalHolder.clear();
        CurrentPrincipalHolder.set(new CurrentPrincipal("coordinator@example.com", Role.COORDINATOR));
        InvocationContext classLevelContext = contextFor(new ClassLevelBean(), "classLevelMethod", "ok");
        assertThrows(UnauthorizedAccessException.class, () -> interceptor.authorize(classLevelContext));
    }
}

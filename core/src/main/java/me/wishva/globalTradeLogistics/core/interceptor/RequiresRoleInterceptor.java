package me.wishva.globalTradeLogistics.core.interceptor;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.lang.reflect.Method;

/**
 * Enforces {@link RequiresRole}: reads the current, JWT-derived principal
 * (set by api-gateway's {@code JwtAuthFilter} on the same thread) and
 * rejects the invocation unless its role is in the annotation's allowed
 * set. Method-level {@code @RequiresRole} takes precedence over a
 * class-level one, so one bean can mix several role requirements across
 * its business methods — associating multiple business interceptor targets
 * with a single enterprise bean, per the assignment's interceptor
 * learning outcome.
 */
public class RequiresRoleInterceptor {

    @Inject
    private Event<LogEvent> logEvent;

    @AroundInvoke
    public Object authorize(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        RequiresRole binding = method.getAnnotation(RequiresRole.class);
        if (binding == null) {
            binding = context.getTarget().getClass().getAnnotation(RequiresRole.class);
        }

        if (binding != null) {
            CurrentPrincipal principal = CurrentPrincipalHolder.get();
            if (principal == null) {
                logEvent.fire(new LogEvent("auth", LogLevel.WARN,
                        "RequiresRoleInterceptor: no authenticated principal for " + method.getName()));
                throw new UnauthorizedAccessException(
                        "No authenticated principal for role-protected method " + method.getName());
            }

            boolean allowed = false;
            for (Role role : binding.value()) {
                if (role == principal.getRole()) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                logEvent.fire(new LogEvent(principal.getEmail(), LogLevel.WARN,
                        "RequiresRoleInterceptor: role " + principal.getRole() + " denied for " + method.getName()));
                throw new UnauthorizedAccessException(
                        "Role " + principal.getRole() + " is not permitted to call " + method.getName());
            }
        }

        return context.proceed();
    }
}

package me.wishva.globalTradeLogistics.core.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.dto.OrderSummary;
import me.wishva.globalTradeLogistics.core.messaging.AuditPublisher;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.lang.reflect.Method;

/**
 * Enforces {@link Audited}: on successful completion of the intercepted
 * method, publishes an {@link AuditEvent} (async, via {@link AuditPublisher})
 * naming the caller, the resource, and the method invoked. Method-level
 * {@code @Audited} takes precedence over class-level, same lookup order as
 * {@link RequiresRoleInterceptor}.
 * <p>
 * Only fires after {@link InvocationContext#proceed()} returns normally —
 * a thrown business exception (e.g. {@code InsufficientInventoryException})
 * skips the audit entry entirely, since nothing was actually committed.
 */
public class AuditInterceptor {

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        Object result = context.proceed();

        Method method = context.getMethod();
        Audited binding = method.getAnnotation(Audited.class);
        if (binding == null) {
            binding = context.getTarget().getClass().getAnnotation(Audited.class);
        }

        if (binding != null) {
            CurrentPrincipal principal = CurrentPrincipalHolder.get();
            String actorEmail = principal != null ? principal.getEmail() : "unknown";
            String reference = (result instanceof OrderSummary)
                    ? String.valueOf(((OrderSummary) result).getOrderId())
                    : null;
            AuditPublisher.publish(new AuditEvent(binding.resource(), method.getName(), actorEmail, reference));
        }

        return result;
    }
}

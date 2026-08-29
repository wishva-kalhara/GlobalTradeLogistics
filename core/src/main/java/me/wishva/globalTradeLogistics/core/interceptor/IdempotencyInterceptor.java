package me.wishva.globalTradeLogistics.core.interceptor;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.idempotency.IdempotencyKeyRegistry;

import java.lang.reflect.Method;

/**
 * Enforces {@link IdempotencyChecked}: a synchronous fast-path check against
 * {@link IdempotencyKeyRegistry} before letting the business method run — if
 * the key has already been recorded, the call short-circuits with no further
 * writes. On first-seen keys, proceeds and records the key in the registry
 * after a successful return.
 * <p>
 * Runs <em>before</em> {@link AuditInterceptor} when both are listed in a
 * bean's {@code @Interceptors}, per {@code IdempotencyChecked}/{@code Audited}
 * ordering documented for {@code ShipmentServiceBean} — list
 * {@code IdempotencyInterceptor} earlier than {@code AuditInterceptor} so a
 * short-circuited (already-seen) call never reaches the business method and
 * therefore never gets audited either.
 */
public class IdempotencyInterceptor {

    private final IdempotencyKeyRegistry idempotencyKeys = IdempotencyKeyRegistry.getInstance();

    @Inject
    private Event<LogEvent> logEvent;

    @AroundInvoke
    public Object checkIdempotency(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        if (!method.isAnnotationPresent(IdempotencyChecked.class)) {
            return context.proceed();
        }

        Object[] params = context.getParameters();
        String idempotencyKey = (String) params[params.length - 1];

        if (idempotencyKeys.hasSeen(idempotencyKey)) {
            logEvent.fire(new LogEvent(idempotencyKey, LogLevel.TRACE,
                    "IdempotencyInterceptor: short-circuiting duplicate key for " + method.getName()));
            return null;
        }

        Object result = context.proceed();
        idempotencyKeys.record(idempotencyKey);
        return result;
    }
}

package me.wishva.globalTradeLogistics.core.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.IdempotencyEvent;
import me.wishva.globalTradeLogistics.core.messaging.IdempotencyPublisher;

import java.lang.reflect.Method;

/**
 * Enforces {@link IdempotencyChecked}: a synchronous fast-path check against
 * {@code logs} (via {@code LogEntry.countByIdempotencyKey}) before letting
 * the business method run — if the key has already been recorded, the call
 * short-circuits with no further writes. On first-seen keys, proceeds and
 * then asynchronously publishes an {@link IdempotencyEvent} (via
 * {@link IdempotencyPublisher}) so {@code monitoring-svc} (Phase 6) can
 * durably record it.
 * <p>
 * Interceptor classes support the same resource injection as the managed
 * bean they're bound to, so {@code @PersistenceContext} here is populated
 * from whichever EJB this interceptor is attached to.
 * <p>
 * Runs <em>before</em> {@link AuditInterceptor} when both are listed in a
 * bean's {@code @Interceptors}, per {@code IdempotencyChecked}/{@code Audited}
 * ordering documented for {@code ShipmentServiceBean} — list
 * {@code IdempotencyInterceptor} earlier than {@code AuditInterceptor} so a
 * short-circuited (already-seen) call never reaches the business method and
 * therefore never gets audited either.
 */
public class IdempotencyInterceptor {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @AroundInvoke
    public Object checkIdempotency(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        if (!method.isAnnotationPresent(IdempotencyChecked.class)) {
            return context.proceed();
        }

        Object[] params = context.getParameters();
        String idempotencyKey = (String) params[params.length - 1];

        long alreadySeen = em.createNamedQuery("LogEntry.countByIdempotencyKey", Long.class)
                .setParameter("idempotencyKey", idempotencyKey)
                .getSingleResult();
        if (alreadySeen > 0) {
            return null;
        }

        Object result = context.proceed();
        IdempotencyPublisher.publish(new IdempotencyEvent(idempotencyKey, method.getDeclaringClass().getName(), method.getName()));
        return result;
    }
}

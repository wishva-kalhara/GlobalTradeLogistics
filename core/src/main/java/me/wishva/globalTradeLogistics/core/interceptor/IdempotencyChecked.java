package me.wishva.globalTradeLogistics.core.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as idempotency-checked, enforced by
 * {@link IdempotencyInterceptor}. Method-level only (unlike {@link Audited},
 * which also supports class-level) — an idempotency key is inherently a
 * per-invocation argument, not a class-wide concern.
 * <p>
 * Convention: the annotated method's <b>last parameter</b> must be the
 * {@code String} idempotency key (e.g.
 * {@code updateStatus(Integer shipmentId, ShipmentStatus newStatus, String idempotencyKey)}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyChecked {
}

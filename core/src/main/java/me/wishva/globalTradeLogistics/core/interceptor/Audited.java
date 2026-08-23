package me.wishva.globalTradeLogistics.core.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bean (class-level, per the assignment's class-level interceptor
 * example — {@code OrderServiceBean}) or a single method for audit logging,
 * enforced by {@link AuditInterceptor}. {@code resource} names the domain
 * entity being acted on (e.g. {@code "ORDER"}) — the action itself is taken
 * from the intercepted method's name.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    String resource();

    /**
     * The {@code audit_records.type} value, distinct from {@code resource}
     * when a bean records more than one kind of event (e.g. procurement-svc
     * distinguishes plain PO/GRN activity from the weekly vendor-performance
     * recompute). Defaults to {@link #resource()} when not given.
     */
    String type() default "";
}

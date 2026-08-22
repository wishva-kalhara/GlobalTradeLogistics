package me.wishva.globalTradeLogistics.core.interceptor;

import me.wishva.globalTradeLogistics.core.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative role check, enforced by {@link RequiresRoleInterceptor}.
 * Applied at class level (every business method requires one of the given
 * roles) or method level (overrides the class-level requirement for that
 * one method) — classic EJB {@code @Interceptors} association, not a CDI
 * interceptor binding, since authorization here is JWT-based rather than
 * container-managed (JAAS) security.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    Role[] value();
}

package me.wishva.globalTradeLogistics.apiGateway.security;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JAX-RS name-binding: marks a resource class/method as requiring a valid
 * JWT, binding it to {@link JwtAuthFilter}. Replaces the old
 * {@code @WebFilter(urlPatterns = ...)} approach with something that moves
 * with the resource instead of a URL-pattern string.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
}

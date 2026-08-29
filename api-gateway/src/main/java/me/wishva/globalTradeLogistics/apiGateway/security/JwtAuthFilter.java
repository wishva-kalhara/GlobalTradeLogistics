package me.wishva.globalTradeLogistics.apiGateway.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.InvalidTokenException;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;
import me.wishva.globalTradeLogistics.core.security.JwtService;

import java.io.IOException;
import java.util.Map;

/**
 * Flow 1.9 — validates the {@code Authorization: Bearer <jwt>} header on
 * every {@code @Secured} JAX-RS resource, populating
 * {@link CurrentPrincipalHolder} for the EJB-tier {@code @RequiresRole}
 * interceptor (1.10) to read. Implements both request and response filter
 * interfaces so the ThreadLocal is always cleared once the response is
 * written, even though this is a request-scoped Jersey component per call.
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        try {
            String header = requestContext.getHeaderString("Authorization");
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                logEvent.fire(new LogEvent(path, LogLevel.WARN, "JwtAuthFilter: missing bearer token"));
                throw new InvalidTokenException("Missing bearer token");
            }

            String token = header.substring(BEARER_PREFIX.length());
            CurrentPrincipal principal = JwtService.parseAndValidate(token, AppConfig.JWT_SECRET);
            CurrentPrincipalHolder.set(principal);
            logEvent.fire(new LogEvent(principal.getEmail(), LogLevel.TRACE, "JwtAuthFilter: authenticated for " + path));
        } catch (InvalidTokenException e) {
            logEvent.fire(new LogEvent(path, LogLevel.WARN, "JwtAuthFilter: rejected - " + e.getMessage()));
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", e.getMessage()))
                    .build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        CurrentPrincipalHolder.clear();
    }
}

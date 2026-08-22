package me.wishva.globalTradeLogistics.apiGateway;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates JAX-RS under {@code /v1/...}. Left empty deliberately — an
 * {@code Application} subclass with no overridden {@code getClasses()}/
 * {@code getSingletons()} makes Jersey classpath-scan for every
 * {@code @Path} resource and {@code @Provider} filter/mapper in this WAR,
 * so new resources never need to be registered here by hand.
 */
@ApplicationPath("/v1")
public class ApiApplication extends Application {
}

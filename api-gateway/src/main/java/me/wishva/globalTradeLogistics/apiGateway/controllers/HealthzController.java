package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;

/**
 * JAX-RS port of the original {@code @WebServlet} — every other endpoint in
 * this module is a JAX-RS resource under {@code ApiApplication}'s
 * {@code @ApplicationPath("/v1")}, so this is the one holdover made
 * consistent (7.7).
 */
@Path("/healthz")
public class HealthzController {

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String healthz() {
        logEvent.fire(new LogEvent("healthz", LogLevel.TRACE, "GET /healthz"));
        return "Up and running";
    }
}

package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS port of the original {@code @WebServlet} — every other endpoint in
 * this module is a JAX-RS resource under {@code ApiApplication}'s
 * {@code @ApplicationPath("/v1")}, so this is the one holdover made
 * consistent (7.7).
 */
@Path("/healthz")
public class HealthzController {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String healthz() {
        return "Up and running";
    }
}

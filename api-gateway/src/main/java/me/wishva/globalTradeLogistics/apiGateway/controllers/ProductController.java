package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.ProductSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.local.IProductService;

import java.util.List;

/**
 * Read-only product catalog — unprotected, so the customer frontend can
 * render the "browse products" page before/without a session.
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductController {

    @EJB
    private IProductService productService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    public List<ProductSummary> listProducts() {
        logEvent.fire(new LogEvent("products-list", LogLevel.TRACE, "GET /products"));
        return productService.listProducts();
    }
}

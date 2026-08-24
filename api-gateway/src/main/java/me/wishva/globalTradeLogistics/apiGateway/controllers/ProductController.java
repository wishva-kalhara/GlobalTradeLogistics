package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.core.dto.ProductSummary;
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

    @GET
    public List<ProductSummary> listProducts() {
        return productService.listProducts();
    }
}

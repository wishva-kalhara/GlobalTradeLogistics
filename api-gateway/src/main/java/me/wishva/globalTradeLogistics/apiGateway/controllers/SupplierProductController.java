package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.AddProductOfferingBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IPurchaseOrderService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

/**
 * A supplier registering which products they can provide, from which
 * warehouse, and their lead time — {@code supplier_providing_products}.
 * {@code @RequiresRole(VENDOR_REP)} at the EJB layer resolves the caller's
 * own {@code supplier_id}; the path carries no id.
 */
@Path("/suppliers/me/products")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
public class SupplierProductController {

    @EJB
    private IPurchaseOrderService purchaseOrderService;

    @Inject
    private Event<LogEvent> logEvent;

    @POST
    public Response addProductOffering(AddProductOfferingBody body)
            throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /suppliers/me/products"));
        if (body == null || body.getProductId() == null || body.getWarehouseId() == null
                || body.getLeadTimeInDays() == null || body.getLeadTimeInDays() < 0) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                    "addProductOffering: productId, warehouseId and a non-negative leadTimeInDays are required"));
            throw new BadRequestException("productId, warehouseId and a non-negative leadTimeInDays are required");
        }
        purchaseOrderService.addProductOffering(body.getProductId(), body.getWarehouseId(), body.getLeadTimeInDays());
        return Response.status(Response.Status.CREATED).build();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "supplier-products";
    }
}

package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreatePurchaseOrderBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreateShipmentBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.dto.ShipmentSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IPurchaseOrderService;
import me.wishva.globalTradeLogistics.core.local.IShipmentService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.List;

/**
 * Purchase order lifecycle — a pure pass-through to procurement-svc's
 * {@code IPurchaseOrderService} and logistics-svc's {@code IShipmentService},
 * per api-gateway's "REST façade, no new business logic" convention. Role
 * enforcement ({@code COORDINATOR} for creating, {@code VENDOR_REP} for the
 * supplier's own list/shippable-list/shipment-creation) happens at the EJB
 * layer via {@code @RequiresRole} — {@code @Secured} here only requires a
 * valid JWT. GRN recording moved to {@link ShipmentController} — see its
 * javadoc for why.
 */
@Path("/purchase-orders")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PurchaseOrderController {

    @EJB
    private IPurchaseOrderService purchaseOrderService;

    @EJB
    private IShipmentService shipmentService;

    @Inject
    private Event<LogEvent> logEvent;

    @POST
    public PurchaseOrderSummary createPo(CreatePurchaseOrderBody body) throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /purchase-orders"));
        if (body == null || body.getSupplierId() == null || body.getProductId() == null
                || body.getQty() == null || body.getQty() <= 0) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                    "createPo: supplierId, productId and a positive qty are required"));
            throw new BadRequestException("supplierId, productId and a positive qty are required");
        }
        return purchaseOrderService.createPo(body.getSupplierId(), body.getProductId(), body.getQty());
    }

    @GET
    public List<PurchaseOrderSummary> listForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /purchase-orders"));
        return purchaseOrderService.listForSupplier();
    }

    @GET
    @Path("/shippable")
    public List<PurchaseOrderSummary> listShippableForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /purchase-orders/shippable"));
        return purchaseOrderService.listShippableForSupplier();
    }

    @POST
    @Path("/{poId}/shipment")
    public ShipmentSummary createShipment(@PathParam("poId") Integer poId, CreateShipmentBody body)
            throws PurchaseOrderNotFoundException, InvalidShipmentStateException, UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /purchase-orders/" + poId + "/shipment"));
        if (body == null || body.getTrackingNumber() == null || body.getTrackingNumber().isBlank()
                || body.getVesselId() == null || body.getVesselId().isBlank()
                || body.getType() == null || body.getType().isBlank()) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                    "createShipment: trackingNumber, vesselId and type are required"));
            throw new BadRequestException("trackingNumber, vesselId and type are required");
        }
        return shipmentService.createShipmentForPurchaseOrder(poId, body.getTrackingNumber(), body.getVesselId(), body.getType());
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "purchase-orders";
    }
}

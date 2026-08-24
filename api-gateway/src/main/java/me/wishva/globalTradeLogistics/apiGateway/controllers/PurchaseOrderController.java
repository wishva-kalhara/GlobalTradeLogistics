package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreatePurchaseOrderBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RecordGrnBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IPurchaseOrderService;

import java.util.List;

/**
 * Purchase order lifecycle — a pure pass-through to procurement-svc's
 * {@code IPurchaseOrderService}, per api-gateway's "REST façade, no new
 * business logic" convention. Role enforcement ({@code COORDINATOR} for
 * creating, {@code WAREHOUSE_MANAGER} for recording a GRN, {@code VENDOR_REP}
 * for the supplier's own list) happens at the EJB layer via
 * {@code @RequiresRole} — {@code @Secured} here only requires a valid JWT.
 */
@Path("/purchase-orders")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PurchaseOrderController {

    @EJB
    private IPurchaseOrderService purchaseOrderService;

    @POST
    public PurchaseOrderSummary createPo(CreatePurchaseOrderBody body) throws UnauthorizedAccessException {
        if (body == null || body.getSupplierId() == null || body.getProductId() == null
                || body.getQty() == null || body.getQty() <= 0) {
            throw new BadRequestException("supplierId, productId and a positive qty are required");
        }
        return purchaseOrderService.createPo(body.getSupplierId(), body.getProductId(), body.getQty());
    }

    @POST
    @Path("/{poId}/grn")
    public PurchaseOrderSummary recordGrn(@PathParam("poId") Integer poId, RecordGrnBody body)
            throws PurchaseOrderNotFoundException, UnauthorizedAccessException {
        if (body == null || body.getQty() == null || body.getQty() <= 0) {
            throw new BadRequestException("A positive qty is required");
        }
        return purchaseOrderService.recordGrn(poId, body.getQty());
    }

    @GET
    public List<PurchaseOrderSummary> listForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException {
        return purchaseOrderService.listForSupplier();
    }
}

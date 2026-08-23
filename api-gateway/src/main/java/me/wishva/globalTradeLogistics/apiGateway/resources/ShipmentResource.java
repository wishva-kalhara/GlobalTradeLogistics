package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreateCustomsRecordBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.UpdateShipmentStatusBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.ShipmentSummary;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IShipmentService;

/**
 * Shipment status/customs-clearance flows — a pure pass-through to
 * logistics-svc's {@code IShipmentService}. {@code @RequiresRole(CUSTOMS_AGENT)}
 * is enforced at the EJB layer; {@code @Secured} here only requires a valid
 * JWT.
 */
@Path("/shipments")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    @EJB
    private IShipmentService shipmentService;

    @GET
    @Path("/{shipmentId}")
    public ShipmentSummary getShipment(@PathParam("shipmentId") Integer shipmentId) throws ShipmentNotFoundException {
        return shipmentService.getShipment(shipmentId);
    }

    @PUT
    @Path("/{shipmentId}/status")
    public ShipmentSummary updateStatus(@PathParam("shipmentId") Integer shipmentId, UpdateShipmentStatusBody body)
            throws ShipmentNotFoundException, UnauthorizedAccessException {
        if (body == null || body.getStatus() == null || body.getIdempotencyKey() == null || body.getIdempotencyKey().isBlank()) {
            throw new BadRequestException("status and idempotencyKey are required");
        }

        ShipmentStatus status;
        try {
            status = ShipmentStatus.valueOf(body.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown status: " + body.getStatus());
        }

        ShipmentSummary result = shipmentService.updateStatus(shipmentId, status, body.getIdempotencyKey());
        // A null result means IdempotencyInterceptor short-circuited an already-seen
        // key — the shipment wasn't touched again, so re-fetch its current state.
        return result != null ? result : shipmentService.getShipment(shipmentId);
    }

    @POST
    @Path("/{shipmentId}/customs")
    public Response createCustomsRecord(@PathParam("shipmentId") Integer shipmentId, CreateCustomsRecordBody body)
            throws ShipmentNotFoundException, UnauthorizedAccessException {
        String declarationNumber = body != null ? body.getDeclarationNumber() : null;
        shipmentService.createCustomsRecord(shipmentId, declarationNumber);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/{shipmentId}/notify-carrier")
    public ShipmentSummary notifyCarrierSystem(@PathParam("shipmentId") Integer shipmentId) throws ShipmentNotFoundException {
        return shipmentService.notifyCarrierSystem(shipmentId);
    }
}

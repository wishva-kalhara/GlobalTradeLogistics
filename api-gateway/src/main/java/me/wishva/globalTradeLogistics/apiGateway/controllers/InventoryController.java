package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.InventorySummary;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.WarehouseSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IInventoryService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.List;

/**
 * Read-only per-warehouse stock view — a pure pass-through to
 * inventory-svc's {@code IInventoryService.listByWarehouse}.
 * {@code @RequiresRole({WAREHOUSE_MANAGER, COORDINATOR, ADMIN})} is enforced
 * at the EJB layer; {@code @Secured} here only requires a valid JWT.
 */
@Path("/inventory")
@Secured
@Produces(MediaType.APPLICATION_JSON)
public class InventoryController {

    @EJB
    private IInventoryService inventoryService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    @Path("/warehouses")
    public List<WarehouseSummary> listWarehouses() throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /inventory/warehouses"));
        return inventoryService.listWarehouses();
    }

    @GET
    @Path("/{warehouseId}")
    public List<InventorySummary> listByWarehouse(@PathParam("warehouseId") Integer warehouseId)
            throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /inventory/" + warehouseId));
        return inventoryService.listByWarehouse(warehouseId);
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "inventory";
    }
}

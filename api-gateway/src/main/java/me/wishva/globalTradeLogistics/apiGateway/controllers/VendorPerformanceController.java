package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.AuditRecordSummary;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IVendorPerformanceService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.List;

/**
 * Read-only view of the weekly vendor-performance recompute's audit trail
 * ({@code audit_records} where {@code type = 'VENDOR_PERFORMANCE'}) — a pure
 * pass-through to procurement-svc's {@code IVendorPerformanceService}.
 * {@code @RequiresRole({ADMIN, COORDINATOR})} is enforced at the EJB layer.
 */
@Path("/admin/vendor-performance")
@Secured
@Produces(MediaType.APPLICATION_JSON)
public class VendorPerformanceController {

    @EJB
    private IVendorPerformanceService vendorPerformanceService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    public List<AuditRecordSummary> listReports() throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /admin/vendor-performance"));
        return vendorPerformanceService.listVendorPerformanceReports();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "vendor-performance";
    }
}

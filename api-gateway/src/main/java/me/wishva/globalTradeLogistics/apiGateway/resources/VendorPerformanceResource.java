package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.AuditRecordSummary;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IVendorPerformanceService;

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
public class VendorPerformanceResource {

    @EJB
    private IVendorPerformanceService vendorPerformanceService;

    @GET
    public List<AuditRecordSummary> listReports() throws UnauthorizedAccessException {
        return vendorPerformanceService.listVendorPerformanceReports();
    }
}

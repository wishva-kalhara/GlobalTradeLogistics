package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.AuditRecordSummary;
import me.wishva.globalTradeLogistics.core.dto.VendorPerformanceResult;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

import java.util.List;

@Local
public interface IVendorPerformanceService {

    /**
     * Recomputes and records one supplier's on-time delivery performance.
     * {@code REQUIRES_NEW} — each supplier's recompute is its own
     * transaction, so one failure (e.g. a bad row) can't roll back the
     * whole weekly batch ({@code VendorPerformanceTimerBean} calls this once
     * per supplier, in a loop).
     */
    VendorPerformanceResult recomputeForSupplier(Integer supplierId);

    List<AuditRecordSummary> listVendorPerformanceReports() throws UnauthorizedAccessException;
}

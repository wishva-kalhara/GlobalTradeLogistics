package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.local.IVendorPerformanceService;

import java.util.List;

/**
 * Declarative weekly timer — loops every supplier, recomputing performance
 * for each in its own {@code REQUIRES_NEW} transaction (see
 * {@link IVendorPerformanceService#recomputeForSupplier}) so one bad
 * supplier's data can't roll back the whole batch.
 */
@Singleton
public class VendorPerformanceTimerBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @EJB
    private IVendorPerformanceService vendorPerformanceService;

    @Inject
    private Event<LogEvent> logEvent;

    @Schedule(dayOfWeek = "Mon", hour = "3", persistent = true)
    void recomputeAllSuppliers() {
        List<Integer> supplierIds = em.createQuery("SELECT s.supplierId FROM Supplier s", Integer.class).getResultList();
        logEvent.fire(new LogEvent("vendor-performance-timer", LogLevel.TRACE, "recomputeAllSuppliers: recomputing " + supplierIds.size() + " supplier(s)"));
        for (Integer supplierId : supplierIds) {
            vendorPerformanceService.recomputeForSupplier(supplierId);
        }
    }
}

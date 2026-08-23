package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @Schedule(dayOfWeek = "Mon", hour = "3", persistent = true)
    void recomputeAllSuppliers() {
        List<Integer> supplierIds = em.createQuery("SELECT s.supplierId FROM Supplier s", Integer.class).getResultList();
        for (Integer supplierId : supplierIds) {
            vendorPerformanceService.recomputeForSupplier(supplierId);
        }
    }
}

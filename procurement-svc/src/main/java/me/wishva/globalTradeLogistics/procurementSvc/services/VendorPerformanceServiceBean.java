package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuditRecordSummary;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.VendorPerformanceResult;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.interceptor.Audited;
import me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IVendorPerformanceService;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.AuditRecord;
import me.wishva.globalTradeLogistics.core.model.Grn;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.SupplierProvidingProduct;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Weekly, per-supplier on-time-delivery scoring. No dedicated table —
 * results are written to {@code audit_records} via {@code @Audited}
 * (type=PROCUREMENT/action=recomputeForSupplier), reusing the existing audit
 * trail per the "don't add a table for a once-a-week report" call in the
 * plan.
 */
@Stateless
@Interceptors({RequiresRoleInterceptor.class, AuditInterceptor.class})
public class VendorPerformanceServiceBean implements IVendorPerformanceService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Audited(resource = "PROCUREMENT", type = "VENDOR_PERFORMANCE")
    public VendorPerformanceResult recomputeForSupplier(Integer supplierId) {
        String key = "supplier-" + supplierId;
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "recomputeForSupplier: starting recompute"));
        List<Grn> grns = em.createQuery(
                        "SELECT g FROM Grn g WHERE g.suppliersSupplierId = :supplierId", Grn.class)
                .setParameter("supplierId", supplierId)
                .getResultList();

        int evaluated = 0;
        int onTime = 0;
        for (Grn grn : grns) {
            PurchaseOrder po = em.find(PurchaseOrder.class, grn.getPurchaseOrdersPoId());
            if (po == null) {
                continue;
            }

            List<SupplierProvidingProduct> offerings = em.createNamedQuery(
                            "SupplierProvidingProduct.findLeadTime", SupplierProvidingProduct.class)
                    .setParameter("productId", grn.getProductsProductId())
                    .setParameter("supplierId", supplierId)
                    .getResultList();
            int leadTimeInDays = offerings.isEmpty() ? 0 : offerings.get(0).getLeadTimeInDays();

            evaluated++;
            if (!grn.getCreatedAt().isAfter(po.getCreatedAt().plus(leadTimeInDays, ChronoUnit.DAYS))) {
                onTime++;
            }
        }

        double onTimeRate = evaluated == 0 ? 100.0 : (onTime * 100.0 / evaluated);
        String summary = evaluated == 0
                ? "No GRNs recorded yet for supplier " + supplierId
                : onTime + "/" + evaluated + " deliveries on time (" + String.format("%.1f", onTimeRate) + "%)";
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "recomputeForSupplier: " + summary));

        Supplier supplier = em.find(Supplier.class, supplierId);
        if (supplier != null) {
            NotificationPublisher.publish(new EmailNotification(
                    EmailType.VENDOR_PERFORMANCE_REPORT, supplier.getEmail(), supplier.getFullName(),
                    Map.of("summary", summary)));
        }
        NotificationPublisher.publish(new EmailNotification(
                EmailType.VENDOR_PERFORMANCE_REPORT, AppConfig.ADMIN_EMAIL, null,
                Map.of("supplierId", String.valueOf(supplierId), "summary", summary)));

        return new VendorPerformanceResult(supplierId, evaluated, onTime, onTimeRate, summary);
    }

    @Override
    @RequiresRole({Role.ADMIN, Role.COORDINATOR})
    public List<AuditRecordSummary> listVendorPerformanceReports() {
        logEvent.fire(new LogEvent("vendor-performance", LogLevel.TRACE, "listVendorPerformanceReports: loading audit records"));
        List<AuditRecord> records = em.createNamedQuery("AuditRecord.findByType", AuditRecord.class)
                .setParameter("type", "VENDOR_PERFORMANCE")
                .getResultList();

        List<AuditRecordSummary> summaries = new ArrayList<>();
        for (AuditRecord record : records) {
            summaries.add(new AuditRecordSummary(
                    record.getId(), record.getCreatedAt(), record.getResource(),
                    record.getAction(), record.getReference(), record.getDetails()));
        }
        logEvent.fire(new LogEvent("vendor-performance", LogLevel.TRACE,
                "listVendorPerformanceReports: returning " + summaries.size() + " record(s)"));
        return summaries;
    }
}

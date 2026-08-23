package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.messaging.AuditPublisher;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.SupplierProvidingProduct;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Declarative daily timer (contrast with {@code InventoryReorderTimerBean}'s
 * programmatic one) — flags any open PO whose supplier's lead time has
 * elapsed. {@code purchase_orders} has no stored deadline; "overdue" is
 * computed on the fly from {@code created_at + lead_time_in_days}, per the
 * plan's "no other schema changes" note.
 */
@Singleton
public class PurchaseOrderOverdueTimerBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Schedule(hour = "2", persistent = true)
    void checkOverduePurchaseOrders() {
        List<PurchaseOrder> open = em.createNamedQuery("PurchaseOrder.findOpen", PurchaseOrder.class).getResultList();
        Instant now = Instant.now();

        for (PurchaseOrder po : open) {
            List<SupplierProvidingProduct> offerings = em.createNamedQuery(
                            "SupplierProvidingProduct.findLeadTime", SupplierProvidingProduct.class)
                    .setParameter("productId", po.getProductsProductId())
                    .setParameter("supplierId", po.getSuppliersSupplierId())
                    .getResultList();
            if (offerings.isEmpty()) {
                continue;
            }

            Instant deadline = po.getCreatedAt().plus(offerings.get(0).getLeadTimeInDays(), ChronoUnit.DAYS);
            if (now.isBefore(deadline)) {
                continue;
            }

            Supplier supplier = em.find(Supplier.class, po.getSuppliersSupplierId());
            Map<String, String> params = Map.of(
                    "poId", String.valueOf(po.getPoId()),
                    "productId", String.valueOf(po.getProductsProductId()),
                    "requestingQty", String.valueOf(po.getRequestingQty()));

            if (supplier != null) {
                NotificationPublisher.publish(new EmailNotification(
                        EmailType.PO_OVERDUE_ALERT, supplier.getEmail(), supplier.getFullName(), params));
            }
            NotificationPublisher.publish(new EmailNotification(
                    EmailType.PO_OVERDUE_ALERT, AppConfig.ADMIN_EMAIL, null, params));

            AuditPublisher.publish(new AuditEvent(
                    "PROCUREMENT", "PO_OVERDUE_ALERT", "system", String.valueOf(po.getPoId()),
                    "PO " + po.getPoId() + " overdue since " + deadline));
        }
    }
}

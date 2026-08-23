package me.wishva.globalTradeLogistics.logisticsSvc.services;

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
import me.wishva.globalTradeLogistics.core.model.CustomClearanceRecord;
import me.wishva.globalTradeLogistics.core.model.Shipment;

import java.util.List;
import java.util.Map;

/**
 * Declarative daily timer flagging customs clearance records still awaiting
 * clearance. {@code custom_clearence_records} has no due-date column (and
 * none is added, per the "no other schema changes" note) — "approaching a
 * deadline" is therefore approximated as "still {@code PENDING}", the same
 * schema-constrained adaptation {@code PurchaseOrderOverdueTimerBean} makes
 * for POs. No stored assignment of which customs agent owns a shipment
 * exists, so the warning goes to {@link AppConfig#ADMIN_EMAIL}, same
 * fallback {@code InventoryReorderTimerBean} uses for {@code REORDER_ALERT}.
 */
@Singleton
public class CustomsDeadlineTimerBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Schedule(hour = "6", persistent = true)
    void checkPendingCustomsClearances() {
        List<CustomClearanceRecord> pending = em.createNamedQuery(
                        "CustomClearanceRecord.findPending", CustomClearanceRecord.class)
                .getResultList();

        for (CustomClearanceRecord record : pending) {
            Shipment shipment = em.find(Shipment.class, record.getSupplierShipmentsShipmentId());

            NotificationPublisher.publish(new EmailNotification(
                    EmailType.CUSTOMS_DEADLINE_WARNING, AppConfig.ADMIN_EMAIL, null,
                    Map.of("recordId", String.valueOf(record.getRecordId()),
                            "shipmentId", String.valueOf(record.getSupplierShipmentsShipmentId()),
                            "trackingNumber", shipment != null ? shipment.getTrackingNumber() : "unknown",
                            "declarationNumber", String.valueOf(record.getDeclarationNumber()))));

            AuditPublisher.publish(new AuditEvent(
                    "LOGISTICS", "CUSTOMS_DEADLINE_WARNING", "system", String.valueOf(record.getRecordId())));
        }
    }
}

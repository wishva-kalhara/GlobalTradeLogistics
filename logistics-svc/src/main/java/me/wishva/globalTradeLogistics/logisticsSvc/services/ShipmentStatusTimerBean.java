package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.messaging.AuditPublisher;
import me.wishva.globalTradeLogistics.core.model.Shipment;

import java.security.SecureRandom;
import java.util.List;

/**
 * Declarative timer, every 15 minutes — simulates polling a carrier system
 * for status changes on every shipment currently {@code IN_TRANSIT}. Timers
 * run with no authenticated principal, so this writes {@code shipments}
 * directly rather than going through {@code IShipmentService.updateStatus}
 * (which requires {@code @RequiresRole(CUSTOMS_AGENT)}) — same reasoning as
 * {@code InventoryReorderTimerBean}/{@code PurchaseOrderOverdueTimerBean}.
 */
@Singleton
public class ShipmentStatusTimerBean {

    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Schedule(minute = "*/15", hour = "*", persistent = true)
    void pollInTransitShipments() {
        List<Shipment> inTransit = em.createNamedQuery("Shipment.findByStatus", Shipment.class)
                .setParameter("status", ShipmentStatus.IN_TRANSIT)
                .getResultList();

        for (Shipment shipment : inTransit) {
            // Simulated external carrier check — no real carrier integration exists,
            // so a coin flip stands in for "has this shipment arrived yet?".
            if (!RANDOM.nextBoolean()) {
                continue;
            }

            shipment.setStatus(ShipmentStatus.DELIVERED);

            AuditPublisher.publish(new AuditEvent(
                    "LOGISTICS", "SHIPMENT_STATUS_POLL", "system", String.valueOf(shipment.getShipmentId()),
                    "Shipment " + shipment.getShipmentId() + " -> DELIVERED (simulated carrier poll)"));
        }
    }
}

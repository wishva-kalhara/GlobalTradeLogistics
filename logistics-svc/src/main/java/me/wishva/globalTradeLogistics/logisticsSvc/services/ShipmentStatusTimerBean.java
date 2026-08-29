package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
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

    @Inject
    private Event<LogEvent> logEvent;

    @Schedule(minute = "*/15", hour = "*", persistent = true)
    void pollInTransitShipments() {
        List<Shipment> inTransit = em.createNamedQuery("Shipment.findByStatus", Shipment.class)
                .setParameter("status", ShipmentStatus.IN_TRANSIT)
                .getResultList();
        logEvent.fire(new LogEvent("shipment-status-timer", LogLevel.TRACE, "pollInTransitShipments: polling " + inTransit.size() + " IN_TRANSIT shipment(s)"));

        for (Shipment shipment : inTransit) {
            String key = "shipment-" + shipment.getShipmentId();
            // Simulated external carrier check — no real carrier integration exists,
            // so a coin flip stands in for "has this shipment arrived yet?".
            if (!RANDOM.nextBoolean()) {
                logEvent.fire(new LogEvent(key, LogLevel.TRACE, "pollInTransitShipments: carrier poll - not yet arrived"));
                continue;
            }

            shipment.setStatus(ShipmentStatus.DELIVERED);
            logEvent.fire(new LogEvent(key, LogLevel.TRACE, "pollInTransitShipments: carrier poll - marked DELIVERED"));

            AuditPublisher.publish(new AuditEvent(
                    "LOGISTICS", "SHIPMENT_STATUS_POLL", "system", String.valueOf(shipment.getShipmentId()),
                    "Shipment " + shipment.getShipmentId() + " -> DELIVERED (simulated carrier poll)"));
        }
    }
}

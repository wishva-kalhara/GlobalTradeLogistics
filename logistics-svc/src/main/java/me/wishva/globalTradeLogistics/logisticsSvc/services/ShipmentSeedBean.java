package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.model.Shipment;

/**
 * Seeds one demo shipment on deploy, idempotently — {@code shipments} is a
 * pre-existing legacy table with no seed data of its own, and without at
 * least one row, none of Phase 4's flows (status update, customs record,
 * carrier notify) have anything to act on. Same pattern as
 * {@code CatalogSeedBean} (order-svc).
 */
@Singleton
@Startup
public class ShipmentSeedBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @PostConstruct
    void seed() {
        long existing = em.createQuery("SELECT COUNT(s) FROM Shipment s", Long.class).getSingleResult();
        if (existing > 0) {
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber("TRK-0001");
        shipment.setVesselId("VESSEL-ALPHA");
        shipment.setType("SEA");
        shipment.setWarehousesWarehouseId(1);
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        em.persist(shipment);
    }
}

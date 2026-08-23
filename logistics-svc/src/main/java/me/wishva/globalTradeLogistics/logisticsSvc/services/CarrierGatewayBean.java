package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.SupplyChainSystemException;
import me.wishva.globalTradeLogistics.core.model.Shipment;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bean-managed transaction (BMT) example, per the assignment's transaction
 * -demarcation learning outcome: {@code @TransactionManagement(BEAN)} is a
 * whole-bean setting (can't mix CMT/BMT methods in one class), so this is a
 * dedicated bean rather than a method on {@code ShipmentServiceBean}
 * (container-managed, like every other bean in this project).
 * <p>
 * The point of BMT here: the (simulated) call to an external carrier system
 * can be slow, and a container-managed transaction would hold a DB
 * connection/locks open for its entire duration. With BMT, the read and the
 * write are each their own short transaction, and the slow external call in
 * between runs with <em>no</em> transaction open at all.
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class CarrierGatewayBean {

    private static final Logger LOG = Logger.getLogger(CarrierGatewayBean.class.getName());

    @Resource
    private UserTransaction userTransaction;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    public String notifyCarrierSystem(Integer shipmentId) throws ShipmentNotFoundException {
        String trackingNumber;
        String vesselId;

        try {
            userTransaction.begin();
            Shipment shipment = em.find(Shipment.class, shipmentId);
            if (shipment == null) {
                userTransaction.rollback();
                throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
            }
            trackingNumber = shipment.getTrackingNumber();
            vesselId = shipment.getVesselId();
            userTransaction.commit();
        } catch (ShipmentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new SupplyChainSystemException("Failed reading shipment " + shipmentId + " before carrier call", e);
        }

        String carrierRef = simulateCarrierCall(trackingNumber, vesselId);

        try {
            userTransaction.begin();
            Shipment shipment = em.find(Shipment.class, shipmentId);
            shipment.setRef(carrierRef);
            userTransaction.commit();
        } catch (Exception e) {
            rollbackQuietly();
            throw new SupplyChainSystemException("Failed recording carrier reference for shipment " + shipmentId, e);
        }

        return carrierRef;
    }

    private String simulateCarrierCall(String trackingNumber, String vesselId) {
        LOG.info(() -> "Calling external carrier system for tracking=" + trackingNumber + " vessel=" + vesselId
                + " (no DB transaction open during this call)");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "CARRIER-" + UUID.randomUUID();
    }

    private void rollbackQuietly() {
        try {
            userTransaction.rollback();
        } catch (Exception ignored) {
            // Best-effort — the original failure is what gets reported.
        }
    }
}

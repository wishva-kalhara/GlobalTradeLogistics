package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.ShipmentSummary;
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

import java.util.List;

@Local
public interface IShipmentService {

    ShipmentSummary getShipment(Integer shipmentId) throws ShipmentNotFoundException;

    /**
     * A supplier creates the shipment for one of their own open purchase
     * orders — the start of the ship → customs → GRN flow. Rejects a PO
     * that's already completed or that already has a shipment
     * ({@link InvalidShipmentStateException}), or one that doesn't belong to
     * the calling supplier ({@link PurchaseOrderNotFoundException}, same
     * indistinguishable-404 convention as {@code OrderNotFoundException}).
     */
    ShipmentSummary createShipmentForPurchaseOrder(Integer poId, String trackingNumber, String vesselId, String type)
            throws PurchaseOrderNotFoundException, InvalidShipmentStateException, UnauthorizedAccessException, UnknownPrincipalException;

    /**
     * Shipments a warehouse manager can record a GRN against right now:
     * status {@code DELIVERED}, linked to a PO, and that PO isn't already
     * completed.
     */
    List<ShipmentSummary> listDeliveredAwaitingGrn() throws UnauthorizedAccessException;

    /**
     * {@code idempotencyKey} must be the last parameter — see
     * {@code IdempotencyChecked}'s convention. Rejects
     * {@link ShipmentStatus#COMPLETED} ({@link InvalidShipmentStateException}) —
     * that status is set only by a successful GRN recording, never directly.
     */
    ShipmentSummary updateStatus(Integer shipmentId, ShipmentStatus newStatus, String idempotencyKey)
            throws ShipmentNotFoundException, InvalidShipmentStateException, UnauthorizedAccessException;

    void createCustomsRecord(Integer shipmentId, String declarationNumber)
            throws ShipmentNotFoundException, UnauthorizedAccessException;

    /**
     * Updates the status of a shipment's most recent customs clearance
     * record — "handling customs" for the ship → customs → GRN flow. A
     * warehouse manager can't record a GRN until this reaches
     * {@link CustomsClearanceStatus#CLEARED}.
     */
    ShipmentSummary updateCustomsStatus(Integer shipmentId, CustomsClearanceStatus status)
            throws ShipmentNotFoundException, InvalidShipmentStateException, UnauthorizedAccessException;

    /**
     * Simulates notifying an external carrier system — delegates to a
     * separate bean-managed-transaction bean, since the (simulated) slow
     * external call must not hold a container transaction open.
     */
    ShipmentSummary notifyCarrierSystem(Integer shipmentId) throws ShipmentNotFoundException;

    /** All shipments, most recent first — visibility for staff who need to track shipments in progress. */
    List<ShipmentSummary> listAll() throws UnauthorizedAccessException;

    /** Shipments created for any of the calling supplier's purchase orders, most recent first. */
    List<ShipmentSummary> listForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException;
}

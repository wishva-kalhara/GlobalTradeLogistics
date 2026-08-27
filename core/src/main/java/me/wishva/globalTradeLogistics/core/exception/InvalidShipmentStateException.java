package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown when a shipment/purchase-order action is attempted out of order in
 * the create-shipment → customs → GRN flow — e.g. a supplier tries to ship
 * an already-completed PO or one that already has a shipment, or a warehouse
 * manager tries to record a GRN for a shipment that isn't yet DELIVERED or
 * whose PO was already completed. Maps to HTTP 409 at the gateway.
 */
public class InvalidShipmentStateException extends SupplyChainApplicationException {

    public InvalidShipmentStateException(String message) {
        super(message);
    }
}

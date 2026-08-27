package me.wishva.globalTradeLogistics.core.enums;

public enum ShipmentStatus {
    CREATED,
    IN_TRANSIT,
    DELIVERED,
    DELAYED,
    /** Set only by {@code recordGrnForShipment} when its GRN is recorded — never settable directly via {@code PUT /shipments/{id}/status}. */
    COMPLETED
}

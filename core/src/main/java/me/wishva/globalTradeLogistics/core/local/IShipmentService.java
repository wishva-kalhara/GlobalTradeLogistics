package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.ShipmentSummary;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

@Local
public interface IShipmentService {

    ShipmentSummary getShipment(Integer shipmentId) throws ShipmentNotFoundException;

    /**
     * {@code idempotencyKey} must be the last parameter — see
     * {@code IdempotencyChecked}'s convention.
     */
    ShipmentSummary updateStatus(Integer shipmentId, ShipmentStatus newStatus, String idempotencyKey)
            throws ShipmentNotFoundException, UnauthorizedAccessException;

    void createCustomsRecord(Integer shipmentId, String declarationNumber)
            throws ShipmentNotFoundException, UnauthorizedAccessException;

    /**
     * Simulates notifying an external carrier system — delegates to a
     * separate bean-managed-transaction bean, since the (simulated) slow
     * external call must not hold a container transaction open.
     */
    ShipmentSummary notifyCarrierSystem(Integer shipmentId) throws ShipmentNotFoundException;
}

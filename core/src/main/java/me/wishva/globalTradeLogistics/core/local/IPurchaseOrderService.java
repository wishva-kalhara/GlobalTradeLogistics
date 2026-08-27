package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

import java.util.List;

/**
 * Purchase order lifecycle (create → ship → customs → GRN → complete) plus a
 * supplier's self-service product-offering catalog. {@code listForSupplier}/
 * {@code listShippableForSupplier}/{@code addProductOffering} always resolve
 * the caller's own {@code supplier_id} from {@code CurrentPrincipalHolder} —
 * never a client-supplied id — same self-scoping pattern as
 * {@link IProfileService}.
 */
@Local
public interface IPurchaseOrderService {

    PurchaseOrderSummary createPo(Integer supplierId, Integer productId, Integer qty)
            throws UnauthorizedAccessException;

    /**
     * Records a GRN against the purchase order a (now {@code DELIVERED})
     * shipment was created for — the last step of the ship → customs → GRN
     * flow. Rejects a shipment that isn't linked to a PO, isn't yet
     * {@code DELIVERED}, or whose PO was already completed
     * ({@link InvalidShipmentStateException}).
     */
    PurchaseOrderSummary recordGrnForShipment(Integer shipmentId, Integer qty)
            throws ShipmentNotFoundException, PurchaseOrderNotFoundException, InvalidShipmentStateException, UnauthorizedAccessException;

    List<PurchaseOrderSummary> listForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException;

    /** The calling supplier's open POs that don't have a shipment yet — to populate the create-shipment dropdown. */
    List<PurchaseOrderSummary> listShippableForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException;

    void addProductOffering(Integer productId, Integer warehouseId, Integer leadTimeInDays)
            throws UnauthorizedAccessException, UnknownPrincipalException;
}

package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

import java.util.List;

/**
 * Purchase order lifecycle (create → GRN → complete) plus a supplier's
 * self-service product-offering catalog. {@code listForSupplier}/
 * {@code addProductOffering} always resolve the caller's own
 * {@code supplier_id} from {@code CurrentPrincipalHolder} — never a
 * client-supplied id — same self-scoping pattern as {@link IProfileService}.
 */
@Local
public interface IPurchaseOrderService {

    PurchaseOrderSummary createPo(Integer supplierId, Integer productId, Integer qty)
            throws UnauthorizedAccessException;

    PurchaseOrderSummary recordGrn(Integer poId, Integer qty)
            throws PurchaseOrderNotFoundException, UnauthorizedAccessException;

    List<PurchaseOrderSummary> listForSupplier() throws UnauthorizedAccessException, UnknownPrincipalException;

    void addProductOffering(Integer productId, Integer warehouseId, Integer leadTimeInDays)
            throws UnauthorizedAccessException, UnknownPrincipalException;
}

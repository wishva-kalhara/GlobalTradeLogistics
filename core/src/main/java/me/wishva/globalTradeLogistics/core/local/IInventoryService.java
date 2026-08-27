package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.InventorySummary;
import me.wishva.globalTradeLogistics.core.dto.WarehouseSummary;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

import java.util.List;

/**
 * Stock check/adjust, called by {@code order-svc} and {@code procurement-svc}.
 * Single-warehouse simplification: {@code checkStock}/{@code decrementStock}/
 * {@code incrementStock} all resolve the target {@code inventory} row by
 * product only (highest-stock row for that product), same as
 * {@code OrderServiceBean}'s inline logic before this module existed — this
 * project seeds exactly one warehouse ({@code CatalogSeedBean}), and neither
 * {@code orders} nor {@code purchase_orders} carries a warehouse id to
 * disambiguate further. {@code listByWarehouse} is the one method that
 * genuinely is warehouse-scoped (7.5's read-only stock view).
 */
@Local
public interface IInventoryService {

    boolean checkStock(Integer productId, Integer qty);

    void decrementStock(Integer productId, Integer qty) throws InsufficientInventoryException;

    void incrementStock(Integer productId, Integer qty);

    List<InventorySummary> listByWarehouse(Integer warehouseId) throws UnauthorizedAccessException;

    /**
     * Warehouses to populate a warehouse-picking dropdown — the staff
     * inventory console's ({@link #listByWarehouse}'s roles) plus
     * {@code VENDOR_REP}, who picks a delivery warehouse when registering a
     * product offering.
     */
    List<WarehouseSummary> listWarehouses() throws UnauthorizedAccessException;
}

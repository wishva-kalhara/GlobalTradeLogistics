package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;

/**
 * Stock check/adjust, called by {@code order-svc} and {@code procurement-svc}.
 * Single-warehouse simplification: every method resolves the target
 * {@code inventory} row by product only (highest-stock row for that
 * product), same as {@code OrderServiceBean}'s inline logic before this
 * module existed — this project seeds exactly one warehouse
 * ({@code CatalogSeedBean}), and neither {@code orders} nor
 * {@code purchase_orders} carries a warehouse id to disambiguate further.
 */
@Local
public interface IInventoryService {

    boolean checkStock(Integer productId, Integer qty);

    void decrementStock(Integer productId, Integer qty) throws InsufficientInventoryException;

    void incrementStock(Integer productId, Integer qty);
}

package me.wishva.globalTradeLogistics.inventorySvc.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.local.IInventoryService;
import me.wishva.globalTradeLogistics.core.model.Inventory;

import java.time.Instant;
import java.util.List;

/**
 * See {@link IInventoryService}'s javadoc for the single-warehouse
 * simplification every method here relies on.
 */
@Stateless
public class InventoryServiceBean implements IInventoryService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Override
    public boolean checkStock(Integer productId, Integer qty) {
        Inventory inventory = resolve(productId);
        return inventory != null && inventory.getQty() >= qty;
    }

    @Override
    public void decrementStock(Integer productId, Integer qty) throws InsufficientInventoryException {
        Inventory inventory = resolve(productId);
        if (inventory == null) {
            throw new InsufficientInventoryException("No inventory found for product " + productId);
        }
        if (inventory.getQty() < qty) {
            throw new InsufficientInventoryException(
                    "Insufficient stock for product " + productId + ": requested " + qty + ", available " + inventory.getQty());
        }
        inventory.setQty(inventory.getQty() - qty);
        inventory.setLastUpdatedAt(Instant.now());
    }

    @Override
    public void incrementStock(Integer productId, Integer qty) {
        Inventory inventory = resolve(productId);
        if (inventory == null) {
            throw new IllegalStateException("No inventory row found for product " + productId + " to receive stock into");
        }
        inventory.setQty(inventory.getQty() + qty);
        inventory.setLastUpdatedAt(Instant.now());
    }

    private Inventory resolve(Integer productId) {
        List<Inventory> stock = em.createNamedQuery("Inventory.findByProductOrderByQtyDesc", Inventory.class)
                .setParameter("productId", productId)
                .setMaxResults(1)
                .getResultList();
        return stock.isEmpty() ? null : stock.get(0);
    }
}

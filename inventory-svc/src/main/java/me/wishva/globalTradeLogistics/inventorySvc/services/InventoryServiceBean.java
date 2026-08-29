package me.wishva.globalTradeLogistics.inventorySvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.InventorySummary;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.WarehouseSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IInventoryService;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Product;
import me.wishva.globalTradeLogistics.core.model.Warehouse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * See {@link IInventoryService}'s javadoc for the single-warehouse
 * simplification most methods here rely on. {@code checkStock}/
 * {@code decrementStock}/{@code incrementStock} are called internally by
 * other EJBs (never exposed directly over REST), so they're unguarded here;
 * {@code listByWarehouse} is the one method a JAX-RS resource calls
 * directly, hence the interceptor and {@code @RequiresRole}.
 */
@Stateless
@Interceptors(RequiresRoleInterceptor.class)
public class InventoryServiceBean implements IInventoryService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public boolean checkStock(Integer productId, Integer qty) {
        Inventory inventory = resolve(productId);
        return inventory != null && inventory.getQty() >= qty;
    }

    @Override
    public void decrementStock(Integer productId, Integer qty) throws InsufficientInventoryException {
        String key = "product-" + productId;
        Inventory inventory = resolve(productId);
        if (inventory == null) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "decrementStock: no inventory row for product " + productId));
            throw new InsufficientInventoryException("No inventory found for product " + productId);
        }
        if (inventory.getQty() < qty) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN,
                    "decrementStock: insufficient stock for product " + productId + " - requested " + qty + ", available " + inventory.getQty()));
            throw new InsufficientInventoryException(
                    "Insufficient stock for product " + productId + ": requested " + qty + ", available " + inventory.getQty());
        }
        inventory.setQty(inventory.getQty() - qty);
        inventory.setLastUpdatedAt(Instant.now());
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "decrementStock: product " + productId + " decremented by " + qty + ", " + inventory.getQty() + " remaining"));
    }

    @Override
    public void incrementStock(Integer productId, Integer qty) {
        String key = "product-" + productId;
        Inventory inventory = resolve(productId);
        if (inventory == null) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "incrementStock: no inventory row for product " + productId + " to receive stock into"));
            throw new IllegalStateException("No inventory row found for product " + productId + " to receive stock into");
        }
        inventory.setQty(inventory.getQty() + qty);
        inventory.setLastUpdatedAt(Instant.now());
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "incrementStock: product " + productId + " incremented by " + qty + ", now " + inventory.getQty()));
    }

    @Override
    @RequiresRole({Role.WAREHOUSE_MANAGER, Role.COORDINATOR, Role.ADMIN})
    public List<InventorySummary> listByWarehouse(Integer warehouseId) {
        logEvent.fire(new LogEvent("warehouse-" + warehouseId, LogLevel.TRACE, "listByWarehouse: loading stock"));
        List<Inventory> stock = em.createNamedQuery("Inventory.findByWarehouse", Inventory.class)
                .setParameter("warehouseId", warehouseId)
                .getResultList();

        List<InventorySummary> summaries = new ArrayList<>();
        for (Inventory inventory : stock) {
            Product product = em.find(Product.class, inventory.getProductsProductId());
            summaries.add(new InventorySummary(
                    inventory.getInventoryId(),
                    inventory.getWarehousesWarehouseId(),
                    inventory.getProductsProductId(),
                    product != null ? product.getName() : null,
                    inventory.getQty(),
                    inventory.getReorderLevel(),
                    inventory.getUnitPrice(),
                    inventory.getLastUpdatedAt()));
        }
        logEvent.fire(new LogEvent("warehouse-" + warehouseId, LogLevel.TRACE,
                "listByWarehouse: returning " + summaries.size() + " inventory row(s)"));
        return summaries;
    }

    @Override
    @RequiresRole({Role.WAREHOUSE_MANAGER, Role.COORDINATOR, Role.ADMIN, Role.VENDOR_REP})
    public List<WarehouseSummary> listWarehouses() {
        logEvent.fire(new LogEvent("warehouses-list", LogLevel.TRACE, "listWarehouses: loading warehouses"));
        List<Warehouse> warehouses = em.createQuery("SELECT w FROM Warehouse w ORDER BY w.warehouseId", Warehouse.class)
                .getResultList();

        List<WarehouseSummary> summaries = new ArrayList<>();
        for (Warehouse warehouse : warehouses) {
            summaries.add(new WarehouseSummary(warehouse.getWarehouseId(), warehouse.getCountry()));
        }
        logEvent.fire(new LogEvent("warehouses-list", LogLevel.TRACE, "listWarehouses: returning " + summaries.size() + " warehouse(s)"));
        return summaries;
    }

    private Inventory resolve(Integer productId) {
        List<Inventory> stock = em.createNamedQuery("Inventory.findByProductOrderByQtyDesc", Inventory.class)
                .setParameter("productId", productId)
                .setMaxResults(1)
                .getResultList();
        return stock.isEmpty() ? null : stock.get(0);
    }
}

package me.wishva.globalTradeLogistics.orderSvc.services;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Product;
import me.wishva.globalTradeLogistics.core.model.Warehouse;

/**
 * Seeds a demo product catalog (one warehouse + a handful of products with
 * stock) on deploy, idempotently — {@code products}/{@code wearhouses}/
 * {@code inventory} are pre-existing legacy tables with no seed data of
 * their own, and without at least one in-stock product the "place an order"
 * flow has nothing to order. Same idempotent-seed pattern as
 * {@code CountrySeedBean} (iam-svc).
 */
@Singleton
@Startup
public class CatalogSeedBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    private static final Object[][] PRODUCTS = {
            {"Steel Pipe (3m)", "Galvanized steel pipe, 3 metre length", "steel-pipe.png", 500, 12.50},
            {"Industrial Bearing", "High-load industrial ball bearing", "bearing.png", 300, 8.25},
            {"Hydraulic Hose (5m)", "Reinforced hydraulic hose, 5 metre length", "hydraulic-hose.png", 150, 34.90},
            {"Circuit Breaker 32A", "32A single-pole circuit breaker", "circuit-breaker.png", 200, 19.75},
            {"Pallet Wrap Roll", "Industrial stretch wrap roll, 500mm", "pallet-wrap.png", 800, 6.40},
    };

    private static final int REORDER_LEVEL = 20;

    @PostConstruct
    void seed() {
        long existingProducts = em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
        if (existingProducts > 0) {
            return;
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(1);
        warehouse.setCountry("US");
        em.persist(warehouse);

        int inventoryId = 1;
        for (Object[] row : PRODUCTS) {
            Product product = new Product();
            product.setName((String) row[0]);
            product.setDescription((String) row[1]);
            product.setProductImage((String) row[2]);
            em.persist(product);
            em.flush();

            Inventory inventory = new Inventory();
            inventory.setInventoryId(inventoryId++);
            inventory.setWarehousesWarehouseId(warehouse.getWarehouseId());
            inventory.setProductsProductId(product.getProductId());
            inventory.setQty((Integer) row[3]);
            inventory.setReorderLevel(REORDER_LEVEL);
            inventory.setUnitPrice((Double) row[4]);
            em.persist(inventory);
        }
    }
}

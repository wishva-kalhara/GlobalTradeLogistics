package me.wishva.globalTradeLogistics.inventorySvc.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.messaging.AuditPublisher;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Product;

import java.util.List;
import java.util.Map;

/**
 * Programmatic timer (injected {@link TimerService}, interval timer created
 * in {@code @PostConstruct}) — the required contrast with the declarative
 * {@code @Schedule} timers elsewhere (procurement-svc, logistics-svc).
 * Every 15 minutes, flags any product whose stock has dropped below its
 * reorder level.
 */
@Singleton
@Startup
public class InventoryReorderTimerBean {

    private static final long INTERVAL_MS = 15 * 60 * 1000L;

    @Resource
    private TimerService timerService;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @PostConstruct
    void scheduleTimer() {
        timerService.createIntervalTimer(INTERVAL_MS, INTERVAL_MS, new TimerConfig("inventory-reorder-check", false));
    }

    @Timeout
    void onTimeout(Timer timer) {
        List<Inventory> lowStock = em.createQuery(
                        "SELECT i FROM Inventory i WHERE i.qty < i.reorderLevel", Inventory.class)
                .getResultList();
        logEvent.fire(new LogEvent("inventory-reorder-timer", LogLevel.TRACE, "onTimeout: " + lowStock.size() + " product(s) below reorder level"));

        for (Inventory inventory : lowStock) {
            Product product = em.find(Product.class, inventory.getProductsProductId());
            String productName = product != null ? product.getName() : ("product " + inventory.getProductsProductId());
            logEvent.fire(new LogEvent("product-" + inventory.getProductsProductId(), LogLevel.TRACE,
                    "onTimeout: " + productName + " at " + inventory.getQty() + "/" + inventory.getReorderLevel()));

            NotificationPublisher.publish(new EmailNotification(
                    EmailType.REORDER_ALERT, AppConfig.ADMIN_EMAIL, null,
                    Map.of("productId", String.valueOf(inventory.getProductsProductId()),
                            "productName", productName,
                            "qty", String.valueOf(inventory.getQty()),
                            "reorderLevel", String.valueOf(inventory.getReorderLevel()))));

            AuditPublisher.publish(new AuditEvent(
                    "INVENTORY", "REORDER_ALERT", "system",
                    String.valueOf(inventory.getInventoryId()),
                    productName + " at " + inventory.getQty() + "/" + inventory.getReorderLevel()));
        }
    }
}

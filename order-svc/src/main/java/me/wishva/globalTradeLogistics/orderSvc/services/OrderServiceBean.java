package me.wishva.globalTradeLogistics.orderSvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.OrderItemRequest;
import me.wishva.globalTradeLogistics.core.dto.OrderLineSummary;
import me.wishva.globalTradeLogistics.core.dto.OrderSummary;
import me.wishva.globalTradeLogistics.core.dto.ProductSalesSummary;
import me.wishva.globalTradeLogistics.core.dto.SalesSummary;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.OrderStatus;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.exception.OrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.interceptor.Audited;
import me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IOrderService;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Order;
import me.wishva.globalTradeLogistics.core.model.OrderItem;
import me.wishva.globalTradeLogistics.core.model.Product;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Place/view orders for the currently authenticated customer. Stock
 * checking and decrementing happens in the same transaction as the order
 * write (2.2-2.5 of the plan) — {@code inventory-svc} doesn't exist yet
 * (Phase 3), so this bean reads/writes {@code inventory} directly for now;
 * once inventory-svc lands, that logic moves behind {@code IInventoryService}
 * without changing this class's public contract.
 * <p>
 * {@code @Audited} is applied at class level — the assignment's class-level
 * interceptor example — so every method here publishes an audit event on
 * success, not just {@code placeOrder}.
 */
@Stateless
@Interceptors({RequiresRoleInterceptor.class, AuditInterceptor.class})
@Audited(resource = "ORDER")
public class OrderServiceBean implements IOrderService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    @RequiresRole(Role.CUSTOMER)
    public OrderSummary placeOrder(List<OrderItemRequest> items) throws InsufficientInventoryException, UnknownPrincipalException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required");
        }

        Customer customer = resolveCustomer();
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "placeOrder: resolved customer, " + items.size() + " line item(s) requested"));

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;
        for (OrderItemRequest item : items) {
            List<Inventory> stock = em.createNamedQuery("Inventory.findByProductOrderByQtyDesc", Inventory.class)
                    .setParameter("productId", item.getProductId())
                    .setMaxResults(1)
                    .getResultList();
            if (stock.isEmpty()) {
                logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.WARN, "placeOrder: no inventory row for product " + item.getProductId()));
                throw new InsufficientInventoryException("No inventory found for product " + item.getProductId());
            }

            Inventory inventory = stock.get(0);
            if (inventory.getQty() < item.getQty()) {
                logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.WARN,
                        "placeOrder: insufficient stock for product " + item.getProductId()
                                + " - requested " + item.getQty() + ", available " + inventory.getQty()));
                throw new InsufficientInventoryException(
                        "Insufficient stock for product " + item.getProductId()
                                + ": requested " + item.getQty() + ", available " + inventory.getQty());
            }

            inventory.setQty(inventory.getQty() - item.getQty());
            inventory.setLastUpdatedAt(Instant.now());
            logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE,
                    "placeOrder: decremented product " + item.getProductId() + " by " + item.getQty() + ", " + inventory.getQty() + " remaining"));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductsProductId(item.getProductId());
            orderItem.setQty(item.getQty());
            orderItem.setUnitPrice(inventory.getUnitPrice());
            orderItems.add(orderItem);
            total += inventory.getUnitPrice() * item.getQty();
        }

        Order order = new Order();
        order.setOrderedAt(Instant.now());
        order.setTotalPrice(total);
        order.setCustomersCustomerId(customer.getUserId());
        order.setStatus(OrderStatus.PLACED);
        em.persist(order);
        em.flush();
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "placeOrder: order " + order.getOrderId() + " persisted, total=" + total));

        List<OrderLineSummary> lines = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrdersOrderId(order.getOrderId());
            em.persist(orderItem);
            lines.add(toLineSummary(orderItem));
        }

        NotificationPublisher.publish(new EmailNotification(
                EmailType.ORDER_CONFIRMATION, customer.getEmail(), customer.getFullName(),
                Map.of("orderId", String.valueOf(order.getOrderId()), "total", String.valueOf(total))));
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "placeOrder: ORDER_CONFIRMATION notification published for order " + order.getOrderId()));

        return new OrderSummary(order.getOrderId(), order.getOrderedAt(), order.getTotalPrice(), order.getStatus(), lines);
    }

    @Override
    @RequiresRole(Role.CUSTOMER)
    public OrderSummary getOrder(Integer orderId) throws OrderNotFoundException, UnknownPrincipalException {
        Customer customer = resolveCustomer();
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "getOrder: loading order " + orderId));
        Order order = em.find(Order.class, orderId);
        if (order == null || !order.getCustomersCustomerId().equals(customer.getUserId())) {
            logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.WARN, "getOrder: order " + orderId + " not found"));
            throw new OrderNotFoundException("No order found with id " + orderId);
        }
        return toSummary(order);
    }

    @Override
    @RequiresRole(Role.CUSTOMER)
    public List<OrderSummary> listOrdersForCurrentCustomer() throws UnknownPrincipalException {
        Customer customer = resolveCustomer();
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "listOrdersForCurrentCustomer: loading orders"));
        List<Order> orders = em.createNamedQuery("Order.findByCustomer", Order.class)
                .setParameter("customerId", customer.getUserId())
                .getResultList();

        List<OrderSummary> summaries = new ArrayList<>();
        for (Order order : orders) {
            summaries.add(toSummary(order));
        }
        logEvent.fire(new LogEvent(customer.getEmail(), LogLevel.TRACE, "listOrdersForCurrentCustomer: returning " + summaries.size() + " order(s)"));
        return summaries;
    }

    @Override
    @RequiresRole({Role.ADMIN, Role.COORDINATOR})
    public SalesSummary getSalesSummary() {
        logEvent.fire(new LogEvent("sales-summary", LogLevel.TRACE, "getSalesSummary: aggregating order data"));
        List<Order> orders = em.createQuery("SELECT o FROM Order o", Order.class).getResultList();

        double totalSales = 0.0;
        Map<String, Integer> ordersByStatus = new LinkedHashMap<>();
        for (Order order : orders) {
            totalSales += order.getTotalPrice();
            ordersByStatus.merge(order.getStatus().name(), 1, Integer::sum);
        }

        List<OrderItem> items = em.createQuery("SELECT oi FROM OrderItem oi", OrderItem.class).getResultList();
        Map<Integer, double[]> perProduct = new LinkedHashMap<>();
        for (OrderItem item : items) {
            double[] agg = perProduct.computeIfAbsent(item.getProductsProductId(), key -> new double[2]);
            agg[0] += item.getQty();
            agg[1] += item.getQty() * item.getUnitPrice();
        }

        List<ProductSalesSummary> topProducts = new ArrayList<>();
        for (Map.Entry<Integer, double[]> entry : perProduct.entrySet()) {
            Product product = em.find(Product.class, entry.getKey());
            topProducts.add(new ProductSalesSummary(
                    entry.getKey(), product != null ? product.getName() : null,
                    (int) entry.getValue()[0], entry.getValue()[1]));
        }
        topProducts.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        if (topProducts.size() > 5) {
            topProducts = topProducts.subList(0, 5);
        }

        logEvent.fire(new LogEvent("sales-summary", LogLevel.TRACE,
                "getSalesSummary: totalSales=" + totalSales + ", orderCount=" + orders.size()));
        return new SalesSummary(totalSales, orders.size(), ordersByStatus, topProducts);
    }

    private OrderSummary toSummary(Order order) {
        List<OrderItem> items = em.createNamedQuery("OrderItem.findByOrder", OrderItem.class)
                .setParameter("orderId", order.getOrderId())
                .getResultList();

        List<OrderLineSummary> lines = new ArrayList<>();
        for (OrderItem item : items) {
            lines.add(toLineSummary(item));
        }
        return new OrderSummary(order.getOrderId(), order.getOrderedAt(), order.getTotalPrice(), order.getStatus(), lines);
    }

    private OrderLineSummary toLineSummary(OrderItem item) {
        Product product = em.find(Product.class, item.getProductsProductId());
        return new OrderLineSummary(
                item.getProductsProductId(), product != null ? product.getName() : null, item.getQty(), item.getUnitPrice());
    }

    private Customer resolveCustomer() throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        List<Customer> matches = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "resolveCustomer: no active customer found"));
            throw new UnknownPrincipalException("No active customer found for " + email);
        }
        return matches.get(0);
    }
}

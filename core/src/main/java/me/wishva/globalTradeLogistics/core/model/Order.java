package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.OrderStatus;

import java.time.Instant;

/**
 * Maps the existing {@code orders} table (schema.postgres.sql), plus a new
 * {@code status} column that table doesn't have yet — added here as a plain
 * entity field; {@code hibernate.hbm2ddl.auto=update} adds the missing
 * column on next deploy, no hand-written migration SQL (same approach used
 * for {@code users}/{@code otp_codes}/{@code countries}).
 */
@Entity
@Table(name = "orders")
@NamedQueries({
        @NamedQuery(
                name = "Order.findByCustomer",
                query = "SELECT o FROM Order o WHERE o.customersCustomerId = :customerId ORDER BY o.orderedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt = Instant.now();

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "customers_customer_id", nullable = false)
    private Integer customersCustomerId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PLACED;
}

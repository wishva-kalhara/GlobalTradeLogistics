package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code order_items} table (schema.postgres.sql).
 */
@Entity
@Table(name = "order_items")
@NamedQueries({
        @NamedQuery(
                name = "OrderItem.findByOrder",
                query = "SELECT oi FROM OrderItem oi WHERE oi.ordersOrderId = :orderId")
})
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "products_product_id", nullable = false)
    private Integer productsProductId;

    @Column(name = "orders_order_id", nullable = false)
    private Integer ordersOrderId;
}

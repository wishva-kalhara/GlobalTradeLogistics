package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Maps the existing {@code inventory} table (schema.postgres.sql). PK is a
 * plain {@code INT}, not {@code SERIAL} — see {@link Warehouse}'s note.
 */
@Entity
@Table(name = "inventory")
@NamedQueries({
        @NamedQuery(
                name = "Inventory.findByProductOrderByQtyDesc",
                query = "SELECT i FROM Inventory i WHERE i.productsProductId = :productId ORDER BY i.qty DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @Id
    @Column(name = "inventory_id")
    private Integer inventoryId;

    @Column(name = "wearhouses_wearhous_id", nullable = false)
    private Integer warehousesWarehouseId;

    @Column(name = "products_product_id", nullable = false)
    private Integer productsProductId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
}

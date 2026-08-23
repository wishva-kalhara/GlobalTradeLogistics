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

import java.time.Instant;

/**
 * Maps the existing {@code purchase_orders} table (schema.postgres.sql).
 * {@code isCompleted} stays the legacy {@code SMALLINT} (0/1), not a
 * boolean/enum — "don't redesign the existing schema".
 * <p>
 * No {@code warehouse_id} column exists here (and none is added, per the
 * "no other schema changes" note in the plan) — this project has exactly
 * one seeded warehouse ({@code CatalogSeedBean}), so every stock mutation
 * ({@link me.wishva.globalTradeLogistics.core.local.IInventoryService})
 * resolves the target {@code inventory} row by product only, same
 * single-warehouse simplification {@code OrderServiceBean} already uses.
 */
@Entity
@Table(name = "purchase_orders")
@NamedQueries({
        @NamedQuery(
                name = "PurchaseOrder.findBySupplier",
                query = "SELECT po FROM PurchaseOrder po WHERE po.suppliersSupplierId = :supplierId ORDER BY po.createdAt DESC"),
        @NamedQuery(
                name = "PurchaseOrder.findOpen",
                query = "SELECT po FROM PurchaseOrder po WHERE po.isCompleted = 0")
})
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Integer poId;

    @Column(name = "suppliers_supplier_id", nullable = false)
    private Integer suppliersSupplierId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "is_completed", nullable = false)
    private Integer isCompleted = 0;

    @Column(name = "products_product_id", nullable = false)
    private Integer productsProductId;

    @Column(name = "requesting_qty", nullable = false)
    private Integer requestingQty;
}

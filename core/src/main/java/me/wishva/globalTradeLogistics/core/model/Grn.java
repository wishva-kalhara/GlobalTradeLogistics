package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Maps the existing {@code grns} (goods-received-note) table
 * (schema.postgres.sql). One row per {@code recordGrn} call — a PO can, in
 * principle, be received in more than one delivery, though this project's
 * {@code recordGrn} always marks the PO complete on the first GRN (see
 * {@code PurchaseOrderServiceBean}).
 */
@Entity
@Table(name = "grns")
@Getter
@Setter
@NoArgsConstructor
public class Grn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_id")
    private Integer grnId;

    @Column(name = "suppliers_supplier_id", nullable = false)
    private Integer suppliersSupplierId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "purchase_orders_po_id", nullable = false)
    private Integer purchaseOrdersPoId;

    @Column(name = "products_product_id", nullable = false)
    private Integer productsProductId;

    @Column(name = "qty", nullable = false)
    private Integer qty;
}

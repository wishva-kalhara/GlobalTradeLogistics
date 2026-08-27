package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code supplier_providing_products} table
 * (schema.postgres.sql) — a supplier's product-offering catalog: which
 * products a supplier can provide, from which warehouse, and their lead
 * time. Composite PK, so {@link SupplierProvidingProductId} is the
 * {@code @IdClass}.
 */
@Entity
@Table(name = "supplier_providing_products")
@IdClass(SupplierProvidingProductId.class)
@NamedQueries({
        @NamedQuery(
                name = "SupplierProvidingProduct.findLeadTime",
                query = "SELECT s FROM SupplierProvidingProduct s "
                        + "WHERE s.productsProductId = :productId AND s.suppliersSupplierId = :supplierId"),
        @NamedQuery(
                name = "SupplierProvidingProduct.findSupplierIdsByProduct",
                query = "SELECT DISTINCT s.suppliersSupplierId FROM SupplierProvidingProduct s WHERE s.productsProductId = :productId")
})
@Getter
@Setter
@NoArgsConstructor
public class SupplierProvidingProduct {

    @Id
    @Column(name = "products_product_id")
    private Integer productsProductId;

    @Id
    @Column(name = "suppliers_supplier_id")
    private Integer suppliersSupplierId;

    @Id
    @Column(name = "wearhouses_wearhous_id")
    private Integer warehousesWarehouseId;

    @Column(name = "lead_time_in_days", nullable = false)
    private Integer leadTimeInDays;
}

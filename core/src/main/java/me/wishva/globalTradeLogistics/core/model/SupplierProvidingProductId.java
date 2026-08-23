package me.wishva.globalTradeLogistics.core.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@code @IdClass} for {@link SupplierProvidingProduct} — the existing
 * {@code supplier_providing_products} table's composite PK is
 * {@code (products_product_id, suppliers_supplier_id, wearhouses_wearhous_id)}.
 * Not an entity itself, so Lombok's generated {@code equals}/{@code hashCode}
 * here don't carry the lazy-proxy pitfall {@link User}'s javadoc warns about.
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProvidingProductId implements Serializable {

    private Integer productsProductId;
    private Integer suppliersSupplierId;
    private Integer warehousesWarehouseId;
}

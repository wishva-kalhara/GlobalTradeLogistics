package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.ProductSummary;

import java.util.List;

/**
 * Read-only product catalog for the customer-facing "browse products, then
 * place an order" flow. Deliberately unguarded — any authenticated (or even
 * anonymous) visitor can browse the catalog; only placing an order requires
 * the {@code CUSTOMER} role.
 */
@Local
public interface IProductService {

    List<ProductSummary> listProducts();
}

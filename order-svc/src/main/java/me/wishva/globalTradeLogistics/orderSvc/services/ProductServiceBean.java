package me.wishva.globalTradeLogistics.orderSvc.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.ProductSummary;
import me.wishva.globalTradeLogistics.core.local.IProductService;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Stateless
public class ProductServiceBean implements IProductService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Override
    public List<ProductSummary> listProducts() {
        List<Product> products = em.createNamedQuery("Product.findAll", Product.class).getResultList();
        List<ProductSummary> summaries = new ArrayList<>();
        for (Product product : products) {
            List<Inventory> stock = em.createNamedQuery("Inventory.findByProductOrderByQtyDesc", Inventory.class)
                    .setParameter("productId", product.getProductId())
                    .getResultList();
            Optional<Inventory> best = stock.stream().max(Comparator.comparing(Inventory::getQty));
            summaries.add(new ProductSummary(
                    product.getProductId(),
                    product.getName(),
                    product.getDescription(),
                    product.getProductImage(),
                    best.map(Inventory::getQty).orElse(0),
                    best.map(Inventory::getUnitPrice).orElse(0.0)));
        }
        return summaries;
    }
}

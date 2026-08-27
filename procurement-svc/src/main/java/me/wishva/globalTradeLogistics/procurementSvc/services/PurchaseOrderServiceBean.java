package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.interceptor.Audited;
import me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IInventoryService;
import me.wishva.globalTradeLogistics.core.local.IPurchaseOrderService;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import me.wishva.globalTradeLogistics.core.model.Grn;
import me.wishva.globalTradeLogistics.core.model.Product;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Shipment;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.SupplierProvidingProduct;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Purchase order lifecycle for {@code procurement-svc} — creation (by a
 * coordinator), GRN recording (by a warehouse manager, which feeds stock
 * back into {@code inventory-svc}), and a supplier's own read-only PO list
 * plus product-offering catalog. See {@link IInventoryService}'s javadoc for
 * why no method here takes a warehouse id.
 */
@Stateless
@Interceptors({RequiresRoleInterceptor.class, AuditInterceptor.class})
public class PurchaseOrderServiceBean implements IPurchaseOrderService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @EJB
    private IInventoryService inventoryService;

    @Override
    @RequiresRole(Role.COORDINATOR)
    @Audited(resource = "PROCUREMENT")
    public PurchaseOrderSummary createPo(Integer supplierId, Integer productId, Integer qty) {
        double unitPrice = resolveUnitPrice(productId);

        PurchaseOrder po = new PurchaseOrder();
        po.setSuppliersSupplierId(supplierId);
        po.setCreatedAt(Instant.now());
        po.setProductsProductId(productId);
        po.setRequestingQty(qty);
        po.setTotalPrice(unitPrice * qty);
        po.setIsCompleted(0);
        em.persist(po);
        em.flush();

        return toSummary(po);
    }

    @Override
    @RequiresRole(Role.WAREHOUSE_MANAGER)
    @Audited(resource = "PROCUREMENT")
    public PurchaseOrderSummary recordGrnForShipment(Integer shipmentId, Integer qty)
            throws ShipmentNotFoundException, PurchaseOrderNotFoundException, InvalidShipmentStateException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }
        if (shipment.getPurchaseOrdersPoId() == null) {
            throw new InvalidShipmentStateException("Shipment " + shipmentId + " isn't linked to a purchase order");
        }
        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new InvalidShipmentStateException("GRN can only be recorded once shipment " + shipmentId + " has been delivered");
        }

        PurchaseOrder po = em.find(PurchaseOrder.class, shipment.getPurchaseOrdersPoId());
        if (po == null) {
            throw new PurchaseOrderNotFoundException("No purchase order found with id " + shipment.getPurchaseOrdersPoId());
        }
        if (po.getIsCompleted() != 0) {
            throw new InvalidShipmentStateException("A GRN has already been recorded for shipment " + shipmentId);
        }

        Grn grn = new Grn();
        grn.setSuppliersSupplierId(po.getSuppliersSupplierId());
        grn.setCreatedAt(Instant.now());
        grn.setPurchaseOrdersPoId(po.getPoId());
        grn.setProductsProductId(po.getProductsProductId());
        grn.setQty(qty);
        em.persist(grn);

        inventoryService.incrementStock(po.getProductsProductId(), qty);

        po.setIsCompleted(1);

        return toSummary(po);
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public List<PurchaseOrderSummary> listForSupplier() throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        List<PurchaseOrder> orders = em.createNamedQuery("PurchaseOrder.findBySupplier", PurchaseOrder.class)
                .setParameter("supplierId", supplier.getSupplierId())
                .getResultList();

        List<PurchaseOrderSummary> summaries = new ArrayList<>();
        for (PurchaseOrder po : orders) {
            summaries.add(toSummary(po));
        }
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public List<PurchaseOrderSummary> listShippableForSupplier() throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        List<PurchaseOrder> orders = em.createNamedQuery("PurchaseOrder.findBySupplier", PurchaseOrder.class)
                .setParameter("supplierId", supplier.getSupplierId())
                .getResultList();

        List<PurchaseOrderSummary> summaries = new ArrayList<>();
        for (PurchaseOrder po : orders) {
            if (po.getIsCompleted() != 0) {
                continue;
            }
            long existingShipments = em.createNamedQuery("Shipment.countByPurchaseOrder", Long.class)
                    .setParameter("poId", po.getPoId())
                    .getSingleResult();
            if (existingShipments == 0) {
                summaries.add(toSummary(po));
            }
        }
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public void addProductOffering(Integer productId, Integer warehouseId, Integer leadTimeInDays) throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();

        SupplierProvidingProduct offering = new SupplierProvidingProduct();
        offering.setProductsProductId(productId);
        offering.setSuppliersSupplierId(supplier.getSupplierId());
        offering.setWarehousesWarehouseId(warehouseId);
        offering.setLeadTimeInDays(leadTimeInDays);
        em.persist(offering);
    }

    private double resolveUnitPrice(Integer productId) {
        List<Inventory> stock = em.createNamedQuery("Inventory.findByProductOrderByQtyDesc", Inventory.class)
                .setParameter("productId", productId)
                .getResultList();
        Optional<Inventory> best = stock.stream().max(Comparator.comparing(Inventory::getQty));
        return best.map(Inventory::getUnitPrice).orElse(0.0);
    }

    private PurchaseOrderSummary toSummary(PurchaseOrder po) {
        Product product = em.find(Product.class, po.getProductsProductId());
        return new PurchaseOrderSummary(
                po.getPoId(),
                po.getSuppliersSupplierId(),
                po.getProductsProductId(),
                product != null ? product.getName() : null,
                po.getRequestingQty(),
                po.getTotalPrice(),
                po.getIsCompleted() != 0,
                po.getCreatedAt());
    }

    private Supplier resolveSupplier() throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        List<Supplier> matches = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            throw new UnknownPrincipalException("No active supplier found for " + email);
        }
        return matches.get(0);
    }
}

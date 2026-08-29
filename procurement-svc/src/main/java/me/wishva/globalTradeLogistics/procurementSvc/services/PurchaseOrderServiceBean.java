package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
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
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;
import me.wishva.globalTradeLogistics.core.model.CustomClearanceRecord;
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

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    @RequiresRole(Role.COORDINATOR)
    @Audited(resource = "PROCUREMENT")
    public PurchaseOrderSummary createPo(Integer supplierId, Integer productId, Integer qty) {
        double unitPrice = resolveUnitPrice(productId);
        logEvent.fire(new LogEvent("supplier-" + supplierId, LogLevel.TRACE,
                "createPo: product " + productId + " qty " + qty + " unitPrice " + unitPrice));

        PurchaseOrder po = new PurchaseOrder();
        po.setSuppliersSupplierId(supplierId);
        po.setCreatedAt(Instant.now());
        po.setProductsProductId(productId);
        po.setRequestingQty(qty);
        po.setTotalPrice(unitPrice * qty);
        po.setIsCompleted(0);
        em.persist(po);
        em.flush();
        logEvent.fire(new LogEvent("supplier-" + supplierId, LogLevel.TRACE, "createPo: PO " + po.getPoId() + " persisted"));

        return toSummary(po);
    }

    @Override
    @RequiresRole(Role.WAREHOUSE_MANAGER)
    @Audited(resource = "PROCUREMENT")
    public PurchaseOrderSummary recordGrnForShipment(Integer shipmentId, Integer qty)
            throws ShipmentNotFoundException, PurchaseOrderNotFoundException, InvalidShipmentStateException {
        String key = "shipment-" + shipmentId;
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "recordGrnForShipment: loading shipment " + shipmentId));
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: shipment not found"));
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }
        if (shipment.getPurchaseOrdersPoId() == null) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: shipment not linked to a purchase order"));
            throw new InvalidShipmentStateException("Shipment " + shipmentId + " isn't linked to a purchase order");
        }
        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: shipment status is " + shipment.getStatus() + ", not DELIVERED"));
            throw new InvalidShipmentStateException("GRN can only be recorded once shipment " + shipmentId + " has been delivered");
        }

        List<CustomClearanceRecord> customsRecords = em.createNamedQuery(
                        "CustomClearanceRecord.findLatestByShipment", CustomClearanceRecord.class)
                .setParameter("shipmentId", shipmentId)
                .setMaxResults(1)
                .getResultList();
        if (customsRecords.isEmpty() || customsRecords.get(0).getStatus() != CustomsClearanceStatus.CLEARED) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: customs clearance not CLEARED"));
            throw new InvalidShipmentStateException(
                    "Customs clearance must be completed (CLEARED) before a GRN can be recorded for shipment " + shipmentId);
        }

        PurchaseOrder po = em.find(PurchaseOrder.class, shipment.getPurchaseOrdersPoId());
        if (po == null) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: linked PO " + shipment.getPurchaseOrdersPoId() + " not found"));
            throw new PurchaseOrderNotFoundException("No purchase order found with id " + shipment.getPurchaseOrdersPoId());
        }
        if (po.getIsCompleted() != 0) {
            logEvent.fire(new LogEvent(key, LogLevel.WARN, "recordGrnForShipment: GRN already recorded for this shipment"));
            throw new InvalidShipmentStateException("A GRN has already been recorded for shipment " + shipmentId);
        }

        Grn grn = new Grn();
        grn.setSuppliersSupplierId(po.getSuppliersSupplierId());
        grn.setCreatedAt(Instant.now());
        grn.setPurchaseOrdersPoId(po.getPoId());
        grn.setProductsProductId(po.getProductsProductId());
        grn.setQty(qty);
        em.persist(grn);
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "recordGrnForShipment: GRN persisted for PO " + po.getPoId() + ", qty " + qty));

        inventoryService.incrementStock(po.getProductsProductId(), qty);

        po.setIsCompleted(1);
        shipment.setStatus(ShipmentStatus.COMPLETED);
        logEvent.fire(new LogEvent(key, LogLevel.TRACE, "recordGrnForShipment: PO " + po.getPoId() + " completed, shipment marked COMPLETED"));

        return toSummary(po);
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public List<PurchaseOrderSummary> listForSupplier() throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE, "listForSupplier: loading POs"));
        List<PurchaseOrder> orders = em.createNamedQuery("PurchaseOrder.findBySupplier", PurchaseOrder.class)
                .setParameter("supplierId", supplier.getSupplierId())
                .getResultList();

        List<PurchaseOrderSummary> summaries = new ArrayList<>();
        for (PurchaseOrder po : orders) {
            summaries.add(toSummary(po));
        }
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE, "listForSupplier: returning " + summaries.size() + " PO(s)"));
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public List<PurchaseOrderSummary> listShippableForSupplier() throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE, "listShippableForSupplier: loading shippable POs"));
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
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE,
                "listShippableForSupplier: returning " + summaries.size() + " shippable PO(s)"));
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public void addProductOffering(Integer productId, Integer warehouseId, Integer leadTimeInDays) throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE,
                "addProductOffering: product " + productId + " warehouse " + warehouseId + " leadTime " + leadTimeInDays + " days"));

        SupplierProvidingProduct offering = new SupplierProvidingProduct();
        offering.setProductsProductId(productId);
        offering.setSuppliersSupplierId(supplier.getSupplierId());
        offering.setWarehousesWarehouseId(warehouseId);
        offering.setLeadTimeInDays(leadTimeInDays);
        em.persist(offering);
        logEvent.fire(new LogEvent(supplier.getEmail(), LogLevel.TRACE, "addProductOffering: offering persisted"));
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
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "resolveSupplier: no active supplier found"));
            throw new UnknownPrincipalException("No active supplier found for " + email);
        }
        return matches.get(0);
    }
}

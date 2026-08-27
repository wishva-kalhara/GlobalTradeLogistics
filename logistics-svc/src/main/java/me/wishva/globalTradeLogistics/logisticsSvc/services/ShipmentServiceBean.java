package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.ShipmentSummary;
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.interceptor.Audited;
import me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.IdempotencyChecked;
import me.wishva.globalTradeLogistics.core.interceptor.IdempotencyInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IShipmentService;
import me.wishva.globalTradeLogistics.core.model.CustomClearanceRecord;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Shipment;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.Warehouse;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Shipment status updates and customs clearance records, for a customs
 * agent. Interceptor order matters: {@code RequiresRoleInterceptor} (auth
 * first) → {@code IdempotencyInterceptor} (short-circuit a repeat call
 * before it does anything) → {@code AuditInterceptor} (only fires if the
 * business method actually ran) — see {@link IdempotencyInterceptor}'s
 * javadoc.
 */
@Stateless
@Interceptors({RequiresRoleInterceptor.class, IdempotencyInterceptor.class, AuditInterceptor.class})
public class ShipmentServiceBean implements IShipmentService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @EJB
    private CarrierGatewayBean carrierGatewayBean;

    @Override
    @RequiresRole(Role.CUSTOMS_AGENT)
    public ShipmentSummary getShipment(Integer shipmentId) throws ShipmentNotFoundException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }
        return toSummary(shipment);
    }

    @Override
    @RequiresRole(Role.CUSTOMS_AGENT)
    @IdempotencyChecked
    @Audited(resource = "LOGISTICS")
    public ShipmentSummary updateStatus(Integer shipmentId, ShipmentStatus newStatus, String idempotencyKey)
            throws ShipmentNotFoundException, InvalidShipmentStateException {
        if (newStatus == ShipmentStatus.COMPLETED) {
            throw new InvalidShipmentStateException(
                    "Shipment status COMPLETED is set automatically when a GRN is recorded — it can't be set directly");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }
        shipment.setStatus(newStatus);
        return toSummary(shipment);
    }

    @Override
    @RequiresRole(Role.CUSTOMS_AGENT)
    @Audited(resource = "LOGISTICS")
    public void createCustomsRecord(Integer shipmentId, String declarationNumber) throws ShipmentNotFoundException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }

        CustomClearanceRecord record = new CustomClearanceRecord();
        record.setDeclarationNumber(declarationNumber);
        record.setSupplierShipmentsShipmentId(shipmentId);
        record.setStatus(CustomsClearanceStatus.PENDING);
        em.persist(record);
    }

    @Override
    @RequiresRole(Role.CUSTOMS_AGENT)
    @Audited(resource = "LOGISTICS")
    public ShipmentSummary updateCustomsStatus(Integer shipmentId, CustomsClearanceStatus status)
            throws ShipmentNotFoundException, InvalidShipmentStateException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentNotFoundException("No shipment found with id " + shipmentId);
        }

        CustomClearanceRecord record = resolveLatestCustomsRecord(shipmentId);
        if (record == null) {
            throw new InvalidShipmentStateException(
                    "No customs record exists yet for shipment " + shipmentId + " — create one first");
        }
        record.setStatus(status);

        return toSummary(shipment);
    }

    @Override
    public ShipmentSummary notifyCarrierSystem(Integer shipmentId) throws ShipmentNotFoundException {
        carrierGatewayBean.notifyCarrierSystem(shipmentId);
        Shipment shipment = em.find(Shipment.class, shipmentId);
        return toSummary(shipment);
    }

    @Override
    @RequiresRole({Role.ADMIN, Role.COORDINATOR, Role.WAREHOUSE_MANAGER, Role.CUSTOMS_AGENT})
    public List<ShipmentSummary> listAll() {
        List<Shipment> shipments = em.createNamedQuery("Shipment.findAllOrderedByIdDesc", Shipment.class)
                .getResultList();

        List<ShipmentSummary> summaries = new ArrayList<>();
        for (Shipment shipment : shipments) {
            summaries.add(toSummary(shipment));
        }
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public List<ShipmentSummary> listForSupplier() throws UnknownPrincipalException {
        Supplier supplier = resolveSupplier();
        List<Shipment> shipments = em.createNamedQuery("Shipment.findBySupplier", Shipment.class)
                .setParameter("supplierId", supplier.getSupplierId())
                .getResultList();

        List<ShipmentSummary> summaries = new ArrayList<>();
        for (Shipment shipment : shipments) {
            summaries.add(toSummary(shipment));
        }
        return summaries;
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    @Audited(resource = "LOGISTICS")
    public ShipmentSummary createShipmentForPurchaseOrder(Integer poId, String trackingNumber, String vesselId, String type)
            throws PurchaseOrderNotFoundException, InvalidShipmentStateException, UnknownPrincipalException {
        Supplier supplier = resolveSupplier();

        PurchaseOrder po = em.find(PurchaseOrder.class, poId);
        if (po == null || !po.getSuppliersSupplierId().equals(supplier.getSupplierId())) {
            throw new PurchaseOrderNotFoundException("No purchase order found with id " + poId);
        }
        if (po.getIsCompleted() != 0) {
            throw new InvalidShipmentStateException("Purchase order " + poId + " is already completed");
        }

        long existingShipments = em.createNamedQuery("Shipment.countByPurchaseOrder", Long.class)
                .setParameter("poId", poId)
                .getSingleResult();
        if (existingShipments > 0) {
            throw new InvalidShipmentStateException("A shipment already exists for purchase order " + poId);
        }

        List<Warehouse> warehouses = em.createQuery("SELECT w FROM Warehouse w ORDER BY w.warehouseId", Warehouse.class)
                .setMaxResults(1)
                .getResultList();

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setVesselId(vesselId);
        shipment.setType(type);
        shipment.setWarehousesWarehouseId(warehouses.isEmpty() ? null : warehouses.get(0).getWarehouseId());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setPurchaseOrdersPoId(poId);
        em.persist(shipment);
        em.flush();

        return toSummary(shipment);
    }

    @Override
    @RequiresRole(Role.WAREHOUSE_MANAGER)
    public List<ShipmentSummary> listDeliveredAwaitingGrn() {
        List<Shipment> shipments = em.createNamedQuery("Shipment.findDeliveredAwaitingGrn", Shipment.class)
                .setParameter("status", ShipmentStatus.DELIVERED)
                .getResultList();

        List<ShipmentSummary> summaries = new ArrayList<>();
        for (Shipment shipment : shipments) {
            summaries.add(toSummary(shipment));
        }
        return summaries;
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

    private CustomClearanceRecord resolveLatestCustomsRecord(Integer shipmentId) {
        List<CustomClearanceRecord> records = em.createNamedQuery("CustomClearanceRecord.findLatestByShipment", CustomClearanceRecord.class)
                .setParameter("shipmentId", shipmentId)
                .setMaxResults(1)
                .getResultList();
        return records.isEmpty() ? null : records.get(0);
    }

    private ShipmentSummary toSummary(Shipment shipment) {
        CustomClearanceRecord latestCustomsRecord = resolveLatestCustomsRecord(shipment.getShipmentId());
        return new ShipmentSummary(
                shipment.getShipmentId(),
                shipment.getTrackingNumber(),
                shipment.getVesselId(),
                shipment.getType(),
                shipment.getWarehousesWarehouseId(),
                shipment.getStatus(),
                shipment.getShipmentType(),
                shipment.getRef(),
                shipment.getPurchaseOrdersPoId(),
                latestCustomsRecord != null ? latestCustomsRecord.getStatus() : null);
    }
}

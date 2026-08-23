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
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.interceptor.Audited;
import me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.IdempotencyChecked;
import me.wishva.globalTradeLogistics.core.interceptor.IdempotencyInterceptor;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IShipmentService;
import me.wishva.globalTradeLogistics.core.model.CustomClearanceRecord;
import me.wishva.globalTradeLogistics.core.model.Shipment;

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
            throws ShipmentNotFoundException {
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
    public ShipmentSummary notifyCarrierSystem(Integer shipmentId) throws ShipmentNotFoundException {
        carrierGatewayBean.notifyCarrierSystem(shipmentId);
        Shipment shipment = em.find(Shipment.class, shipmentId);
        return toSummary(shipment);
    }

    private ShipmentSummary toSummary(Shipment shipment) {
        return new ShipmentSummary(
                shipment.getShipmentId(),
                shipment.getTrackingNumber(),
                shipment.getVesselId(),
                shipment.getType(),
                shipment.getWarehousesWarehouseId(),
                shipment.getStatus(),
                shipment.getShipmentType(),
                shipment.getRef());
    }
}

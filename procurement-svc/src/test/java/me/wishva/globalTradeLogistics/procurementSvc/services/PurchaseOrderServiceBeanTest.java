package me.wishva.globalTradeLogistics.procurementSvc.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import me.wishva.globalTradeLogistics.core.dto.PurchaseOrderSummary;
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;
import me.wishva.globalTradeLogistics.core.local.IInventoryService;
import me.wishva.globalTradeLogistics.core.model.CustomClearanceRecord;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Shipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the ship -> customs -> GRN gate built into {@code recordGrnForShipment}
 * this session — the bean is instantiated directly (no container proxy), so
 * its @RequiresRole/@Audited interceptors never run here; only the plain
 * business logic is under test.
 */
class PurchaseOrderServiceBeanTest {

    private PurchaseOrderServiceBean bean;
    private EntityManager em;
    private IInventoryService inventoryService;

    @BeforeEach
    void setUp() throws Exception {
        bean = new PurchaseOrderServiceBean();
        em = mock(EntityManager.class);
        inventoryService = mock(IInventoryService.class);
        setField(bean, "em", em);
        setField(bean, "inventoryService", inventoryService);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private void stubClearedCustomsRecord(Integer shipmentId) {
        CustomClearanceRecord record = new CustomClearanceRecord();
        record.setStatus(CustomsClearanceStatus.CLEARED);
        TypedQuery<CustomClearanceRecord> query = mock(TypedQuery.class);
        when(em.createNamedQuery(eq("CustomClearanceRecord.findLatestByShipment"), eq(CustomClearanceRecord.class)))
                .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(record));
    }

    @SuppressWarnings("unchecked")
    private void stubNoCustomsRecord() {
        TypedQuery<CustomClearanceRecord> query = mock(TypedQuery.class);
        when(em.createNamedQuery(eq("CustomClearanceRecord.findLatestByShipment"), eq(CustomClearanceRecord.class)))
                .thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
    }

    @Test
    void recordGrnForShipment_shipmentNotFound_throwsShipmentNotFoundException() {
        when(em.find(Shipment.class, 99)).thenReturn(null);

        assertThrows(ShipmentNotFoundException.class, () -> bean.recordGrnForShipment(99, 10));
    }

    @Test
    void recordGrnForShipment_notYetDelivered_throwsInvalidShipmentStateException() {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(1);
        shipment.setPurchaseOrdersPoId(5);
        shipment.setStatus(ShipmentStatus.CREATED);
        when(em.find(Shipment.class, 1)).thenReturn(shipment);

        assertThrows(InvalidShipmentStateException.class, () -> bean.recordGrnForShipment(1, 10));
    }

    @Test
    void recordGrnForShipment_customsNotCleared_throwsInvalidShipmentStateException() {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(1);
        shipment.setPurchaseOrdersPoId(5);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(em.find(Shipment.class, 1)).thenReturn(shipment);
        stubNoCustomsRecord();

        assertThrows(InvalidShipmentStateException.class, () -> bean.recordGrnForShipment(1, 10));
    }

    @Test
    void recordGrnForShipment_deliveredAndCleared_completesPoAndShipmentAndIncrementsStock()
            throws ShipmentNotFoundException, PurchaseOrderNotFoundException, InvalidShipmentStateException {
        Shipment shipment = new Shipment();
        shipment.setShipmentId(1);
        shipment.setPurchaseOrdersPoId(5);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(em.find(Shipment.class, 1)).thenReturn(shipment);
        stubClearedCustomsRecord(1);

        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(5);
        po.setSuppliersSupplierId(7);
        po.setProductsProductId(3);
        po.setIsCompleted(0);
        po.setTotalPrice(100.0);
        when(em.find(PurchaseOrder.class, 5)).thenReturn(po);
        when(em.find(me.wishva.globalTradeLogistics.core.model.Product.class, 3)).thenReturn(null);

        PurchaseOrderSummary summary = bean.recordGrnForShipment(1, 10);

        assertTrue(summary.isCompleted());
        assertEquals(5, summary.getPoId());
        assertEquals(1, po.getIsCompleted());
        assertEquals(ShipmentStatus.COMPLETED, shipment.getStatus());
        verify(inventoryService).incrementStock(3, 10);
        verify(em).persist(any(me.wishva.globalTradeLogistics.core.model.Grn.class));
    }
}

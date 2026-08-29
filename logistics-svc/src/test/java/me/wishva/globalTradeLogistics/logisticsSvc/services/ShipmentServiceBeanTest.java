package me.wishva.globalTradeLogistics.logisticsSvc.services;

import jakarta.enterprise.event.Event;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;
import me.wishva.globalTradeLogistics.core.exception.PurchaseOrderNotFoundException;
import me.wishva.globalTradeLogistics.core.model.PurchaseOrder;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers two of the invariants built into the ship -> customs -> GRN flow
 * this session: {@code COMPLETED} is never settable directly, and a
 * supplier can't create a shipment for a PO that isn't their own.
 */
class ShipmentServiceBeanTest {

    private ShipmentServiceBean bean;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        bean = new ShipmentServiceBean();
        em = mock(EntityManager.class);
        Field field = ShipmentServiceBean.class.getDeclaredField("em");
        field.setAccessible(true);
        field.set(bean, em);

        Field logEventField = ShipmentServiceBean.class.getDeclaredField("logEvent");
        logEventField.setAccessible(true);
        logEventField.set(bean, mock(Event.class));
    }

    @AfterEach
    void clearPrincipal() {
        CurrentPrincipalHolder.clear();
    }

    @Test
    void updateStatus_rejectsCompletedStatus_asItIsOnlySetByARecordedGrn() {
        assertThrows(InvalidShipmentStateException.class,
                () -> bean.updateStatus(1, ShipmentStatus.COMPLETED, "any-key"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createShipmentForPurchaseOrder_poBelongsToAnotherSupplier_throwsPurchaseOrderNotFoundException() {
        CurrentPrincipalHolder.set(new CurrentPrincipal("vendor@example.com", Role.VENDOR_REP));

        Supplier callingSupplier = new Supplier();
        callingSupplier.setSupplierId(1);
        TypedQuery<Supplier> supplierQuery = mock(TypedQuery.class);
        when(em.createNamedQuery(eq("Supplier.findActiveByEmail"), eq(Supplier.class))).thenReturn(supplierQuery);
        when(supplierQuery.setParameter(anyString(), any())).thenReturn(supplierQuery);
        when(supplierQuery.getResultList()).thenReturn(List.of(callingSupplier));

        PurchaseOrder poOwnedBySomeoneElse = new PurchaseOrder();
        poOwnedBySomeoneElse.setPoId(42);
        poOwnedBySomeoneElse.setSuppliersSupplierId(2);
        poOwnedBySomeoneElse.setIsCompleted(0);
        when(em.find(PurchaseOrder.class, 42)).thenReturn(poOwnedBySomeoneElse);

        assertThrows(PurchaseOrderNotFoundException.class,
                () -> bean.createShipmentForPurchaseOrder(42, "TRK-1", "VESSEL-1", "SEA"));
    }
}

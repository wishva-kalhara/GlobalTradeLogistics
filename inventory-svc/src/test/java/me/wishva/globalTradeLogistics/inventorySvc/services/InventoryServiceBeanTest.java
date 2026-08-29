package me.wishva.globalTradeLogistics.inventorySvc.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceBeanTest {

    private InventoryServiceBean bean;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        bean = new InventoryServiceBean();
        em = mock(EntityManager.class);
        Field field = InventoryServiceBean.class.getDeclaredField("em");
        field.setAccessible(true);
        field.set(bean, em);
    }

    @SuppressWarnings("unchecked")
    private void stubInventoryRowForProduct(Integer productId, Inventory inventory) {
        TypedQuery<Inventory> query = mock(TypedQuery.class);
        when(em.createNamedQuery(eq("Inventory.findByProductOrderByQtyDesc"), eq(Inventory.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(inventory == null ? List.of() : List.of(inventory));
    }

    @Test
    void decrementStock_requestedQtyExceedsAvailable_throwsInsufficientInventoryException() {
        Inventory inventory = new Inventory();
        inventory.setProductsProductId(3);
        inventory.setQty(5);
        stubInventoryRowForProduct(3, inventory);

        assertThrows(InsufficientInventoryException.class, () -> bean.decrementStock(3, 10));
        assertEquals(5, inventory.getQty(), "qty must be unchanged when the decrement is rejected");
    }

    @Test
    void decrementStock_sufficientAvailable_decrementsByTheRequestedQty() throws InsufficientInventoryException {
        Inventory inventory = new Inventory();
        inventory.setProductsProductId(3);
        inventory.setQty(10);
        stubInventoryRowForProduct(3, inventory);

        bean.decrementStock(3, 4);

        assertEquals(6, inventory.getQty());
    }
}

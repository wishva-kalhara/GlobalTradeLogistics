package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import me.wishva.globalTradeLogistics.core.dto.SupplierSummary;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the "no bare-email suppliers in the create-PO dropdown" filter
 * added this session: only suppliers with a completed profile (non-blank
 * fullName) should ever come back from listSuppliers().
 */
class UserAdminServiceBeanTest {

    @Test
    @SuppressWarnings("unchecked")
    void listSuppliers_excludesSuppliersWithoutACompletedProfile() throws Exception {
        UserAdminServiceBean bean = new UserAdminServiceBean();
        EntityManager em = mock(EntityManager.class);
        Field field = UserAdminServiceBean.class.getDeclaredField("em");
        field.setAccessible(true);
        field.set(bean, em);

        Supplier onboarded = new Supplier();
        onboarded.setSupplierId(1);
        onboarded.setEmail("onboarded@example.com");
        onboarded.setFullName("Onboarded Supplier Co");

        Supplier bareSignup = new Supplier();
        bareSignup.setSupplierId(2);
        bareSignup.setEmail("bare-signup@example.com");
        bareSignup.setFullName(null);

        Supplier blankFullName = new Supplier();
        blankFullName.setSupplierId(3);
        blankFullName.setEmail("blank@example.com");
        blankFullName.setFullName("   ");

        TypedQuery<Supplier> query = mock(TypedQuery.class);
        when(em.createNamedQuery(eq("Supplier.findAllActive"), eq(Supplier.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(onboarded, bareSignup, blankFullName));

        List<SupplierSummary> result = bean.listSuppliers();

        assertEquals(1, result.size());
        assertEquals("onboarded@example.com", result.get(0).getEmail());
        assertEquals("Onboarded Supplier Co", result.get(0).getFullName());
    }
}

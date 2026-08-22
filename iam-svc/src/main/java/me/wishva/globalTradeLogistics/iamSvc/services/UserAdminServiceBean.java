package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IUserAdminService;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.User;

import java.util.Collections;
import java.util.Map;

/**
 * Implements the Admin Provisions a Staff User (1.12), Customer Onboarding
 * (1.13), and Supplier Onboarding (1.14) flows. Every method is guarded by
 * {@code @RequiresRole(Role.ADMIN)}, enforced by {@link RequiresRoleInterceptor}
 * (1.10) — the class-level {@code @Interceptors} association is the
 * "multiple business interceptor methods on one bean" example, since this
 * bean will grow further {@code @RequiresRole}-guarded methods over time.
 */
@Stateless
@Interceptors(RequiresRoleInterceptor.class)
public class UserAdminServiceBean implements IUserAdminService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Override
    @RequiresRole(Role.ADMIN)
    public void createUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        em.persist(user);

        NotificationPublisher.publish(new EmailNotification(
                EmailType.WORKER_ONBOARDING, email, fullName, Map.of("role", role.name())));
    }

    @Override
    @RequiresRole(Role.ADMIN)
    public void registerCustomer(String email, String fullName, String mobile1, String address, String country, String regionKey) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setFullName(fullName);
        customer.setMobile1(mobile1);
        customer.setAddress(address);
        customer.setCountry(country);
        customer.setRegionKey(regionKey);
        customer.setIsActive("true");
        em.persist(customer);

        NotificationPublisher.publish(new EmailNotification(
                EmailType.CUSTOMER_ONBOARDING, email, fullName, Collections.emptyMap()));
    }

    @Override
    @RequiresRole(Role.ADMIN)
    public void registerSupplier(String email, String fullName, String mobile1, String address, String country) {
        Supplier supplier = new Supplier();
        supplier.setEmail(email);
        supplier.setFullName(fullName);
        supplier.setMobile1(mobile1);
        supplier.setAddress(address);
        supplier.setCountry(country);
        supplier.setIsActive("true");
        em.persist(supplier);

        NotificationPublisher.publish(new EmailNotification(
                EmailType.SUPPLIER_ONBOARDING, email, fullName, Collections.emptyMap()));
    }
}

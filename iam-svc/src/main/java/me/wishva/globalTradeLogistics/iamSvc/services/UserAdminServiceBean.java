package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.SupplierSummary;
import me.wishva.globalTradeLogistics.core.dto.UserSummary;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IUserAdminService;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    @RequiresRole(Role.ADMIN)
    public void createUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        em.persist(user);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "createUser: staff user persisted with role " + role));

        NotificationPublisher.publish(new EmailNotification(
                EmailType.WORKER_ONBOARDING, email, fullName, Map.of("role", role.name())));
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "createUser: WORKER_ONBOARDING notification published"));
    }

    @Override
    @RequiresRole(Role.ADMIN)
    public void registerCustomer(String email, String fullName, String mobile1, String address, String country) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setFullName(fullName);
        customer.setMobile1(mobile1);
        customer.setAddress(address);
        customer.setCountry(country);
        customer.setIsActive("true");
        em.persist(customer);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "registerCustomer: customer persisted"));

        NotificationPublisher.publish(new EmailNotification(
                EmailType.CUSTOMER_ONBOARDING, email, fullName, Collections.emptyMap()));
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "registerCustomer: CUSTOMER_ONBOARDING notification published"));
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
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "registerSupplier: supplier persisted"));

        NotificationPublisher.publish(new EmailNotification(
                EmailType.SUPPLIER_ONBOARDING, email, fullName, Collections.emptyMap()));
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "registerSupplier: SUPPLIER_ONBOARDING notification published"));
    }

    @Override
    @RequiresRole(Role.ADMIN)
    public List<UserSummary> listUsers() {
        logEvent.fire(new LogEvent("admin-users", LogLevel.TRACE, "listUsers: loading staff users"));
        return em.createNamedQuery("User.findAll", User.class)
                .getResultList()
                .stream()
                .map(u -> new UserSummary(u.getEmail(), u.getFullName(), u.getRole()))
                .collect(Collectors.toList());
    }

    @Override
    @RequiresRole({Role.ADMIN, Role.COORDINATOR})
    public List<SupplierSummary> listSuppliers() {
        logEvent.fire(new LogEvent("admin-suppliers", LogLevel.TRACE, "listSuppliers: loading active suppliers"));
        return em.createNamedQuery("Supplier.findAllActive", Supplier.class)
                .getResultList()
                .stream()
                .filter(s -> s.getFullName() != null && !s.getFullName().isBlank())
                .map(s -> new SupplierSummary(s.getSupplierId(), s.getFullName(), s.getEmail()))
                .collect(Collectors.toList());
    }

    @Override
    @RequiresRole({Role.ADMIN, Role.COORDINATOR})
    public List<SupplierSummary> listSuppliersForProduct(Integer productId) {
        logEvent.fire(new LogEvent("product-" + productId, LogLevel.TRACE, "listSuppliersForProduct: loading suppliers"));
        List<Integer> supplierIds = em.createNamedQuery("SupplierProvidingProduct.findSupplierIdsByProduct", Integer.class)
                .setParameter("productId", productId)
                .getResultList();
        if (supplierIds.isEmpty()) {
            logEvent.fire(new LogEvent("product-" + productId, LogLevel.TRACE, "listSuppliersForProduct: no suppliers offer this product"));
            return Collections.emptyList();
        }

        return em.createNamedQuery("Supplier.findAllActive", Supplier.class)
                .getResultList()
                .stream()
                .filter(s -> supplierIds.contains(s.getSupplierId()))
                .filter(s -> s.getFullName() != null && !s.getFullName().isBlank())
                .map(s -> new SupplierSummary(s.getSupplierId(), s.getFullName(), s.getEmail()))
                .collect(Collectors.toList());
    }
}

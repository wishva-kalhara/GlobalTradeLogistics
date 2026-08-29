package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.ProfileSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRole;
import me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor;
import me.wishva.globalTradeLogistics.core.local.IProfileService;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.List;

/**
 * Profile completion (shipping/contact details) after sign-up. Which row
 * gets updated is resolved from {@link CurrentPrincipalHolder} (the JWT's
 * {@code sub} claim), never a client-supplied email — one principal can
 * only ever edit their own row.
 */
@Stateless
@Interceptors(RequiresRoleInterceptor.class)
public class ProfileServiceBean implements IProfileService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    @RequiresRole(Role.CUSTOMER)
    public ProfileSummary getCustomerProfile() throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "getCustomerProfile: loading profile"));
        List<Customer> matches = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "getCustomerProfile: no active customer found"));
            throw new UnknownPrincipalException("No active customer found for " + email);
        }

        Customer customer = matches.get(0);
        return new ProfileSummary(
                customer.getEmail(), customer.getFullName(), customer.getMobile1(),
                customer.getMobile2(), customer.getAddress(), customer.getCountry());
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public ProfileSummary getSupplierProfile() throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "getSupplierProfile: loading profile"));
        List<Supplier> matches = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "getSupplierProfile: no active supplier found"));
            throw new UnknownPrincipalException("No active supplier found for " + email);
        }

        Supplier supplier = matches.get(0);
        return new ProfileSummary(
                supplier.getEmail(), supplier.getFullName(), supplier.getMobile1(),
                supplier.getMobile2(), supplier.getAddress(), supplier.getCountry());
    }

    @Override
    @RequiresRole(Role.CUSTOMER)
    public void updateCustomerProfile(String fullName, String mobile1, String mobile2, String address, String country)
            throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "updateCustomerProfile: updating profile"));
        List<Customer> matches = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "updateCustomerProfile: no active customer found"));
            throw new UnknownPrincipalException("No active customer found for " + email);
        }

        Customer customer = matches.get(0);
        customer.setFullName(fullName);
        customer.setMobile1(mobile1);
        customer.setMobile2(mobile2);
        customer.setAddress(address);
        customer.setCountry(country);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "updateCustomerProfile: profile updated"));
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public void updateSupplierProfile(String fullName, String mobile1, String mobile2, String address, String country)
            throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "updateSupplierProfile: updating profile"));
        List<Supplier> matches = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "updateSupplierProfile: no active supplier found"));
            throw new UnknownPrincipalException("No active supplier found for " + email);
        }

        Supplier supplier = matches.get(0);
        supplier.setFullName(fullName);
        supplier.setMobile1(mobile1);
        supplier.setMobile2(mobile2);
        supplier.setAddress(address);
        supplier.setCountry(country);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "updateSupplierProfile: profile updated"));
    }
}

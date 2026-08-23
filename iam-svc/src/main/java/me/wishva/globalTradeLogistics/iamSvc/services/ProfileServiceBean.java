package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @Override
    @RequiresRole(Role.CUSTOMER)
    public void updateCustomerProfile(String mobile1, String mobile2, String address, String country)
            throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        List<Customer> matches = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            throw new UnknownPrincipalException("No active customer found for " + email);
        }

        Customer customer = matches.get(0);
        customer.setMobile1(mobile1);
        customer.setMobile2(mobile2);
        customer.setAddress(address);
        customer.setCountry(country);
    }

    @Override
    @RequiresRole(Role.VENDOR_REP)
    public void updateSupplierProfile(String mobile1, String mobile2, String address, String country)
            throws UnknownPrincipalException {
        String email = CurrentPrincipalHolder.get().getEmail();
        List<Supplier> matches = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (matches.isEmpty()) {
            throw new UnknownPrincipalException("No active supplier found for " + email);
        }

        Supplier supplier = matches.get(0);
        supplier.setMobile1(mobile1);
        supplier.setMobile2(mobile2);
        supplier.setAddress(address);
        supplier.setCountry(country);
    }
}

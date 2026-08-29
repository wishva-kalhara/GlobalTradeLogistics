package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.EmailAlreadyRegisteredException;
import me.wishva.globalTradeLogistics.core.local.IRegistrationService;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.security.JwtService;

import java.util.Collections;

/**
 * Public self-service signup (customer/seller "Create Account" flows).
 * Deliberately unguarded — there is no principal yet when these run — and
 * auto-logs the caller in on success so the browser can go straight to the
 * profile-completion page.
 */
@Stateless
public class RegistrationServiceBean implements IRegistrationService {

    private static final long TOKEN_TTL_SECONDS = 60 * 60;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public AuthResult signUpCustomer(String email, String country) throws EmailAlreadyRegisteredException {
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpCustomer: checking for an existing account"));
        long existing = em.createNamedQuery("Customer.countByEmail", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (existing > 0) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "signUpCustomer: email already registered"));
            throw new EmailAlreadyRegisteredException("An account already exists for " + email);
        }

        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setCountry(country);
        customer.setIsActive("true");
        em.persist(customer);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpCustomer: customer persisted, country=" + country));

        NotificationPublisher.publish(new EmailNotification(
                EmailType.CUSTOMER_ONBOARDING, email, null, Collections.emptyMap()));

        String token = JwtService.issueToken(email, Role.CUSTOMER, AppConfig.JWT_SECRET, TOKEN_TTL_SECONDS);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpCustomer: auto-login JWT issued"));
        return new AuthResult(token, email, Role.CUSTOMER);
    }

    @Override
    public AuthResult signUpSupplier(String email, String country) throws EmailAlreadyRegisteredException {
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpSupplier: checking for an existing account"));
        long existing = em.createNamedQuery("Supplier.countByEmail", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (existing > 0) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "signUpSupplier: email already registered"));
            throw new EmailAlreadyRegisteredException("An account already exists for " + email);
        }

        Supplier supplier = new Supplier();
        supplier.setEmail(email);
        supplier.setCountry(country);
        supplier.setIsActive("true");
        em.persist(supplier);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpSupplier: supplier persisted, country=" + country));

        NotificationPublisher.publish(new EmailNotification(
                EmailType.SUPPLIER_ONBOARDING, email, null, Collections.emptyMap()));

        String token = JwtService.issueToken(email, Role.VENDOR_REP, AppConfig.JWT_SECRET, TOKEN_TTL_SECONDS);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "signUpSupplier: auto-login JWT issued"));
        return new AuthResult(token, email, Role.VENDOR_REP);
    }
}

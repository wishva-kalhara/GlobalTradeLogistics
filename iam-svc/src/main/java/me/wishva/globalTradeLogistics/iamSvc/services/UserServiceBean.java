package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.EJB;
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
import me.wishva.globalTradeLogistics.core.exception.OtpExpiredOrInvalidException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher;
import me.wishva.globalTradeLogistics.core.model.Customer;
import me.wishva.globalTradeLogistics.core.model.Supplier;
import me.wishva.globalTradeLogistics.core.model.User;
import me.wishva.globalTradeLogistics.core.remote.IUsersService;
import me.wishva.globalTradeLogistics.core.security.JwtService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the OTP Request (1.1-1.3) and OTP Verify & JWT Issuance
 * (1.4-1.8) flows.
 */
@Stateless
public class UserServiceBean implements IUsersService {

    private static final long TOKEN_TTL_SECONDS = 60 * 60;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @EJB
    private OtpServiceBean otpService;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public void requestOtp(String email) throws UnknownPrincipalException {
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "requestOtp: resolving role for " + email));
        resolveRole(email);

        String code = otpService.generateAndStore(email);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "requestOtp: OTP generated and stored for " + email));

        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        NotificationPublisher.publish(new EmailNotification(EmailType.OTP_AUTHENTICATION, email, null, params));
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "requestOtp: OTP_AUTHENTICATION notification published for " + email));
    }

    @Override
    public AuthResult verifyOtp(String email, String code) throws OtpExpiredOrInvalidException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "verifyOtp: verifying OTP for " + email));
        if (!otpService.verifyAndConsume(email, code)) {
            logEvent.fire(new LogEvent(email, LogLevel.WARN, "verifyOtp: invalid or expired OTP for " + email));
            throw new OtpExpiredOrInvalidException("OTP is invalid or expired for " + email);
        }

        Role role = resolveRole(email);
        String token = JwtService.issueToken(email, role, AppConfig.JWT_SECRET, TOKEN_TTL_SECONDS);
        logEvent.fire(new LogEvent(email, LogLevel.TRACE, "verifyOtp: JWT issued for " + email + " with role " + role));
        return new AuthResult(token, email, role);
    }

    /**
     * Resolves the principal's role by checking {@code users}, then
     * {@code customers}, then {@code suppliers}, in that order (1.5).
     */
    private Role resolveRole(String email) throws UnknownPrincipalException {
        List<User> users = em.createNamedQuery("User.findActiveByEmail", User.class)
                .setParameter("email", email)
                .getResultList();
        if (!users.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.TRACE, "resolveRole: matched staff user, role=" + users.get(0).getRole()));
            return users.get(0).getRole();
        }

        List<Customer> customers = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (!customers.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.TRACE, "resolveRole: matched customer"));
            return Role.CUSTOMER;
        }

        List<Supplier> suppliers = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (!suppliers.isEmpty()) {
            logEvent.fire(new LogEvent(email, LogLevel.TRACE, "resolveRole: matched supplier"));
            return Role.VENDOR_REP;
        }

        logEvent.fire(new LogEvent(email, LogLevel.WARN, "resolveRole: no active principal found for " + email));
        throw new UnknownPrincipalException("No active principal found for email " + email);
    }
}

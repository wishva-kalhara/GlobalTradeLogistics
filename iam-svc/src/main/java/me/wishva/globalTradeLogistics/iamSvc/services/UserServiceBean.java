package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.enums.EmailType;
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

    @Override
    public void requestOtp(String email) throws UnknownPrincipalException {
        resolveRole(email);

        String code = otpService.generateAndStore(email);

        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        NotificationPublisher.publish(new EmailNotification(EmailType.OTP_AUTHENTICATION, email, null, params));
    }

    @Override
    public AuthResult verifyOtp(String email, String code) throws OtpExpiredOrInvalidException, UnknownPrincipalException {
        if (!otpService.verifyAndConsume(email, code)) {
            throw new OtpExpiredOrInvalidException("OTP is invalid or expired for " + email);
        }

        Role role = resolveRole(email);
        String token = JwtService.issueToken(email, role, AppConfig.JWT_SECRET, TOKEN_TTL_SECONDS);
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
            return users.get(0).getRole();
        }

        List<Customer> customers = em.createNamedQuery("Customer.findActiveByEmail", Customer.class)
                .setParameter("email", email)
                .getResultList();
        if (!customers.isEmpty()) {
            return Role.CUSTOMER;
        }

        List<Supplier> suppliers = em.createNamedQuery("Supplier.findActiveByEmail", Supplier.class)
                .setParameter("email", email)
                .getResultList();
        if (!suppliers.isEmpty()) {
            return Role.VENDOR_REP;
        }

        throw new UnknownPrincipalException("No active principal found for email " + email);
    }
}

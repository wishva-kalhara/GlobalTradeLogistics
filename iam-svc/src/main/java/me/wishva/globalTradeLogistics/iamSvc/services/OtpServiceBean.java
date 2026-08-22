package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.exception.SupplyChainSystemException;
import me.wishva.globalTradeLogistics.core.model.OtpCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * No-interface-view EJB — only ever injected within iam-svc (by
 * {@link UserServiceBean}), so it doesn't need a {@code core} contract.
 * Implements flows 1.2 (generate/store) and 1.4 (verify/consume).
 */
@Stateless
public class OtpServiceBean {

    private static final long OTP_TTL_SECONDS = 5 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    public String generateAndStore(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setCodeHash(hash(code));
        otpCode.setPurpose("LOGIN");
        otpCode.setExpiresAt(LocalDateTime.now().plusSeconds(OTP_TTL_SECONDS));
        otpCode.setConsumed(false);
        em.persist(otpCode);

        return code;
    }

    public boolean verifyAndConsume(String email, String code) {
        String hash = hash(code);
        List<OtpCode> matches = em.createNamedQuery("OtpCode.findLatestUnconsumedMatch", OtpCode.class)
                .setParameter("email", email)
                .setParameter("codeHash", hash)
                .setMaxResults(1)
                .getResultList();

        if (matches.isEmpty()) {
            return false;
        }

        OtpCode otpCode = matches.get(0);
        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        otpCode.setConsumed(true);
        return true;
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new SupplyChainSystemException("SHA-256 is not available", e);
        }
    }
}

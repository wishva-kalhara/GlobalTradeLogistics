package me.wishva.globalTradeLogistics.core.security;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.InvalidTokenException;
import me.wishva.globalTradeLogistics.core.exception.SupplyChainSystemException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

/**
 * Minimal, dependency-free HS256 JWT issuer/validator. Hand-rolled rather
 * than pulling in a third-party JWT library, since the Jakarta EE platform
 * already ships everything needed (JSON-P for the header/payload,
 * {@code javax.crypto} for the HMAC signature) — small enough to reason
 * about directly for the assignment's critical-analysis write-up.
 */
public final class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private JwtService() {
    }

    public static String issueToken(String email, Role role, String secret, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(ttlSeconds);

        String header = Json.createObjectBuilder()
                .add("alg", "HS256")
                .add("typ", "JWT")
                .build()
                .toString();

        String payload = Json.createObjectBuilder()
                .add("sub", email)
                .add("role", role.name())
                .add("iat", now.getEpochSecond())
                .add("exp", expiry.getEpochSecond())
                .build()
                .toString();

        String signingInput = encode(header) + "." + encode(payload);
        return signingInput + "." + sign(signingInput, secret);
    }

    public static CurrentPrincipal parseAndValidate(String token, String secret) throws InvalidTokenException {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is missing");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Token is malformed");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput, secret);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidTokenException("Token signature is invalid");
        }

        try {
            JsonObject payload;
            try (JsonReader reader = Json.createReader(new StringReader(decode(parts[1])))) {
                payload = reader.readObject();
            }

            long exp = payload.getJsonNumber("exp").longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new InvalidTokenException("Token has expired");
            }

            String email = payload.getString("sub");
            Role role = Role.valueOf(payload.getString("role"));
            return new CurrentPrincipal(email, role);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Token payload could not be parsed", e);
        }
    }

    private static String sign(String input, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SupplyChainSystemException("Unable to sign JWT", e);
        }
    }

    private static String encode(String value) {
        return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(URL_DECODER.decode(value), StandardCharsets.UTF_8);
    }
}

package me.wishva.globalTradeLogistics.core.security;

import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests";

    @Test
    void issueTokenThenParseAndValidate_roundTripsTheOriginalPrincipal() throws InvalidTokenException {
        String token = JwtService.issueToken("admin@example.com", Role.ADMIN, SECRET, 3600);

        CurrentPrincipal principal = JwtService.parseAndValidate(token, SECRET);

        assertEquals("admin@example.com", principal.getEmail());
        assertEquals(Role.ADMIN, principal.getRole());
    }

    @Test
    void parseAndValidate_missingOrMalformedToken_throwsInvalidTokenException() {
        assertThrows(InvalidTokenException.class, () -> JwtService.parseAndValidate(null, SECRET));
        assertThrows(InvalidTokenException.class, () -> JwtService.parseAndValidate("", SECRET));
        assertThrows(InvalidTokenException.class, () -> JwtService.parseAndValidate("not-a-jwt", SECRET));
    }

    @Test
    void parseAndValidate_wrongSigningSecret_throwsInvalidTokenException() {
        String token = JwtService.issueToken("vendor@example.com", Role.VENDOR_REP, SECRET, 3600);

        assertThrows(InvalidTokenException.class, () -> JwtService.parseAndValidate(token, "a-different-secret"));
    }

    @Test
    void parseAndValidate_expiredToken_throwsInvalidTokenException() {
        String token = JwtService.issueToken("customer@example.com", Role.CUSTOMER, SECRET, -1);

        assertThrows(InvalidTokenException.class, () -> JwtService.parseAndValidate(token, SECRET));
    }
}

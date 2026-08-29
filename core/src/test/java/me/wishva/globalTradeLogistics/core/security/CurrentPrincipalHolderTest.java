package me.wishva.globalTradeLogistics.core.security;

import me.wishva.globalTradeLogistics.core.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentPrincipalHolderTest {

    @AfterEach
    void clearHolder() {
        CurrentPrincipalHolder.clear();
    }

    @Test
    void setThenGet_returnsTheSamePrincipal() {
        CurrentPrincipal principal = new CurrentPrincipal("coordinator@example.com", Role.COORDINATOR);

        CurrentPrincipalHolder.set(principal);

        assertEquals(principal, CurrentPrincipalHolder.get());
    }

    @Test
    void clear_removesThePreviouslySetPrincipal() {
        CurrentPrincipalHolder.set(new CurrentPrincipal("agent@example.com", Role.CUSTOMS_AGENT));

        CurrentPrincipalHolder.clear();

        assertNull(CurrentPrincipalHolder.get());
    }
}

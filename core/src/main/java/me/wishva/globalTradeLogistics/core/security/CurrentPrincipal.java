package me.wishva.globalTradeLogistics.core.security;

import me.wishva.globalTradeLogistics.core.enums.Role;

import java.io.Serializable;

/**
 * The identity resolved from a validated JWT for the lifetime of one
 * request. Not a JPA entity, not persisted — purely in-memory auth context.
 */
public final class CurrentPrincipal implements Serializable {

    private final String email;
    private final Role role;

    public CurrentPrincipal(String email, Role role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}

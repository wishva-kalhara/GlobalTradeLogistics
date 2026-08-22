package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.Role;

import java.io.Serializable;

/**
 * Returned by {@code IUsersService.verifyOtp} — the JWT plus the resolved
 * principal, ready to hand back to the caller as the login response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult implements Serializable {

    private String token;
    private String email;
    private Role role;
}

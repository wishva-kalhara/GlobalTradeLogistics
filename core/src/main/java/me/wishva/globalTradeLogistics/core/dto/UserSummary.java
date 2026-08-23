package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.Role;

import java.io.Serializable;

/**
 * Row shape for the {@code /app/app-user-management.jsp} table — returned
 * by {@code IUserAdminService.listUsers()}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary implements Serializable {

    private String email;
    private String fullName;
    private Role role;
}

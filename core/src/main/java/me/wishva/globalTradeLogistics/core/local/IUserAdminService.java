package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

/**
 * Admin-only onboarding operations. Every method requires
 * {@code Role.ADMIN}, enforced by {@code @RequiresRole} on the
 * implementation (not container-managed security).
 */
@Local
public interface IUserAdminService {

    void createUser(String email, String fullName, Role role) throws UnauthorizedAccessException;

    void registerCustomer(String email, String fullName, String mobile1, String address, String country, String regionKey)
            throws UnauthorizedAccessException;

    void registerSupplier(String email, String fullName, String mobile1, String address, String country)
            throws UnauthorizedAccessException;
}

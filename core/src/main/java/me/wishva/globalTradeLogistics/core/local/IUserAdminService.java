package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.SupplierSummary;
import me.wishva.globalTradeLogistics.core.dto.UserSummary;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

import java.util.List;

/**
 * Admin-only onboarding operations. Every method requires
 * {@code Role.ADMIN}, enforced by {@code @RequiresRole} on the
 * implementation (not container-managed security).
 */
@Local
public interface IUserAdminService {

    void createUser(String email, String fullName, Role role) throws UnauthorizedAccessException;

    void registerCustomer(String email, String fullName, String mobile1, String address, String country)
            throws UnauthorizedAccessException;

    void registerSupplier(String email, String fullName, String mobile1, String address, String country)
            throws UnauthorizedAccessException;

    List<UserSummary> listUsers() throws UnauthorizedAccessException;

    /**
     * Suppliers to populate the create-purchase-order page's dropdown
     * (roles: ADMIN, COORDINATOR). Only suppliers who've completed their
     * profile (a non-blank {@code fullName}) are included — a supplier
     * that's only ever self-signed-up has no name to show, which reads as a
     * bare, user-like email address rather than a real seller.
     */
    List<SupplierSummary> listSuppliers() throws UnauthorizedAccessException;
}

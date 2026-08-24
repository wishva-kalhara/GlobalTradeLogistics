package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.CustomerProfileSummary;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

/**
 * Self-service profile completion (shipping/contact details) for the
 * currently authenticated principal — resolved from {@code CurrentPrincipalHolder},
 * never from a client-supplied identifier, so one customer can never edit
 * another's profile.
 */
@Local
public interface IProfileService {

    /** Lets the profile-completion page pre-fill the form with whatever's already saved. */
    CustomerProfileSummary getCustomerProfile() throws UnauthorizedAccessException, UnknownPrincipalException;

    void updateCustomerProfile(String fullName, String mobile1, String mobile2, String address, String country)
            throws UnauthorizedAccessException, UnknownPrincipalException;

    void updateSupplierProfile(String fullName, String mobile1, String mobile2, String address, String country)
            throws UnauthorizedAccessException, UnknownPrincipalException;
}

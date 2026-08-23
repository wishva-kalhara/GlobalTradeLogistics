package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.exception.EmailAlreadyRegisteredException;

/**
 * Public self-service signup for customers and suppliers — deliberately no
 * {@code @RequiresRole}, since these are called before the caller has any
 * identity at all. Both methods auto-login on success (return a JWT
 * immediately) so the browser can go straight from sign-up into the
 * profile-completion page without a separate OTP round-trip.
 */
@Local
public interface IRegistrationService {

    AuthResult signUpCustomer(String email, String fullName, String mobile1, String address, String country, String regionKey)
            throws EmailAlreadyRegisteredException;

    AuthResult signUpSupplier(String email, String fullName, String mobile1, String address, String country)
            throws EmailAlreadyRegisteredException;
}

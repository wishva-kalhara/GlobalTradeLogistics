package me.wishva.globalTradeLogistics.core.remote;

import jakarta.ejb.Remote;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.exception.OtpExpiredOrInvalidException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

@Remote
public interface IUsersService {

    /**
     * Generates and emails a one-time login code for {@code email}.
     * Publishes an {@code EmailNotification} (type=OTP_AUTHENTICATION).
     */
    void requestOtp(String email) throws UnknownPrincipalException;

    /**
     * Verifies a previously requested OTP and, on success, resolves the
     * caller's role and issues a signed JWT.
     */
    AuthResult verifyOtp(String email, String code) throws OtpExpiredOrInvalidException, UnknownPrincipalException;
}

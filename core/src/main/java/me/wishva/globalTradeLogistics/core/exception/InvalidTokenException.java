package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown by {@link me.wishva.globalTradeLogistics.core.security.JwtService}
 * when a bearer token is missing, malformed, expired, or fails signature
 * verification. Maps to HTTP 401 at the gateway's auth filter.
 */
public class InvalidTokenException extends SupplyChainApplicationException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

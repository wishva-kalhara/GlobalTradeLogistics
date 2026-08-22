package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown by {@link me.wishva.globalTradeLogistics.core.interceptor.RequiresRoleInterceptor}
 * when the current principal's role isn't permitted to call a
 * {@code @RequiresRole}-annotated method. Maps to HTTP 403 at the gateway.
 */
public class UnauthorizedAccessException extends SupplyChainApplicationException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

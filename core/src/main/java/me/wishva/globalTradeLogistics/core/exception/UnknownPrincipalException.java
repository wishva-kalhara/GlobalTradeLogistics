package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown when an email doesn't resolve to an active row in {@code users},
 * {@code customers}, or {@code suppliers}. Maps to HTTP 404 at the gateway.
 */
public class UnknownPrincipalException extends SupplyChainApplicationException {

    public UnknownPrincipalException(String message) {
        super(message);
    }
}

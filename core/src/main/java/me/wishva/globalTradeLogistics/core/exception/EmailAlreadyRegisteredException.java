package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown by self-service signup when the email already has a customer or
 * supplier account. Maps to HTTP 409 at the gateway.
 */
public class EmailAlreadyRegisteredException extends SupplyChainApplicationException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}

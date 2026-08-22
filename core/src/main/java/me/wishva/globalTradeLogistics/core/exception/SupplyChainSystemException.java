package me.wishva.globalTradeLogistics.core.exception;

/**
 * Base type for unexpected/system failures (unchecked — always rolls back a
 * container-managed transaction, per EJB exception-handling semantics).
 */
public class SupplyChainSystemException extends RuntimeException {

    public SupplyChainSystemException(String message) {
        super(message);
    }

    public SupplyChainSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}

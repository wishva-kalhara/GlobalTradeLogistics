package me.wishva.globalTradeLogistics.core.exception;

/**
 * Base type for recoverable, expected business conditions (checked — the
 * caller is meant to handle these, e.g. map them to a 4xx HTTP response).
 */
public class SupplyChainApplicationException extends Exception {

    public SupplyChainApplicationException(String message) {
        super(message);
    }

    public SupplyChainApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

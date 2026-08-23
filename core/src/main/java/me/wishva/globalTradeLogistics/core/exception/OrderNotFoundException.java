package me.wishva.globalTradeLogistics.core.exception;

/**
 * Thrown both when an order id doesn't exist and when it exists but belongs
 * to a different customer — deliberately not distinguished from the
 * caller's perspective, so a customer probing other customers' order ids
 * can't tell the difference between "no such order" and "not yours".
 */
public class OrderNotFoundException extends SupplyChainApplicationException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}

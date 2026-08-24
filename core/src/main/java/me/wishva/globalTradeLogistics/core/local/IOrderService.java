package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.OrderItemRequest;
import me.wishva.globalTradeLogistics.core.dto.OrderSummary;
import me.wishva.globalTradeLogistics.core.dto.SalesSummary;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.exception.OrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;

import java.util.List;

/**
 * Order placement and retrieval for the currently authenticated customer —
 * which customer is always resolved from {@code CurrentPrincipalHolder},
 * never a client-supplied id, same self-scoping pattern as
 * {@link IProfileService}.
 */
@Local
public interface IOrderService {

    OrderSummary placeOrder(List<OrderItemRequest> items)
            throws InsufficientInventoryException, UnauthorizedAccessException, UnknownPrincipalException;

    OrderSummary getOrder(Integer orderId)
            throws OrderNotFoundException, UnauthorizedAccessException, UnknownPrincipalException;

    List<OrderSummary> listOrdersForCurrentCustomer() throws UnauthorizedAccessException, UnknownPrincipalException;

    /** Store-wide sales aggregate for the staff dashboard (roles: ADMIN, COORDINATOR). */
    SalesSummary getSalesSummary() throws UnauthorizedAccessException;
}

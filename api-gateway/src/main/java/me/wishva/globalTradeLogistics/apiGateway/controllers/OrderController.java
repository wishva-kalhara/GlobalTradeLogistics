package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.dto.OrderItemBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.PlaceOrderBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.OrderItemRequest;
import me.wishva.globalTradeLogistics.core.dto.OrderSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.exception.OrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IOrderService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Place/view orders for the currently authenticated customer — a pure
 * pass-through to {@code order-svc}'s {@code IOrderService}, per api-gateway's
 * "REST façade, no new business logic" convention.
 */
@Path("/orders")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderController {

    @EJB
    private IOrderService orderService;

    @Inject
    private Event<LogEvent> logEvent;

    @POST
    public OrderSummary placeOrder(PlaceOrderBody body)
            throws InsufficientInventoryException, UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /orders"));
        if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN, "placeOrder: at least one order item is required"));
            throw new BadRequestException("At least one order item is required");
        }

        List<OrderItemRequest> items = new ArrayList<>();
        for (OrderItemBody item : body.getItems()) {
            if (item.getProductId() == null || item.getQty() == null || item.getQty() <= 0) {
                logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN, "placeOrder: each item requires a productId and a positive qty"));
                throw new BadRequestException("Each item requires a productId and a positive qty");
            }
            items.add(new OrderItemRequest(item.getProductId(), item.getQty()));
        }

        return orderService.placeOrder(items);
    }

    @GET
    public List<OrderSummary> listOrders() throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /orders"));
        return orderService.listOrdersForCurrentCustomer();
    }

    @GET
    @Path("/{orderId}")
    public OrderSummary getOrder(@PathParam("orderId") Integer orderId)
            throws OrderNotFoundException, UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /orders/" + orderId));
        return orderService.getOrder(orderId);
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "orders";
    }
}

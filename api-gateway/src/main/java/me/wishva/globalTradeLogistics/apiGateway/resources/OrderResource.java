package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
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
import me.wishva.globalTradeLogistics.core.dto.OrderItemRequest;
import me.wishva.globalTradeLogistics.core.dto.OrderSummary;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;
import me.wishva.globalTradeLogistics.core.exception.OrderNotFoundException;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IOrderService;

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
public class OrderResource {

    @EJB
    private IOrderService orderService;

    @POST
    public OrderSummary placeOrder(PlaceOrderBody body)
            throws InsufficientInventoryException, UnauthorizedAccessException, UnknownPrincipalException {
        if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
            throw new BadRequestException("At least one order item is required");
        }

        List<OrderItemRequest> items = new ArrayList<>();
        for (OrderItemBody item : body.getItems()) {
            if (item.getProductId() == null || item.getQty() == null || item.getQty() <= 0) {
                throw new BadRequestException("Each item requires a productId and a positive qty");
            }
            items.add(new OrderItemRequest(item.getProductId(), item.getQty()));
        }

        return orderService.placeOrder(items);
    }

    @GET
    public List<OrderSummary> listOrders() throws UnauthorizedAccessException, UnknownPrincipalException {
        return orderService.listOrdersForCurrentCustomer();
    }

    @GET
    @Path("/{orderId}")
    public OrderSummary getOrder(@PathParam("orderId") Integer orderId)
            throws OrderNotFoundException, UnauthorizedAccessException, UnknownPrincipalException {
        return orderService.getOrder(orderId);
    }
}

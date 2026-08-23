package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.InsufficientInventoryException;

import java.util.Map;

@Provider
public class InsufficientInventoryExceptionMapper implements ExceptionMapper<InsufficientInventoryException> {

    @Override
    public Response toResponse(InsufficientInventoryException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

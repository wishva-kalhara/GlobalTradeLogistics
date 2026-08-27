package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.InvalidShipmentStateException;

import java.util.Map;

@Provider
public class InvalidShipmentStateExceptionMapper implements ExceptionMapper<InvalidShipmentStateException> {

    @Override
    public Response toResponse(InvalidShipmentStateException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

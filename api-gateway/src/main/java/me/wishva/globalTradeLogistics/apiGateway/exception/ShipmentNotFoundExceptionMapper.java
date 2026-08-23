package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.ShipmentNotFoundException;

import java.util.Map;

@Provider
public class ShipmentNotFoundExceptionMapper implements ExceptionMapper<ShipmentNotFoundException> {

    @Override
    public Response toResponse(ShipmentNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

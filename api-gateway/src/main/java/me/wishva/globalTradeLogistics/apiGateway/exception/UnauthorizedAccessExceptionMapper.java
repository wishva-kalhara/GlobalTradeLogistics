package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;

import java.util.Map;

@Provider
public class UnauthorizedAccessExceptionMapper implements ExceptionMapper<UnauthorizedAccessException> {

    @Override
    public Response toResponse(UnauthorizedAccessException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

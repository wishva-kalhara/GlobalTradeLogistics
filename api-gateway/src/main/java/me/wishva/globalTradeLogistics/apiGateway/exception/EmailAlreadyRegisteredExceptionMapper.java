package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.EmailAlreadyRegisteredException;

import java.util.Map;

@Provider
public class EmailAlreadyRegisteredExceptionMapper implements ExceptionMapper<EmailAlreadyRegisteredException> {

    @Override
    public Response toResponse(EmailAlreadyRegisteredException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

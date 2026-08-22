package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.OtpExpiredOrInvalidException;

import java.util.Map;

@Provider
public class OtpExpiredOrInvalidExceptionMapper implements ExceptionMapper<OtpExpiredOrInvalidException> {

    @Override
    public Response toResponse(OtpExpiredOrInvalidException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

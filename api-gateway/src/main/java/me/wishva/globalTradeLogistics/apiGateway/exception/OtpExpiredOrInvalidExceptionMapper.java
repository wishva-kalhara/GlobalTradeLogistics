package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.OtpExpiredOrInvalidException;

import java.util.Map;

@Provider
public class OtpExpiredOrInvalidExceptionMapper implements ExceptionMapper<OtpExpiredOrInvalidException> {

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public Response toResponse(OtpExpiredOrInvalidException exception) {
        logEvent.fire(new LogEvent("otp-verify", LogLevel.WARN,
                "OtpExpiredOrInvalidExceptionMapper: " + exception.getMessage()));
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}

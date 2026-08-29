package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.Map;

@Provider
public class UnknownPrincipalExceptionMapper implements ExceptionMapper<UnknownPrincipalException> {

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public Response toResponse(UnknownPrincipalException exception) {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                "UnknownPrincipalExceptionMapper: " + exception.getMessage()));
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "api-gateway";
    }
}

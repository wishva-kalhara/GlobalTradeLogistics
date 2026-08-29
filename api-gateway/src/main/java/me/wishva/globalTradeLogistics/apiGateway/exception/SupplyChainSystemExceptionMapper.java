package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.SupplyChainSystemException;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class SupplyChainSystemExceptionMapper implements ExceptionMapper<SupplyChainSystemException> {

    private static final Logger LOG = Logger.getLogger(SupplyChainSystemExceptionMapper.class.getName());

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public Response toResponse(SupplyChainSystemException exception) {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                "SupplyChainSystemExceptionMapper: " + exception.getMessage()));
        LOG.log(Level.SEVERE, "Unhandled system exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "An unexpected error occurred"))
                .build();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "api-gateway";
    }
}

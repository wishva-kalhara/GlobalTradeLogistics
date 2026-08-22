package me.wishva.globalTradeLogistics.apiGateway.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import me.wishva.globalTradeLogistics.core.exception.SupplyChainSystemException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class SupplyChainSystemExceptionMapper implements ExceptionMapper<SupplyChainSystemException> {

    private static final Logger LOG = Logger.getLogger(SupplyChainSystemExceptionMapper.class.getName());

    @Override
    public Response toResponse(SupplyChainSystemException exception) {
        LOG.log(Level.SEVERE, "Unhandled system exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "An unexpected error occurred"))
                .build();
    }
}

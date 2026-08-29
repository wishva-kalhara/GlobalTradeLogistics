package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.UpdateCustomerProfileBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.UpdateSupplierProfileBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.ProfileSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IProfileService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

/**
 * Profile completion for the currently authenticated customer/supplier —
 * {@code @Secured} (1.9) resolves the caller; {@code @RequiresRole} (1.10)
 * on the EJB decides which of the two methods a given JWT may call.
 */
@Path("/me")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileController {

    @EJB
    private IProfileService profileService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    @Path("/customer")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileSummary getCustomerProfile() throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /me/customer"));
        return profileService.getCustomerProfile();
    }

    @PUT
    @Path("/customer")
    public Response updateCustomerProfile(UpdateCustomerProfileBody body) throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "PUT /me/customer"));
        profileService.updateCustomerProfile(
                body.getFullName(), body.getMobile1(), body.getMobile2(), body.getAddress(), body.getCountry());
        return Response.noContent().build();
    }

    @GET
    @Path("/supplier")
    @Produces(MediaType.APPLICATION_JSON)
    public ProfileSummary getSupplierProfile() throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /me/supplier"));
        return profileService.getSupplierProfile();
    }

    @PUT
    @Path("/supplier")
    public Response updateSupplierProfile(UpdateSupplierProfileBody body) throws UnauthorizedAccessException, UnknownPrincipalException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "PUT /me/supplier"));
        profileService.updateSupplierProfile(
                body.getFullName(), body.getMobile1(), body.getMobile2(), body.getAddress(), body.getCountry());
        return Response.noContent().build();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "profile";
    }
}

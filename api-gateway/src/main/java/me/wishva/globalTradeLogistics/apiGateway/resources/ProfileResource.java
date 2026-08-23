package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.UpdateCustomerProfileBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.UpdateSupplierProfileBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.local.IProfileService;

/**
 * Profile completion for the currently authenticated customer/supplier —
 * {@code @Secured} (1.9) resolves the caller; {@code @RequiresRole} (1.10)
 * on the EJB decides which of the two methods a given JWT may call.
 */
@Path("/me")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @EJB
    private IProfileService profileService;

    @PUT
    @Path("/customer")
    public Response updateCustomerProfile(UpdateCustomerProfileBody body) throws UnauthorizedAccessException, UnknownPrincipalException {
        profileService.updateCustomerProfile(
                body.getMobile1(), body.getMobile2(), body.getAddress(), body.getCountry(), body.getRegionKey());
        return Response.noContent().build();
    }

    @PUT
    @Path("/supplier")
    public Response updateSupplierProfile(UpdateSupplierProfileBody body) throws UnauthorizedAccessException, UnknownPrincipalException {
        profileService.updateSupplierProfile(body.getMobile1(), body.getMobile2(), body.getAddress(), body.getCountry());
        return Response.noContent().build();
    }
}

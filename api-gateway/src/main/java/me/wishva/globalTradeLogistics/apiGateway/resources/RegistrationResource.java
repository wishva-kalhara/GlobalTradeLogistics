package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.dto.AuthResponseBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterCustomerBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterSupplierBody;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.exception.EmailAlreadyRegisteredException;
import me.wishva.globalTradeLogistics.core.local.IRegistrationService;

/**
 * Public "Create Account" self-service signup for the customer and seller
 * frontends — unprotected (no {@code @Secured}), since the caller has no
 * identity yet. Auto-logs in on success so the browser can go straight to
 * the profile-completion page.
 */
@Path("/auth/signup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    @EJB
    private IRegistrationService registrationService;

    @POST
    @Path("/customer")
    public AuthResponseBody signUpCustomer(RegisterCustomerBody body) throws EmailAlreadyRegisteredException {
        if (body == null || body.getEmail() == null || body.getFullName() == null) {
            throw new BadRequestException("email and fullName are required");
        }

        AuthResult result = registrationService.signUpCustomer(
                body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }

    @POST
    @Path("/supplier")
    public AuthResponseBody signUpSupplier(RegisterSupplierBody body) throws EmailAlreadyRegisteredException {
        if (body == null || body.getEmail() == null || body.getFullName() == null
                || body.getMobile1() == null || body.getAddress() == null || body.getCountry() == null) {
            throw new BadRequestException("email, fullName, mobile1, address and country are required");
        }

        AuthResult result = registrationService.signUpSupplier(
                body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }
}

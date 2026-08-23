package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.apiGateway.dto.AuthResponseBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.SignUpCustomerBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.SignUpSupplierBody;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.exception.EmailAlreadyRegisteredException;
import me.wishva.globalTradeLogistics.core.local.IRegistrationService;

/**
 * Public "Create Account" self-service signup for the customer and seller
 * frontends — unprotected (no {@code @Secured}), since the caller has no
 * identity yet. Deliberately minimal (email + country only); auto-logs in
 * on success so the browser can go straight to the profile-completion page,
 * where the rest of the details (full name, mobile, address) are collected.
 */
@Path("/auth/signup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    @EJB
    private IRegistrationService registrationService;

    @POST
    @Path("/customer")
    public AuthResponseBody signUpCustomer(SignUpCustomerBody body) throws EmailAlreadyRegisteredException {
        if (body == null || body.getEmail() == null || body.getCountry() == null) {
            throw new BadRequestException("email and country are required");
        }

        AuthResult result = registrationService.signUpCustomer(body.getEmail(), body.getCountry());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }

    @POST
    @Path("/supplier")
    public AuthResponseBody signUpSupplier(SignUpSupplierBody body) throws EmailAlreadyRegisteredException {
        if (body == null || body.getEmail() == null || body.getCountry() == null) {
            throw new BadRequestException("email and country are required");
        }

        AuthResult result = registrationService.signUpSupplier(body.getEmail(), body.getCountry());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }
}

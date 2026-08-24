package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.AuthResponseBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.OtpRequestBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.OtpVerifyBody;
import me.wishva.globalTradeLogistics.core.dto.AuthResult;
import me.wishva.globalTradeLogistics.core.exception.OtpExpiredOrInvalidException;
import me.wishva.globalTradeLogistics.core.exception.UnknownPrincipalException;
import me.wishva.globalTradeLogistics.core.remote.IUsersService;

import java.util.Map;

/**
 * Flows: OTP Request (1.1-1.3) and OTP Verify &amp; JWT Issuance (1.4-1.8).
 * Unprotected — no {@code @Secured} — since these ARE the login flow.
 */
@Path("/auth")
public class AuthController {

    @EJB
    private IUsersService usersService;

    @POST
    @Path("/otp/request")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestOtp(OtpRequestBody body) throws UnknownPrincipalException {
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "email is required")).build();
        }
        usersService.requestOtp(body.getEmail());
        return Response.ok(Map.of("status", "otp_sent")).build();
    }

    @POST
    @Path("/otp/verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuthResponseBody verifyOtp(OtpVerifyBody body) throws OtpExpiredOrInvalidException, UnknownPrincipalException {
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()
                || body.getCode() == null || body.getCode().isBlank()) {
            throw new BadRequestException("email and code are required");
        }
        AuthResult result = usersService.verifyOtp(body.getEmail(), body.getCode());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }
}

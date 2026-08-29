package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
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
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
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

    @Inject
    private Event<LogEvent> logEvent;

    @POST
    @Path("/otp/request")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestOtp(OtpRequestBody body) throws UnknownPrincipalException {
        String email = body != null ? body.getEmail() : null;
        logEvent.fire(new LogEvent(email != null ? email : "otp-request", LogLevel.TRACE, "POST /auth/otp/request"));
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()) {
            logEvent.fire(new LogEvent("otp-request", LogLevel.WARN, "requestOtp: missing email in request body"));
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
        String email = body != null ? body.getEmail() : null;
        logEvent.fire(new LogEvent(email != null ? email : "otp-verify", LogLevel.TRACE, "POST /auth/otp/verify"));
        if (body == null || body.getEmail() == null || body.getEmail().isBlank()
                || body.getCode() == null || body.getCode().isBlank()) {
            logEvent.fire(new LogEvent("otp-verify", LogLevel.WARN, "verifyOtp: missing email or code in request body"));
            throw new BadRequestException("email and code are required");
        }
        AuthResult result = usersService.verifyOtp(body.getEmail(), body.getCode());
        return new AuthResponseBody(result.getToken(), result.getEmail(), result.getRole().name());
    }
}

package me.wishva.globalTradeLogistics.apiGateway.resources;

import jakarta.ejb.EJB;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreateUserBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterCustomerBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterSupplierBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.UserSummary;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IUserAdminService;

import java.util.List;

/**
 * Flows: Admin Provisions a Staff User (1.12), Customer Onboarding (1.13),
 * Supplier Onboarding (1.14). {@code @Secured} at class level binds every
 * method here to {@link me.wishva.globalTradeLogistics.apiGateway.security.JwtAuthFilter}
 * (1.9); {@code @RequiresRole(ADMIN)} (1.10) at the EJB layer does the
 * actual role check.
 */
@Path("/admin")
@Secured
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @EJB
    private IUserAdminService userAdminService;

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSummary> listUsers() throws UnauthorizedAccessException {
        return userAdminService.listUsers();
    }

    @POST
    @Path("/users")
    public Response createUser(CreateUserBody body) throws UnauthorizedAccessException {
        if (body == null || body.getEmail() == null || body.getFullName() == null || body.getRole() == null) {
            throw new BadRequestException("email, fullName and role are required");
        }

        Role role;
        try {
            role = Role.valueOf(body.getRole());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown role: " + body.getRole());
        }

        userAdminService.createUser(body.getEmail(), body.getFullName(), role);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/customers")
    public Response registerCustomer(RegisterCustomerBody body) throws UnauthorizedAccessException {
        if (body == null || body.getEmail() == null || body.getFullName() == null) {
            throw new BadRequestException("email and fullName are required");
        }

        userAdminService.registerCustomer(
                body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/suppliers")
    public Response registerSupplier(RegisterSupplierBody body) throws UnauthorizedAccessException {
        if (body == null || body.getEmail() == null || body.getFullName() == null
                || body.getMobile1() == null || body.getAddress() == null || body.getCountry() == null) {
            throw new BadRequestException("email, fullName, mobile1, address and country are required");
        }

        userAdminService.registerSupplier(body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return Response.status(Response.Status.CREATED).build();
    }
}

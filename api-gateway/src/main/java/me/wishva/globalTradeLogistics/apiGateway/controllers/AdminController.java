package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.wishva.globalTradeLogistics.apiGateway.dto.CreateUserBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterCustomerBody;
import me.wishva.globalTradeLogistics.apiGateway.dto.RegisterSupplierBody;
import me.wishva.globalTradeLogistics.apiGateway.security.Secured;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.dto.SalesSummary;
import me.wishva.globalTradeLogistics.core.dto.SupplierSummary;
import me.wishva.globalTradeLogistics.core.dto.UserSummary;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.enums.Role;
import me.wishva.globalTradeLogistics.core.exception.UnauthorizedAccessException;
import me.wishva.globalTradeLogistics.core.local.IOrderService;
import me.wishva.globalTradeLogistics.core.local.IUserAdminService;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipal;
import me.wishva.globalTradeLogistics.core.security.CurrentPrincipalHolder;

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
public class AdminController {

    @EJB
    private IUserAdminService userAdminService;

    @EJB
    private IOrderService orderService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    @Path("/sales-summary")
    @Produces(MediaType.APPLICATION_JSON)
    public SalesSummary getSalesSummary() throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /admin/sales-summary"));
        return orderService.getSalesSummary();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSummary> listUsers() throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "GET /admin/users"));
        return userAdminService.listUsers();
    }

    @POST
    @Path("/users")
    public Response createUser(CreateUserBody body) throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /admin/users"));
        if (body == null || body.getEmail() == null || body.getFullName() == null || body.getRole() == null) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN, "createUser: email, fullName and role are required"));
            throw new BadRequestException("email, fullName and role are required");
        }

        Role role;
        try {
            role = Role.valueOf(body.getRole());
        } catch (IllegalArgumentException e) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN, "createUser: unknown role " + body.getRole()));
            throw new BadRequestException("Unknown role: " + body.getRole());
        }

        userAdminService.createUser(body.getEmail(), body.getFullName(), role);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/customers")
    public Response registerCustomer(RegisterCustomerBody body) throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /admin/customers"));
        if (body == null || body.getEmail() == null || body.getFullName() == null) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN, "registerCustomer: email and fullName are required"));
            throw new BadRequestException("email and fullName are required");
        }

        userAdminService.registerCustomer(
                body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/suppliers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SupplierSummary> listSuppliers(@QueryParam("productId") Integer productId) throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE,
                productId != null ? "GET /admin/suppliers?productId=" + productId : "GET /admin/suppliers"));
        return productId != null ? userAdminService.listSuppliersForProduct(productId) : userAdminService.listSuppliers();
    }

    @POST
    @Path("/suppliers")
    public Response registerSupplier(RegisterSupplierBody body) throws UnauthorizedAccessException {
        logEvent.fire(new LogEvent(correlationKey(), LogLevel.TRACE, "POST /admin/suppliers"));
        if (body == null || body.getEmail() == null || body.getFullName() == null
                || body.getMobile1() == null || body.getAddress() == null || body.getCountry() == null) {
            logEvent.fire(new LogEvent(correlationKey(), LogLevel.WARN,
                    "registerSupplier: email, fullName, mobile1, address and country are required"));
            throw new BadRequestException("email, fullName, mobile1, address and country are required");
        }

        userAdminService.registerSupplier(body.getEmail(), body.getFullName(), body.getMobile1(), body.getAddress(), body.getCountry());
        return Response.status(Response.Status.CREATED).build();
    }

    private static String correlationKey() {
        CurrentPrincipal principal = CurrentPrincipalHolder.get();
        return principal != null ? principal.getEmail() : "admin";
    }
}

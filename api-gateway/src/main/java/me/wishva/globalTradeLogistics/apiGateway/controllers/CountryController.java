package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.core.dto.CountrySummary;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.local.ICountryService;

import java.util.List;

/**
 * Unprotected (no {@code @Secured}) — the sign-up pages need this before
 * the caller has any identity.
 */
@Path("/countries")
@Produces(MediaType.APPLICATION_JSON)
public class CountryController {

    @EJB
    private ICountryService countryService;

    @Inject
    private Event<LogEvent> logEvent;

    @GET
    public List<CountrySummary> listCountries() {
        logEvent.fire(new LogEvent("countries-list", LogLevel.TRACE, "GET /countries"));
        return countryService.listCountries();
    }
}

package me.wishva.globalTradeLogistics.apiGateway.controllers;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import me.wishva.globalTradeLogistics.core.dto.CountrySummary;
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

    @GET
    public List<CountrySummary> listCountries() {
        return countryService.listCountries();
    }
}

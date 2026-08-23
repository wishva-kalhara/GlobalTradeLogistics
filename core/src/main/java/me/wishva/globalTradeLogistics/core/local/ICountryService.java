package me.wishva.globalTradeLogistics.core.local;

import jakarta.ejb.Local;
import me.wishva.globalTradeLogistics.core.dto.CountrySummary;

import java.util.List;

/**
 * Read-only country reference data for the sign-up/profile `<select>`
 * dropdowns. Deliberately unguarded — needed before a caller has any
 * identity (the sign-up page itself).
 */
@Local
public interface ICountryService {

    List<CountrySummary> listCountries();
}

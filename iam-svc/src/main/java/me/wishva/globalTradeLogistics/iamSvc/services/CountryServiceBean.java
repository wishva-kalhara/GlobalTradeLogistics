package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.CountrySummary;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.local.ICountryService;
import me.wishva.globalTradeLogistics.core.model.Country;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class CountryServiceBean implements ICountryService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public List<CountrySummary> listCountries() {
        logEvent.fire(new LogEvent("countries-list", LogLevel.TRACE, "listCountries: loading all countries"));
        return em.createNamedQuery("Country.findAllOrderByName", Country.class)
                .getResultList()
                .stream()
                .map(c -> new CountrySummary(c.getCode(), c.getName()))
                .collect(Collectors.toList());
    }
}

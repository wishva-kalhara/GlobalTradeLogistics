package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.CountrySummary;
import me.wishva.globalTradeLogistics.core.local.ICountryService;
import me.wishva.globalTradeLogistics.core.model.Country;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class CountryServiceBean implements ICountryService {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Override
    public List<CountrySummary> listCountries() {
        return em.createNamedQuery("Country.findAllOrderByName", Country.class)
                .getResultList()
                .stream()
                .map(c -> new CountrySummary(c.getCode(), c.getName()))
                .collect(Collectors.toList());
    }
}

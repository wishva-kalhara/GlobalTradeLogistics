package me.wishva.globalTradeLogistics.iamSvc.services;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.model.Country;

/**
 * Seeds the {@code countries} reference table on deploy, idempotently
 * (only inserts if the table is empty) — since Hibernate's
 * {@code hbm2ddl.auto=update} creates the table but never seeds data, and
 * we're avoiding hand-written SQL entirely for our own additive schema.
 */
@Singleton
@Startup
public class CountrySeedBean {

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    private static final String[][] COUNTRIES = {
            {"AU", "Australia"}, {"BD", "Bangladesh"}, {"BE", "Belgium"}, {"BR", "Brazil"},
            {"CA", "Canada"}, {"CH", "Switzerland"}, {"CN", "China"}, {"DE", "Germany"},
            {"DK", "Denmark"}, {"EG", "Egypt"}, {"ES", "Spain"}, {"FI", "Finland"},
            {"FR", "France"}, {"GB", "United Kingdom"}, {"HK", "Hong Kong"}, {"ID", "Indonesia"},
            {"IE", "Ireland"}, {"IN", "India"}, {"IT", "Italy"}, {"JP", "Japan"},
            {"KE", "Kenya"}, {"KR", "South Korea"}, {"LK", "Sri Lanka"}, {"MV", "Maldives"},
            {"MX", "Mexico"}, {"MY", "Malaysia"}, {"NG", "Nigeria"}, {"NL", "Netherlands"},
            {"NO", "Norway"}, {"NZ", "New Zealand"}, {"PH", "Philippines"}, {"PK", "Pakistan"},
            {"PL", "Poland"}, {"PT", "Portugal"}, {"QA", "Qatar"}, {"RU", "Russia"},
            {"SA", "Saudi Arabia"}, {"SE", "Sweden"}, {"SG", "Singapore"}, {"TH", "Thailand"},
            {"TR", "Turkey"}, {"TW", "Taiwan"}, {"AE", "United Arab Emirates"}, {"US", "United States"},
            {"VN", "Vietnam"}, {"ZA", "South Africa"},
    };

    @PostConstruct
    void seed() {
        long existing = em.createQuery("SELECT COUNT(c) FROM Country c", Long.class).getSingleResult();
        if (existing > 0) {
            return;
        }
        for (String[] country : COUNTRIES) {
            em.persist(new Country(country[0], country[1]));
        }
    }
}

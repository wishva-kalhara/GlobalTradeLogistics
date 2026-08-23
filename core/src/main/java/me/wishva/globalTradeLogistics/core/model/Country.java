package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Additive reference table for the country `<select>` dropdowns on the
 * customer/seller sign-up and profile pages. Schema-owned by Hibernate
 * ({@code hibernate.hbm2ddl.auto=update} creates {@code countries} from
 * this entity) and seeded idempotently at deploy time by
 * {@code CountrySeedBean} — no hand-written SQL.
 * <p>
 * {@code customers.country}/{@code suppliers.country} stay the plain
 * free-text VARCHAR columns they already are — the `<select>` just submits
 * this table's {@code name} as that string, so no FK/schema change was
 * needed on either existing table.
 */
@Entity
@Table(name = "countries")
@NamedQuery(
        name = "Country.findAllOrderByName",
        query = "SELECT c FROM Country c ORDER BY c.name")
@Getter
@Setter
@NoArgsConstructor
public class Country {

    @Id
    @Column(name = "code", length = 2)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    public Country(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

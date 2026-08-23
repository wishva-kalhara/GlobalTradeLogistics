package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code suppliers} table (schema.postgres.sql). Note
 * {@code is_active} is a legacy VARCHAR("true"/"false"), not a boolean —
 * kept as-is per the "don't redesign the existing schema" constraint.
 * <p>
 * {@code email}'s {@code unique=true} only affects DDL Hibernate would
 * generate for a brand-new table — {@code hibernate.hbm2ddl.auto=update}
 * does NOT retrofit constraints onto an already-existing table/column
 * (verified: it adds missing tables/columns only). Since we're avoiding
 * hand-written migration SQL entirely, email-uniqueness for this table is
 * enforced at the application layer only (see
 * {@code RegistrationServiceBean}'s pre-insert {@code countByEmail} check)
 * — a known, accepted small TOCTOU gap at this project's scope.
 */
@Entity
@Table(name = "suppliers")
@NamedQueries({
        @NamedQuery(
                name = "Supplier.findActiveByEmail",
                query = "SELECT s FROM Supplier s WHERE s.email = :email AND s.isActive = 'true'"),
        @NamedQuery(
                name = "Supplier.countByEmail",
                query = "SELECT COUNT(s) FROM Supplier s WHERE s.email = :email")
})
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "is_active", nullable = false)
    private String isActive = "true";

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_1", nullable = false)
    private String mobile1;

    @Column(name = "mobile_2")
    private String mobile2;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "country", nullable = false)
    private String country;
}

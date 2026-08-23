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
 * Maps the existing {@code customers} table (schema.postgres.sql). Note
 * {@code is_active} is a legacy VARCHAR("true"/"false"), not a boolean —
 * kept as-is per the "don't redesign the existing schema" constraint.
 */
@Entity
@Table(name = "customers")
@NamedQueries({
        @NamedQuery(
                name = "Customer.findActiveByEmail",
                query = "SELECT c FROM Customer c WHERE c.email = :email AND c.isActive = 'true'"),
        @NamedQuery(
                name = "Customer.countByEmail",
                query = "SELECT COUNT(c) FROM Customer c WHERE c.email = :email")
})
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "is_active", nullable = false)
    private String isActive = "true";

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_1")
    private String mobile1;

    @Column(name = "mobile_2")
    private String mobile2;

    @Column(name = "address")
    private String address;

    @Column(name = "country")
    private String country;

    @Column(name = "regions_region_key", nullable = false)
    private String regionKey;
}

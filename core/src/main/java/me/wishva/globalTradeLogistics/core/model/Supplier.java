package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code suppliers} table (schema.postgres.sql). Note
 * {@code is_active} is a legacy VARCHAR("true"/"false"), not a boolean —
 * kept as-is per the "don't redesign the existing schema" constraint.
 */
@Entity
@Table(name = "suppliers")
@NamedQuery(
        name = "Supplier.findActiveByEmail",
        query = "SELECT s FROM Supplier s WHERE s.email = :email AND s.isActive = 'true'")
@Getter
@Setter
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "email", nullable = false)
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

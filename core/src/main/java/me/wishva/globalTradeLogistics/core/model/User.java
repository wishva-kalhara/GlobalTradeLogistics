package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.Role;

/**
 * Maps the additive {@code users} table (internal staff, admin-provisioned,
 * no password column). Schema-owned by Hibernate ({@code hibernate.hbm2ddl.auto=update}
 * in persistence.xml) — this table is created from this entity, not a
 * hand-written SQL migration.
 * <p>
 * {@code role} is a plain VARCHAR ({@code @Enumerated(STRING)}), not a
 * native Postgres enum type — Hibernate's {@code update} mode reliably
 * creates/evolves plain columns; a native enum column would need
 * {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)} and its own DDL-generation
 * quirks, not worth the risk here.
 * <p>
 * {@code @Getter}/{@code @Setter}/{@code @NoArgsConstructor} only — no
 * Lombok {@code @EqualsAndHashCode}/{@code @Data} on JPA entities, to avoid
 * the classic Lombok-vs-lazy-proxy/equals pitfall.
 */
@Entity
@Table(name = "users")
@NamedQueries({
        @NamedQuery(
                name = "User.findActiveByEmail",
                query = "SELECT u FROM User u WHERE u.email = :email AND u.active = true"),
        @NamedQuery(
                name = "User.findAll",
                query = "SELECT u FROM User u ORDER BY u.fullName")
})
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;
}

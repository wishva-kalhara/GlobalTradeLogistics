package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.Role;

/**
 * Maps the additive {@code users} table (internal staff, admin-provisioned,
 * no password column — see {@code .no-build/db/02-auth.postgres.sql}).
 * <p>
 * {@code @Getter}/{@code @Setter}/{@code @NoArgsConstructor} only — no
 * Lombok {@code @EqualsAndHashCode}/{@code @Data} on JPA entities, to avoid
 * the classic Lombok-vs-lazy-proxy/equals pitfall.
 */
@Entity
@Table(name = "users")
@NamedQuery(
        name = "User.findActiveByEmail",
        query = "SELECT u FROM User u WHERE u.email = :email AND u.active = true")
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

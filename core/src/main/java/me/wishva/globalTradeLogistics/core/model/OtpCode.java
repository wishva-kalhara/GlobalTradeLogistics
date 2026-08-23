package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps the additive {@code otp_codes} table — schema-owned by Hibernate
 * ({@code hibernate.hbm2ddl.auto=update} in persistence.xml creates this
 * table from this entity, no hand-written migration SQL). Only the SHA-256
 * hash of the one-time code is ever persisted — the plaintext code exists
 * only in the outbound {@code EmailNotification} JMS message.
 */
@Entity
@Table(name = "otp_codes")
@NamedQuery(
        name = "OtpCode.findLatestUnconsumedMatch",
        query = "SELECT o FROM OtpCode o WHERE o.email = :email AND o.codeHash = :codeHash "
                + "AND o.consumed = false ORDER BY o.createdAt DESC")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "purpose", nullable = false)
    private String purpose = "LOGIN";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed", nullable = false)
    private Boolean consumed = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (consumed == null) {
            consumed = Boolean.FALSE;
        }
    }
}

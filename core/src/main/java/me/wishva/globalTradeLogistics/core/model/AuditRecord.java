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

import java.time.Instant;

/**
 * Maps the existing {@code audit_records} table (schema.postgres.sql).
 * Written by {@code monitoring-svc}'s {@code AuditPersisterMdb} (Phase 6),
 * consuming {@code AuditEvent}s published by
 * {@link me.wishva.globalTradeLogistics.core.messaging.AuditPublisher} —
 * and while {@code IS_PROD=false} (the dev default), {@code AuditPublisher}
 * only logs instead of publishing, so this table stays empty in local runs;
 * see {@code VendorPerformanceServiceBean}'s report-listing method, which
 * simply returns whatever is present.
 * <p>
 * The table's declared PK is the composite {@code (id, reference, type)},
 * but {@code id} is a {@code SERIAL} and therefore globally unique on its
 * own — mapped as the sole {@code @Id} here (with {@code IDENTITY}
 * generation, same as every other {@code SERIAL} PK in this codebase);
 * {@code em.find(...)} by the full composite key is never needed.
 */
@Entity
@Table(name = "audit_records")
@NamedQueries({
        @NamedQuery(
                name = "AuditRecord.findByType",
                query = "SELECT a FROM AuditRecord a WHERE a.type = :type ORDER BY a.createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resource", nullable = false)
    private String resource;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "details")
    private String details;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "reference", nullable = false)
    private String reference;
}

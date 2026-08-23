package me.wishva.globalTradeLogistics.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Pulled forward from monitoring-svc (Phase 6) so {@code @Audited} has
 * something to publish now — {@code monitoring-svc}'s
 * {@code AuditPersisterMdb} is the eventual consumer that writes this into
 * {@code audit_records}; until it exists, {@link me.wishva.globalTradeLogistics.core.messaging.AuditPublisher}
 * just logs it (same deferred-consumer pattern {@code NotificationPublisher}
 * used before notification-svc existed).
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent implements Serializable {

    private String resource;
    private String action;
    private String actorEmail;
    private String reference;
    private Instant occurredAt = Instant.now();

    public AuditEvent(String resource, String action, String actorEmail, String reference) {
        this.resource = resource;
        this.action = action;
        this.actorEmail = actorEmail;
        this.reference = reference;
    }
}

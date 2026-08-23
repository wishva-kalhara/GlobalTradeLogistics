package me.wishva.globalTradeLogistics.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Pulled forward from monitoring-svc (Phase 6) so {@code @IdempotencyChecked}
 * has something to publish now — {@code monitoring-svc}'s
 * {@code IdempotencyRecorderMdb} is the eventual consumer that writes this
 * into {@code logs}; until it exists, {@link me.wishva.globalTradeLogistics.core.messaging.IdempotencyPublisher}
 * just logs it (same deferred-consumer pattern as {@code AuditEvent}/
 * {@code AuditPublisher}).
 */
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyEvent implements Serializable {

    private String idempotencyKey;
    private String className;
    private String methodName;
    private Instant occurredAt = Instant.now();

    public IdempotencyEvent(String idempotencyKey, String className, String methodName) {
        this.idempotencyKey = idempotencyKey;
        this.className = className;
        this.methodName = methodName;
    }
}

package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code logs} table (schema.postgres.sql) — read-only
 * from this side, same deferred-consumer relationship {@link AuditRecord}
 * has with {@code monitoring-svc}: rows are written by
 * {@code IdempotencyRecorderMdb} (Phase 6), consuming
 * {@code IdempotencyEvent}s published by
 * {@link me.wishva.globalTradeLogistics.core.messaging.IdempotencyPublisher}.
 * Until Phase 6 exists (and while {@code IS_PROD=false}), no rows ever land
 * here — {@code IdempotencyInterceptor}'s fast-path check against this table
 * is therefore a real, but currently always-empty, check in dev.
 */
@Entity
@Table(name = "logs")
@IdClass(LogEntryId.class)
@NamedQueries({
        @NamedQuery(
                name = "LogEntry.countByIdempotencyKey",
                query = "SELECT COUNT(l) FROM LogEntry l WHERE l.idempotencyKey = :idempotencyKey")
})
@Getter
@Setter
@NoArgsConstructor
public class LogEntry {

    @Id
    @Column(name = "created_at")
    private Integer createdAt;

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "log_level", nullable = false)
    private String logLevel;

    @Column(name = "messages", nullable = false)
    private String messages;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "line_nuber", nullable = false)
    private String lineNumber;

    @Column(name = "thread_name", nullable = false)
    private String threadName;
}

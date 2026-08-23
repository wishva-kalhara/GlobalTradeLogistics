package me.wishva.globalTradeLogistics.core.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@code @IdClass} for {@link LogEntry} — the existing {@code logs} table's
 * composite PK is {@code (created_at, idempotency_key)}.
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class LogEntryId implements Serializable {

    private Integer createdAt;
    private String idempotencyKey;
}

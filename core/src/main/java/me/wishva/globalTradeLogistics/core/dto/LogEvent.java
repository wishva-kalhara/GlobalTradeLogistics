package me.wishva.globalTradeLogistics.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;

import java.io.Serializable;
import java.time.Instant;

/**
 * A single step-by-step breadcrumb fired via CDI ({@code Event<LogEvent>})
 * from inside a business flow, observed application-wide by {@code LogsObserver}
 * (monitoring-svc) and forwarded onto {@code AppConfig#LOG_TOPIC_JNDI} so it's
 * visible in the server log even when nothing has actually broken — the only
 * way to see what a flow was doing in prod without a debugger attached.
 * <p>
 * Deliberately not persisted anywhere (see {@code TraceLogMdb}) — this is a
 * live tail, not an audit trail; {@code AuditEvent}/{@code IdempotencyEvent}
 * already own "durably recorded."
 */
@Getter
@Setter
@NoArgsConstructor
public class LogEvent implements Serializable {

    private String correlationKey;
    private LogLevel level;
    private String message;
    private Instant occurredAt = Instant.now();

    public LogEvent(String correlationKey, LogLevel level, String message) {
        this.correlationKey = correlationKey;
        this.level = level;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + level + "] " + correlationKey + " - " + message;
    }
}

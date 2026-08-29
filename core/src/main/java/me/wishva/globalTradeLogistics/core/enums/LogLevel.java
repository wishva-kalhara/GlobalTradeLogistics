package me.wishva.globalTradeLogistics.core.enums;

/**
 * Severity of a {@link me.wishva.globalTradeLogistics.core.dto.LogEvent} —
 * TRACE is the level every business flow's step-by-step breadcrumbs use (see
 * {@code LogsObserver}); the others exist so a flow can flag something worth
 * more attention without inventing a second event type.
 */
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

package me.wishva.globalTradeLogistics.core.dto;

/**
 * Implemented by a business method's return DTO when it wants
 * {@link me.wishva.globalTradeLogistics.core.interceptor.AuditInterceptor} to
 * record more than just "this method succeeded" — a reference id and/or a
 * free-text details string for the resulting {@code AuditEvent}. Optional:
 * a return type that doesn't implement this still gets a plain audit entry
 * with no reference/details, same as before this interface existed.
 */
public interface Auditable {

    default String getAuditReference() {
        return null;
    }

    default String getAuditDetails() {
        return null;
    }
}

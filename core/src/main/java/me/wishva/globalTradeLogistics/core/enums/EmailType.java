package me.wishva.globalTradeLogistics.core.enums;

/**
 * Every kind of email the system ever sends. Adding a future email use case
 * is a new value here plus a template in notification-svc — never a change
 * to the {@link me.wishva.globalTradeLogistics.core.dto.EmailNotification} shape.
 */
public enum EmailType {
    OTP_AUTHENTICATION,
    CUSTOMER_ONBOARDING,
    SUPPLIER_ONBOARDING,
    WORKER_ONBOARDING,
    ORDER_CONFIRMATION,
    PO_OVERDUE_ALERT,
    CUSTOMS_DEADLINE_WARNING,
    REORDER_ALERT,
    VENDOR_PERFORMANCE_REPORT
}

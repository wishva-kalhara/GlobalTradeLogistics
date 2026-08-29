package me.wishva.globalTradeLogistics.notificationSvc.mail;

import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.enums.EmailType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders an {@link EmailNotification} into a subject line and an HTML body,
 * by merging a per-{@link EmailType} content fragment (under
 * {@code src/main/resources/templates/}) into the shared {@code layout.html}
 * wrapper. Placeholders are {@code {{key}}} tokens, filled from the
 * notification's {@code templateParams} plus a derived {@code recipientName}
 * (defaulted to "there" when blank, since several call sites — see
 * {@code RegistrationServiceBean} — publish with a null name).
 */
final class EmailTemplateService {

    private static final Map<EmailType, String> TEMPLATE_FILES = new EnumMap<>(EmailType.class);
    private static final Map<EmailType, String> SUBJECTS = new EnumMap<>(EmailType.class);

    static {
        TEMPLATE_FILES.put(EmailType.OTP_AUTHENTICATION, "otp-authentication.html");
        SUBJECTS.put(EmailType.OTP_AUTHENTICATION, "Your GlobalTrade Logistics verification code");

        TEMPLATE_FILES.put(EmailType.CUSTOMER_ONBOARDING, "customer-onboarding.html");
        SUBJECTS.put(EmailType.CUSTOMER_ONBOARDING, "Welcome to GlobalTrade Logistics");

        TEMPLATE_FILES.put(EmailType.SUPPLIER_ONBOARDING, "supplier-onboarding.html");
        SUBJECTS.put(EmailType.SUPPLIER_ONBOARDING, "Welcome to GlobalTrade Logistics");

        TEMPLATE_FILES.put(EmailType.WORKER_ONBOARDING, "worker-onboarding.html");
        SUBJECTS.put(EmailType.WORKER_ONBOARDING, "Your GlobalTrade Logistics staff account");

        TEMPLATE_FILES.put(EmailType.ORDER_CONFIRMATION, "order-confirmation.html");
        SUBJECTS.put(EmailType.ORDER_CONFIRMATION, "Order #{{orderId}} confirmed");

        TEMPLATE_FILES.put(EmailType.PO_OVERDUE_ALERT, "po-overdue-alert.html");
        SUBJECTS.put(EmailType.PO_OVERDUE_ALERT, "Purchase order #{{poId}} is overdue");

        TEMPLATE_FILES.put(EmailType.CUSTOMS_DEADLINE_WARNING, "customs-deadline-warning.html");
        SUBJECTS.put(EmailType.CUSTOMS_DEADLINE_WARNING, "Customs clearance pending for shipment #{{shipmentId}}");

        TEMPLATE_FILES.put(EmailType.REORDER_ALERT, "reorder-alert.html");
        SUBJECTS.put(EmailType.REORDER_ALERT, "Low stock alert: {{productName}}");

        TEMPLATE_FILES.put(EmailType.VENDOR_PERFORMANCE_REPORT, "vendor-performance-report.html");
        SUBJECTS.put(EmailType.VENDOR_PERFORMANCE_REPORT, "Vendor performance report");
    }

    private static final String LAYOUT = loadResource("templates/layout.html");
    private static final Map<EmailType, String> CONTENT_CACHE = new ConcurrentHashMap<>();

    private EmailTemplateService() {
    }

    static String subjectFor(EmailNotification notification) {
        String subject = SUBJECTS.getOrDefault(notification.getType(), "GlobalTrade Logistics notification");
        return interpolate(subject, valuesFor(notification));
    }

    static String renderHtml(EmailNotification notification) {
        String contentTemplate = CONTENT_CACHE.computeIfAbsent(notification.getType(), type -> {
            String file = TEMPLATE_FILES.get(type);
            if (file == null) {
                throw new IllegalStateException("No email template mapped for EmailType." + type);
            }
            return loadResource("templates/" + file);
        });

        String content = interpolate(contentTemplate, valuesFor(notification));
        return LAYOUT.replace("{{content}}", content);
    }

    private static Map<String, String> valuesFor(EmailNotification notification) {
        Map<String, String> values = new HashMap<>(notification.getTemplateParams());
        String name = notification.getRecipientName();
        values.put("recipientName", (name == null || name.isBlank()) ? "there" : name);
        return values;
    }

    private static String interpolate(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", escapeHtml(entry.getValue()));
        }
        return result;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String loadResource(String path) {
        try (InputStream in = EmailTemplateService.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing email template resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template resource: " + path, e);
        }
    }
}

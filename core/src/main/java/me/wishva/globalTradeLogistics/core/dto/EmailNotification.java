package me.wishva.globalTradeLogistics.core.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.EmailType;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The single shape every email-sending flow in the system publishes to
 * {@code jms/notification.email.send}. Adding a new kind of email later is
 * a new {@link EmailType} value plus a template in notification-svc —
 * never a change to this class.
 * <p>
 * The 4-arg constructor is hand-written (not {@code @AllArgsConstructor})
 * since {@code occurredAt} is always defaulted to "now" at construction
 * time, never passed in by callers.
 */
@Getter
@Setter
@NoArgsConstructor
public class EmailNotification implements Serializable {

    private EmailType type;
    private String recipientEmail;
    private String recipientName;
    private Map<String, String> templateParams = new HashMap<>();
    private Instant occurredAt = Instant.now();

    public EmailNotification(EmailType type, String recipientEmail, String recipientName, Map<String, String> templateParams) {
        this.type = type;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        if (templateParams != null) {
            this.templateParams = templateParams;
        }
    }
}

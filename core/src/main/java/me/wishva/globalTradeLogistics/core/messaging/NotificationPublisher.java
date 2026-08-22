package me.wishva.globalTradeLogistics.core.messaging;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Topic;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes {@link EmailNotification}s to {@link AppConfig#NOTIFICATION_TOPIC_JNDI}.
 * Every email-sending flow in every module goes through this one class.
 * <p>
 * Gated by {@link AppConfig#IS_PROD}: when false (the default for local/dev
 * runs), no JMS send happens at all — the notification is logged instead,
 * so OTP/onboarding/alert flows stay fully testable without a real inbox
 * or even a provisioned JMS destination.
 * <p>
 * When {@code IS_PROD=true}, the topic + connection factory are provisioned
 * by notification-svc's deployment wiring (Phase 5/9) — until that lands,
 * a lookup failure here is logged and swallowed rather than failing the
 * caller's business operation, since sending the email is a side effect,
 * not the operation itself.
 */
public final class NotificationPublisher {

    private static final Logger LOG = Logger.getLogger(NotificationPublisher.class.getName());

    private NotificationPublisher() {
    }

    public static void publish(EmailNotification notification) {
        if (!AppConfig.IS_PROD) {
            LOG.info(() -> "IS_PROD=false — skipping email send. type=" + notification.getType()
                    + " recipientEmail=" + notification.getRecipientEmail()
                    + " recipientName=" + notification.getRecipientName()
                    + " templateParams=" + notification.getTemplateParams());
            return;
        }

        try {
            InitialContext context = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(AppConfig.NOTIFICATION_TOPIC_CF_JNDI);
            Topic topic = (Topic) context.lookup(AppConfig.NOTIFICATION_TOPIC_JNDI);
            try (JMSContext jmsContext = connectionFactory.createContext()) {
                jmsContext.createProducer().send(topic, notification);
            }
        } catch (NamingException | JMSRuntimeException e) {
            LOG.log(Level.WARNING,
                    "Could not publish EmailNotification of type " + notification.getType()
                            + " (" + AppConfig.NOTIFICATION_TOPIC_JNDI + " not provisioned yet?)", e);
        }
    }
}

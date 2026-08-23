package me.wishva.globalTradeLogistics.core.messaging;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Topic;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes {@link AuditEvent}s to {@link AppConfig#AUDIT_TOPIC_JNDI}. Pulled
 * forward from monitoring-svc (Phase 6) so {@code @Audited} has a real
 * publisher now — same {@code IS_PROD}-gated, missing-destination-tolerant
 * pattern as {@link NotificationPublisher}: until monitoring-svc provisions
 * the topic and a consumer, the event is just logged.
 */
public final class AuditPublisher {

    private static final Logger LOG = Logger.getLogger(AuditPublisher.class.getName());

    private AuditPublisher() {
    }

    public static void publish(AuditEvent event) {
        if (!AppConfig.IS_PROD) {
            LOG.info(() -> "IS_PROD=false — logging audit event instead of publishing. resource=" + event.getResource()
                    + " action=" + event.getAction() + " actorEmail=" + event.getActorEmail()
                    + " reference=" + event.getReference());
            return;
        }

        try {
            InitialContext context = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(AppConfig.AUDIT_TOPIC_CF_JNDI);
            Topic topic = (Topic) context.lookup(AppConfig.AUDIT_TOPIC_JNDI);
            try (JMSContext jmsContext = connectionFactory.createContext()) {
                jmsContext.createProducer().send(topic, event);
            }
        } catch (NamingException | JMSRuntimeException e) {
            LOG.log(Level.WARNING,
                    "Could not publish AuditEvent for resource " + event.getResource()
                            + " (" + AppConfig.AUDIT_TOPIC_JNDI + " not provisioned yet?)", e);
        }
    }
}

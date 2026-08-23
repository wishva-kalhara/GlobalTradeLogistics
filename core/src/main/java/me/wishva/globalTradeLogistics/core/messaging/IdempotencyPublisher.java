package me.wishva.globalTradeLogistics.core.messaging;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.IdempotencyEvent;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publishes {@link IdempotencyEvent}s to {@link AppConfig#IDEMPOTENCY_QUEUE_JNDI}.
 * Pulled forward from monitoring-svc (Phase 6) so {@code @IdempotencyChecked}
 * has a real publisher now — same {@code IS_PROD}-gated, missing-destination
 * -tolerant pattern as {@link NotificationPublisher}/{@link AuditPublisher}:
 * until monitoring-svc provisions the queue and a consumer, the event is
 * just logged.
 */
public final class IdempotencyPublisher {

    private static final Logger LOG = Logger.getLogger(IdempotencyPublisher.class.getName());

    private IdempotencyPublisher() {
    }

    public static void publish(IdempotencyEvent event) {
        if (!AppConfig.IS_PROD) {
            LOG.info(() -> "IS_PROD=false — logging idempotency event instead of publishing. key=" + event.getIdempotencyKey()
                    + " class=" + event.getClassName() + " method=" + event.getMethodName());
            return;
        }

        try {
            InitialContext context = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup(AppConfig.IDEMPOTENCY_QUEUE_CF_JNDI);
            Queue queue = (Queue) context.lookup(AppConfig.IDEMPOTENCY_QUEUE_JNDI);
            try (JMSContext jmsContext = connectionFactory.createContext()) {
                jmsContext.createProducer().send(queue, event);
            }
        } catch (NamingException | JMSRuntimeException e) {
            LOG.log(Level.WARNING,
                    "Could not publish IdempotencyEvent for key " + event.getIdempotencyKey()
                            + " (" + AppConfig.IDEMPOTENCY_QUEUE_JNDI + " not provisioned yet?)", e);
        }
    }
}

package me.wishva.globalTradeLogistics.monitoringSvc.services;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumes {@link LogEvent}s forwarded by {@link LogsObserver} and prints
 * them — this is a live tail for prod debugging, not a durable record.
 * Deliberately does NOT write to the {@code logs} table (unlike
 * {@code IdempotencyRecorderMdb}, which legitimately owns rows there): a
 * TRACE line fired on every step of every flow would flood that table, and
 * nothing here needs to survive a restart.
 * <p>
 * The JNDI name below must stay in sync with
 * {@link me.wishva.globalTradeLogistics.core.configs.AppConfig#LOG_TOPIC_JNDI}'s
 * default — {@code @ActivationConfigProperty} values must be compile-time
 * constants, so it can't reference the constant directly.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/monitoring.trace.log"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
})
public class TraceLogMdb implements MessageListener {

    private static final Logger LOG = Logger.getLogger("TRACE");

    @Override
    public void onMessage(Message message) {
        try {
            LogEvent event = (LogEvent) ((ObjectMessage) message).getObject();
            LOG.log(Level.INFO, event::toString);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Failed to read LogEvent from JMS message", e);
        }
    }
}

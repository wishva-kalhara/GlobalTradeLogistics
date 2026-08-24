package me.wishva.globalTradeLogistics.monitoringSvc.services;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.IdempotencyEvent;
import me.wishva.globalTradeLogistics.core.model.LogEntry;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumes {@link IdempotencyEvent}s published by
 * {@link me.wishva.globalTradeLogistics.core.messaging.IdempotencyPublisher}
 * (every {@code @IdempotencyChecked} method, since Phase 4) and durably
 * records them into {@code logs} — the missing half of
 * {@code IdempotencyInterceptor}'s fast-path check, which reads exactly this
 * table.
 * <p>
 * The JNDI name below must stay in sync with
 * {@link me.wishva.globalTradeLogistics.core.configs.AppConfig#IDEMPOTENCY_QUEUE_JNDI}'s
 * default — {@code @ActivationConfigProperty} values must be compile-time
 * constants, so it can't reference the constant directly.
 * <p>
 * {@code file_name}/{@code line_nuber} have no real source to recover from
 * an async JMS message (the original caller's stack frame is long gone by
 * the time this consumer runs) — populated with the same best-effort
 * placeholders {@code ShipmentServiceBean}'s Phase 4 timers already use for
 * similarly-unavailable context.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/monitoring.idempotency.check"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class IdempotencyRecorderMdb implements MessageListener {

    private static final Logger LOG = Logger.getLogger(IdempotencyRecorderMdb.class.getName());

    /** Every {@code logs} string column (other than {@code created_at}) is a legacy {@code VARCHAR(45)}. */
    private static final int MAX_COLUMN_LENGTH = 45;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Override
    public void onMessage(Message message) {
        try {
            IdempotencyEvent event = (IdempotencyEvent) ((ObjectMessage) message).getObject();

            LogEntry entry = new LogEntry();
            entry.setCreatedAt((int) event.getOccurredAt().getEpochSecond());
            entry.setIdempotencyKey(truncate(event.getIdempotencyKey()));
            entry.setLogLevel("INFO");
            entry.setMessages(truncate("Idempotency key recorded"));
            entry.setClassName(truncate(event.getClassName()));
            entry.setMethodName(truncate(event.getMethodName()));
            entry.setFileName(truncate(event.getClassName() + ".java"));
            entry.setLineNumber("0");
            entry.setThreadName(truncate(Thread.currentThread().getName()));
            em.persist(entry);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Failed to read IdempotencyEvent from JMS message", e);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_COLUMN_LENGTH ? value.substring(0, MAX_COLUMN_LENGTH) : value;
    }
}

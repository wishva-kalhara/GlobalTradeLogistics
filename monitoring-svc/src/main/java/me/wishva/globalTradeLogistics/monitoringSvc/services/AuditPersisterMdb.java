package me.wishva.globalTradeLogistics.monitoringSvc.services;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.wishva.globalTradeLogistics.core.dto.AuditEvent;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.core.model.AuditRecord;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumes {@link AuditEvent}s published by
 * {@link me.wishva.globalTradeLogistics.core.messaging.AuditPublisher} (every
 * {@code @Audited} bean, since Phase 2) and durably records them —
 * completing the deferred-consumer pattern those beans have been publishing
 * into since before this module existed.
 * <p>
 * The JNDI name below must stay in sync with
 * {@link me.wishva.globalTradeLogistics.core.configs.AppConfig#AUDIT_TOPIC_JNDI}'s
 * default — {@code @ActivationConfigProperty} values must be compile-time
 * constants, so it can't reference the constant directly.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/monitoring.audit.log"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
})
public class AuditPersisterMdb implements MessageListener {

    private static final Logger LOG = Logger.getLogger(AuditPersisterMdb.class.getName());

    /** Every {@code audit_records} string column is a legacy {@code VARCHAR(45)}. */
    private static final int MAX_COLUMN_LENGTH = 45;

    @PersistenceContext(unitName = "globalTradeLogisticsPU")
    private EntityManager em;

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public void onMessage(Message message) {
        try {
            AuditEvent event = (AuditEvent) ((ObjectMessage) message).getObject();
            logEvent.fire(new LogEvent(event.getActorEmail(), LogLevel.TRACE,
                    "AuditPersisterMdb: persisting audit " + event.getResource() + "." + event.getAction()));

            AuditRecord record = new AuditRecord();
            record.setCreatedAt(event.getOccurredAt());
            record.setResource(truncate(event.getResource()));
            record.setAction(truncate(event.getAction()));
            record.setDetails(truncate(event.getDetails()));
            record.setType(truncate(event.getType() != null ? event.getType() : event.getResource()));
            record.setReference(truncate(event.getReference() != null ? event.getReference() : ""));
            em.persist(record);
        } catch (JMSException e) {
            logEvent.fire(new LogEvent("audit-mdb", LogLevel.WARN,
                    "AuditPersisterMdb: failed to read AuditEvent from JMS message - " + e.getMessage()));
            LOG.log(Level.SEVERE, "Failed to read AuditEvent from JMS message", e);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_COLUMN_LENGTH ? value.substring(0, MAX_COLUMN_LENGTH) : value;
    }
}

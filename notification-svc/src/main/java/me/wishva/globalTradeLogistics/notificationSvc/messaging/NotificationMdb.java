package me.wishva.globalTradeLogistics.notificationSvc.messaging;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;
import me.wishva.globalTradeLogistics.core.enums.LogLevel;
import me.wishva.globalTradeLogistics.notificationSvc.mail.EmailSenderService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumes {@link EmailNotification}s published by
 * {@link me.wishva.globalTradeLogistics.core.messaging.NotificationPublisher}
 * (every email-sending flow in the system, gated behind {@code IS_PROD=true})
 * and actually sends the email — completing the deferred-consumer pattern
 * that publisher has been publishing into since before this module existed.
 * <p>
 * The JNDI name below must stay in sync with
 * {@link me.wishva.globalTradeLogistics.core.configs.AppConfig#NOTIFICATION_TOPIC_JNDI}'s
 * default — {@code @ActivationConfigProperty} values must be compile-time
 * constants, so it can't reference the constant directly.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/notification.email.send"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic")
})
public class NotificationMdb implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NotificationMdb.class.getName());

    @Inject
    private Event<LogEvent> logEvent;

    @Override
    public void onMessage(Message message) {
        try {
            EmailNotification notification = (EmailNotification) ((ObjectMessage) message).getObject();
            logEvent.fire(new LogEvent(notification.getRecipientEmail(), LogLevel.TRACE,
                    "onMessage: received " + notification.getType() + " notification, forwarding to EmailSenderService"));
            EmailSenderService.send(notification);
        } catch (JMSException e) {
            logEvent.fire(new LogEvent("notification-mdb", LogLevel.WARN, "onMessage: failed to read EmailNotification from JMS message - " + e.getMessage()));
            LOG.log(Level.SEVERE, "Failed to read EmailNotification from JMS message", e);
        }
    }
}

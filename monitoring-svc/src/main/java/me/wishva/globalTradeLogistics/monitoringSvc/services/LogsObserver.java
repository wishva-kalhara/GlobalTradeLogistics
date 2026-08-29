package me.wishva.globalTradeLogistics.monitoringSvc.services;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Topic;
import me.wishva.globalTradeLogistics.core.dto.LogEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-wide CDI observer: every {@code Event<LogEvent>.fire(...)}
 * call anywhere in the EAR — one per business-flow step, fired from the
 * service beans themselves — lands here (CDI events cross bean-archive
 * boundaries within one Java EE application, so this single
 * {@code monitoring-svc} bean sees traces from every other module without
 * any of them depending on monitoring-svc directly). Forwards each one onto
 * {@link me.wishva.globalTradeLogistics.core.configs.AppConfig#LOG_TOPIC_JNDI}
 * for {@code TraceLogMdb} to print.
 * <p>
 * Unlike {@code AuditPublisher}/{@code NotificationPublisher}/
 * {@code IdempotencyPublisher}, this is not gated behind {@code IS_PROD} —
 * step-by-step tracing is exactly what's needed to debug a real prod issue,
 * so it always forwards. The JMS resources are provisioned unconditionally
 * by {@code entrypoint.sh} regardless of {@code IS_PROD}, so the lookup
 * always succeeds; a failure is logged and swallowed rather than thrown,
 * since losing one trace line must never break the business flow that fired
 * it.
 * <p>
 * {@code @Resource(lookup = ...)} values must be compile-time constants, so
 * these can't reference {@code AppConfig}'s constants directly — keep them
 * in sync with {@code AppConfig#LOG_TOPIC_CF_JNDI}/{@code #LOG_TOPIC_JNDI}'s
 * defaults.
 */
@ApplicationScoped
public class LogsObserver {

    private static final Logger LOG = Logger.getLogger(LogsObserver.class.getName());

    @Resource(lookup = "jms/monitoring.trace.log.factory")
    private ConnectionFactory connectionFactory;

    @Resource(lookup = "jms/monitoring.trace.log")
    private Topic topic;

    public void onLogEvent(@Observes LogEvent event) {
        try (JMSContext context = connectionFactory.createContext()) {
            context.createProducer().send(topic, event);
        } catch (JMSRuntimeException e) {
            LOG.log(Level.WARNING, "Could not forward LogEvent to " + topic, e);
        }
    }
}

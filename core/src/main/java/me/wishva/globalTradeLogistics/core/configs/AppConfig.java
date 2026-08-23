package me.wishva.globalTradeLogistics.core.configs;

import java.util.logging.Logger;

/**
 * Single source of truth for every environment variable the application
 * reads. Every module MUST go through this class instead of calling
 * {@code System.getenv(...)} directly — scattering raw env var name
 * literals across modules is exactly how config drifts out of sync with
 * {@code .desired-state/app.env} and {@code entrypoint.sh}. Add a new
 * variable here once, and every module picks it up through one field.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    /** HS256 signing secret for issued JWTs — see {@code JwtService}. */
    public static final String JWT_SECRET = require("JWT_SECRET");

    /**
     * Gates side effects that only make sense against a real downstream
     * system (currently: sending real email). False — the safe default for
     * local/dev runs — logs what would have happened instead of doing it.
     * See {@code NotificationPublisher}.
     */
    public static final boolean IS_PROD = Boolean.parseBoolean(optional("IS_PROD", "false"));

    /** JNDI name of the notification-email JMS Topic (notification-svc). */
    public static final String NOTIFICATION_TOPIC_JNDI =
            optional("NOTIFICATION_TOPIC_JNDI", "jms/notification.email.send");

    /** JNDI name of the connection factory for {@link #NOTIFICATION_TOPIC_JNDI}. */
    public static final String NOTIFICATION_TOPIC_CF_JNDI =
            optional("NOTIFICATION_TOPIC_CF_JNDI", "jms/notification.email.send.factory");

    /** JNDI name of the audit-log JMS Topic (monitoring-svc, Phase 6). */
    public static final String AUDIT_TOPIC_JNDI =
            optional("AUDIT_TOPIC_JNDI", "jms/monitoring.audit.log");

    /** JNDI name of the connection factory for {@link #AUDIT_TOPIC_JNDI}. */
    public static final String AUDIT_TOPIC_CF_JNDI =
            optional("AUDIT_TOPIC_CF_JNDI", "jms/monitoring.audit.log.factory");

    /**
     * Email/full name for the bootstrap ADMIN account — see
     * {@code AdminSeedBean}. There is no other way to get an ADMIN into an
     * empty {@code users} table ({@code IUserAdminService.createUser}
     * itself requires an existing ADMIN), so this has to be a deploy-time
     * seed rather than an API call.
     */
    public static final String ADMIN_EMAIL = optional("ADMIN_EMAIL", "admin@globaltradelogistics.local");
    public static final String ADMIN_FULL_NAME = optional("ADMIN_FULL_NAME", "System Administrator");

    private AppConfig() {
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required but was not set");
        }
        return value;
    }

    private static String optional(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            LOG.fine(() -> name + " not set, defaulting to '" + defaultValue + "'");
            return defaultValue;
        }
        return value;
    }
}

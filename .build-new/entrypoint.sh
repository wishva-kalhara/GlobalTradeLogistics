#!/usr/bin/env bash
set -euo pipefail

# All desired state for this container is expected to arrive as environment
# variables — MySQL credentials + GlassFish/JDBC-pool/JMS-resource config
# sourced from .desired-state/glassfish.conf (env_file), the application's
# own runtime config from docker-compose.yml's app.environment block
# directly (wired up via docker-compose.yml). Fail fast if required values
# are missing instead of silently defaulting.
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required (see .desired-state/glassfish.conf)}"
: "${MYSQL_USER:?MYSQL_USER is required (see .desired-state/glassfish.conf)}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required (see .desired-state/glassfish.conf)}"
: "${DB_HOST:?DB_HOST is required (see .desired-state/glassfish.conf)}"
: "${DB_PORT:?DB_PORT is required (see .desired-state/glassfish.conf)}"
: "${HTTP_PORT:?HTTP_PORT is required (see .desired-state/glassfish.conf)}"
: "${HTTPS_PORT:?HTTPS_PORT is required (see .desired-state/glassfish.conf)}"
: "${JNDI_NAME:?JNDI_NAME is required (see .desired-state/glassfish.conf)}"
: "${POOL_NAME:?POOL_NAME is required (see .desired-state/glassfish.conf)}"
: "${DB_POOL_MIN_SIZE:?DB_POOL_MIN_SIZE is required (see .desired-state/glassfish.conf)}"
: "${DB_POOL_MAX_SIZE:?DB_POOL_MAX_SIZE is required (see .desired-state/glassfish.conf)}"
: "${DB_POOL_MAX_WAIT_MS:?DB_POOL_MAX_WAIT_MS is required (see .desired-state/glassfish.conf)}"
: "${JWT_SECRET:?JWT_SECRET is required (see docker-compose.yml app.environment block)}"
: "${NOTIFICATION_TOPIC_CF_JNDI:?NOTIFICATION_TOPIC_CF_JNDI is required (see .desired-state/glassfish.conf)}"
: "${NOTIFICATION_TOPIC_JNDI:?NOTIFICATION_TOPIC_JNDI is required (see .desired-state/glassfish.conf)}"
: "${AUDIT_TOPIC_CF_JNDI:?AUDIT_TOPIC_CF_JNDI is required (see .desired-state/glassfish.conf)}"
: "${AUDIT_TOPIC_JNDI:?AUDIT_TOPIC_JNDI is required (see .desired-state/glassfish.conf)}"
: "${IDEMPOTENCY_QUEUE_CF_JNDI:?IDEMPOTENCY_QUEUE_CF_JNDI is required (see .desired-state/glassfish.conf)}"
: "${IDEMPOTENCY_QUEUE_JNDI:?IDEMPOTENCY_QUEUE_JNDI is required (see .desired-state/glassfish.conf)}"

ASADMIN="${GLASSFISH_HOME}/bin/asadmin"
DOMAIN="domain1"
EAR_FILE="/opt/deploy/global-trade-logistics.ear"

echo "[entrypoint] starting GlassFish domain: ${DOMAIN}"
"${ASADMIN}" start-domain "${DOMAIN}"

echo "[entrypoint] configuring listeners"
"${ASADMIN}" set server-config.network-config.network-listeners.network-listener.http-listener-1.port="${HTTP_PORT}"
"${ASADMIN}" set server-config.network-config.network-listeners.network-listener.http-listener-2.port="${HTTPS_PORT}"

echo "[entrypoint] configuring JDBC connection pool for MySQL (${DB_HOST}:${DB_PORT}/${MYSQL_DATABASE}, steady=${DB_POOL_MIN_SIZE} max=${DB_POOL_MAX_SIZE})"
# useSSL=false + allowPublicKeyRetrieval=true: this dev container's MySQL
# isn't configured with TLS certs, and its default auth plugin
# (caching_sha2_password) needs the server's RSA public key up front unless
# the connection is already encrypted — both are dev-only conveniences, not
# something to carry into a real deployment. serverTimezone=UTC avoids
# Connector/J's "unable to determine time zone" failure when the container's
# system timezone isn't explicitly set.
if ! "${ASADMIN}" list-jdbc-connection-pools | grep -q "^${POOL_NAME}$"; then
  "${ASADMIN}" create-jdbc-connection-pool \
    --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
    --restype javax.sql.DataSource \
    --steadypoolsize "${DB_POOL_MIN_SIZE}" \
    --maxpoolsize "${DB_POOL_MAX_SIZE}" \
    --maxwait "${DB_POOL_MAX_WAIT_MS}" \
    --property "serverName=${DB_HOST}:port=${DB_PORT}:databaseName=${MYSQL_DATABASE}:user=${MYSQL_USER}:password=${MYSQL_PASSWORD}:useSSL=false:allowPublicKeyRetrieval=true:serverTimezone=UTC" \
    "${POOL_NAME}"
fi

if ! "${ASADMIN}" list-jdbc-resources | grep -q "^${JNDI_NAME}$"; then
  "${ASADMIN}" create-jdbc-resource --connectionpoolid "${POOL_NAME}" "${JNDI_NAME}"
fi

# Pulled forward from Phase 5/9 (notification-svc) so Phase 1's OTP/onboarding
# flows can publish EmailNotifications end-to-end. The physical destination
# and connection factory ride on GlassFish's built-in OpenMQ (jmsra) — no
# external broker container needed.
echo "[entrypoint] configuring JMS notification topic (${NOTIFICATION_TOPIC_JNDI})"
if ! "${ASADMIN}" list-jms-resources | grep -q "^${NOTIFICATION_TOPIC_CF_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.ConnectionFactory "${NOTIFICATION_TOPIC_CF_JNDI}"
fi

if ! "${ASADMIN}" list-jms-resources | grep -q "^${NOTIFICATION_TOPIC_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.Topic --property Name=notification.email.send "${NOTIFICATION_TOPIC_JNDI}"
fi

# Phase 6 (monitoring-svc): audit trail Topic + idempotency-check Queue,
# same idempotent asadmin pattern as the notification topic above.
echo "[entrypoint] configuring JMS monitoring resources (${AUDIT_TOPIC_JNDI}, ${IDEMPOTENCY_QUEUE_JNDI})"
if ! "${ASADMIN}" list-jms-resources | grep -q "^${AUDIT_TOPIC_CF_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.ConnectionFactory "${AUDIT_TOPIC_CF_JNDI}"
fi

if ! "${ASADMIN}" list-jms-resources | grep -q "^${AUDIT_TOPIC_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.Topic --property Name=monitoring.audit.log "${AUDIT_TOPIC_JNDI}"
fi

if ! "${ASADMIN}" list-jms-resources | grep -q "^${IDEMPOTENCY_QUEUE_CF_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.ConnectionFactory "${IDEMPOTENCY_QUEUE_CF_JNDI}"
fi

if ! "${ASADMIN}" list-jms-resources | grep -q "^${IDEMPOTENCY_QUEUE_JNDI}$"; then
  "${ASADMIN}" create-jms-resource --restype jakarta.jms.Queue --property Name=monitoring.idempotency.check "${IDEMPOTENCY_QUEUE_JNDI}"
fi

echo "[entrypoint] deploying ${EAR_FILE}"
"${ASADMIN}" deploy --force=true "${EAR_FILE}"

echo "[entrypoint] GlassFish is up. Tailing server log..."
exec tail -F "${GLASSFISH_HOME}/glassfish/domains/${DOMAIN}/logs/server.log"

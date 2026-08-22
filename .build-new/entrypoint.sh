#!/usr/bin/env bash
set -euo pipefail

# All desired state for this container is expected to arrive as environment
# variables sourced from .desired-state/db.env and .desired-state/app.env
# (wired up via docker-compose.yml's env_file directives). Fail fast if
# required values are missing instead of silently defaulting.
: "${POSTGRES_DB:?POSTGRES_DB is required (see .desired-state/db.env)}"
: "${POSTGRES_USER:?POSTGRES_USER is required (see .desired-state/db.env)}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required (see .desired-state/db.env)}"
: "${DB_HOST:?DB_HOST is required (see .desired-state/app.env)}"
: "${DB_PORT:?DB_PORT is required (see .desired-state/app.env)}"
: "${HTTP_PORT:?HTTP_PORT is required (see .desired-state/app.env)}"
: "${HTTPS_PORT:?HTTPS_PORT is required (see .desired-state/app.env)}"
: "${JNDI_NAME:?JNDI_NAME is required (see .desired-state/app.env)}"
: "${POOL_NAME:?POOL_NAME is required (see .desired-state/app.env)}"

ASADMIN="${GLASSFISH_HOME}/bin/asadmin"
DOMAIN="domain1"
EAR_FILE="/opt/deploy/global-trade-logistics.ear"

echo "[entrypoint] starting GlassFish domain: ${DOMAIN}"
"${ASADMIN}" start-domain "${DOMAIN}"

echo "[entrypoint] configuring listeners"
"${ASADMIN}" set server-config.network-config.network-listeners.network-listener.http-listener-1.port="${HTTP_PORT}"
"${ASADMIN}" set server-config.network-config.network-listeners.network-listener.http-listener-2.port="${HTTPS_PORT}"

echo "[entrypoint] configuring JDBC connection pool for PostgreSQL (${DB_HOST}:${DB_PORT}/${POSTGRES_DB})"
if ! "${ASADMIN}" list-jdbc-connection-pools | grep -q "^${POOL_NAME}$"; then
  "${ASADMIN}" create-jdbc-connection-pool \
    --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
    --restype javax.sql.DataSource \
    --property "serverName=${DB_HOST}:portNumber=${DB_PORT}:databaseName=${POSTGRES_DB}:user=${POSTGRES_USER}:password=${POSTGRES_PASSWORD}" \
    "${POOL_NAME}"
fi

if ! "${ASADMIN}" list-jdbc-resources | grep -q "^${JNDI_NAME}$"; then
  "${ASADMIN}" create-jdbc-resource --connectionpoolid "${POOL_NAME}" "${JNDI_NAME}"
fi

echo "[entrypoint] deploying ${EAR_FILE}"
"${ASADMIN}" deploy --force=true "${EAR_FILE}"

echo "[entrypoint] GlassFish is up. Tailing server log..."
exec tail -F "${GLASSFISH_HOME}/glassfish/domains/${DOMAIN}/logs/server.log"

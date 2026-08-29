# GlobalTrade Logistics — Manual Setup Guide (no `docker compose up`)

Everything `docker compose up` does for you — build the EAR, install GlassFish + the Postgres JDBC driver, configure the JDBC pool/resource and JMS resources, deploy — done by hand instead. Every value below (ports, pool sizes, JNDI names, env vars) is copied straight from this repo's actual desired-state files, not invented:

- [`.desired-state/versions.env`](../../.desired-state/versions.env) — tool versions
- [`.desired-state/db.env`](../../.desired-state/db.env) — Postgres credentials
- [`.desired-state/app.env`](../../.desired-state/app.env) — everything else
- [`.build-new/entrypoint.sh`](../../.build-new/entrypoint.sh) — the exact `asadmin` commands this guide manualizes
- [`Dockerfile`](../../Dockerfile) — the exact build/runtime versions this guide manualizes

If a value here ever disagrees with one of those files, **those files win** — this doc can drift, they're what actually runs.

---

## 1. Prerequisites

| Tool | Version used by this project | Notes |
|---|---|---|
| JDK | **11** (build *and* runtime) | `pom.xml`'s `maven.compiler.source/target` is `11`; the Dockerfile's runtime image is `eclipse-temurin:11-jdk-jammy`. A newer JDK (17/21) has been used successfully to *build* this repo in Docker during development, but match JDK 11 for the actual GlassFish runtime to stay identical to the Dockerfile. **Avoid JDK 25** for building — it breaks Lombok's annotation processing (every model/DTO in `core` uses `@Getter`/`@Setter`/etc.), which this project depends on heavily. |
| Maven | 3.9.x | Same image family (`maven:3.9-eclipse-temurin-11`) as the Dockerfile's build stage. |
| PostgreSQL | any recent version | The Dockerfile uses the `postgres:16` image; any version that supports the SQL in `schema.postgres.sql` works. |
| GlassFish | **7.0.21** | Jakarta EE 10. Download URL pattern (from the Dockerfile): `https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.21.zip` |
| PostgreSQL JDBC driver | **42.7.3** | `https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar` |

Set `GLASSFISH_HOME` to wherever you unzip GlassFish (e.g. `C:\glassfish7`), and add `%GLASSFISH_HOME%\bin` to `PATH` so `asadmin` is callable directly. On Windows the executable is `asadmin.bat`.

---

## 2. Set up PostgreSQL

These are the exact credentials the app is configured to use (`.desired-state/db.env`) — reuse them so you don't have to touch `app.env`'s connection-pool property string in §5.

```sql
CREATE DATABASE global_trade_log_corp;
CREATE USER gtl_app WITH PASSWORD 'gtl_app';
GRANT ALL PRIVILEGES ON DATABASE global_trade_log_corp TO gtl_app;
```

Then load the legacy schema (the 15 pre-existing SCM tables — everything else is created automatically by Hibernate on first deploy, see §7):

```powershell
psql -h localhost -U gtl_app -d global_trade_log_corp -f ".no-build\db\schema.postgres.sql"
```

If PostgreSQL is running somewhere other than `localhost:5432`, note the actual host/port — you'll need it for the JDBC connection pool in §5.

---

## 3. Install GlassFish and the JDBC driver

1. Download and unzip GlassFish 7.0.21 to `GLASSFISH_HOME` (e.g. `C:\glassfish7`).
2. Drop the PostgreSQL JDBC driver jar directly into the domain's lib folder — this is what makes `org.postgresql.ds.PGSimpleDataSource` resolvable when you create the connection pool in §5:
   ```
   %GLASSFISH_HOME%\glassfish\domains\domain1\lib\postgresql-jdbc.jar
   ```
   (download from the URL in §1's table).

---

## 4. Build the EAR

From the repo root:

```powershell
mvn -B clean package -DskipTests
```

This produces `app\target\glolabl-trade-logistics.ear` (note the `finalName` typo in `app/pom.xml` — it's real, not a doc typo). The EAR bundles:

| Module type | Artifacts |
|---|---|
| EJB modules | `iam-svc`, `order-svc`, `inventory-svc`, `procurement-svc`, `logistics-svc`, `monitoring-svc` |
| WAR modules (context roots) | `frontend-customer` → `/`, `frontend-seller` → `/seller`, `frontend-app` → `/app`, `api-gateway` → `/api` |
| Shared library | `core` (bundled in `lib/` — its `META-INF/persistence.xml` must resolve EAR-wide by plain `unitName`, which requires it stay in the EAR's library dir, not the EAR root) |

---

## 5. Environment variables

The app reads every one of these through `core.configs.AppConfig` — never scattered `System.getenv(...)` calls, so this table is exhaustive. **Only `JWT_SECRET` is actually required** (the app throws `IllegalStateException` at deploy time if it's missing); everything else has a code-level default matching the "Default" column.

| Variable | Default (if unset) | Used for |
|---|---|---|
| `JWT_SECRET` | *(none — required)* | HS256 signing secret for issued JWTs (`JwtService`). Any non-blank string works for local dev — e.g. `dev-only-change-me-globaltradelogistics-jwt-secret` (the repo's own dev value, from `app.env`). |
| `IS_PROD` | `false` | `false` = `NotificationPublisher` logs emails instead of sending them, and durable idempotency-key tracking is inactive (see `EXCEPTIONS.md`'s idempotency caveat). Leave `false` for local dev. |
| `NOTIFICATION_TOPIC_JNDI` | `jms/notification.email.send` | JMS Topic name for outbound email notifications. |
| `NOTIFICATION_TOPIC_CF_JNDI` | `jms/notification.email.send.factory` | Connection factory for the above. |
| `AUDIT_TOPIC_JNDI` | `jms/monitoring.audit.log` | JMS Topic for the audit trail (`@Audited`, see `ANNOTATIONS.md`). |
| `AUDIT_TOPIC_CF_JNDI` | `jms/monitoring.audit.log.factory` | Connection factory for the above. |
| `IDEMPOTENCY_QUEUE_JNDI` | `jms/monitoring.idempotency.check` | JMS Queue for durable idempotency-key tracking (`@IdempotencyChecked`). |
| `IDEMPOTENCY_QUEUE_CF_JNDI` | `jms/monitoring.idempotency.check.factory` | Connection factory for the above. |
| `ADMIN_EMAIL` | `admin@globaltradelogistics.local` | Bootstrap ADMIN account email, seeded once by `AdminSeedBean` if `users` is empty. |
| `ADMIN_FULL_NAME` | `System Administrator` | Bootstrap ADMIN's full name. |

Since the code defaults already match every value in `app.env` except `JWT_SECRET`, the minimum you actually need to set is:

```powershell
$env:JWT_SECRET = "dev-only-change-me-globaltradelogistics-jwt-secret"
```

**This must be set in the same shell/session you launch `asadmin start-domain` from** — GlassFish inherits environment variables from the process that starts it, not from a config file. If you close that shell, you'll need to re-export it before the next `start-domain`. To make it durable across sessions, set it as a permanent Windows environment variable instead (System Properties → Environment Variables, or `[Environment]::SetEnvironmentVariable("JWT_SECRET", "...", "User")` in PowerShell, then open a **new** shell).

The database connection is **not** an env var the app reads directly — it goes through the GlassFish-managed JDBC resource configured in §6, referenced by JNDI name in `core/src/main/resources/META-INF/persistence.xml`.

---

## 6. Start and configure the GlassFish domain

All commands below are the exact `asadmin` calls `entrypoint.sh` runs, with `.desired-state/app.env`'s actual values substituted in. Run from a shell where `JWT_SECRET` is already set (§5) and `asadmin` is on `PATH`.

### 6.1 Start the domain

```powershell
asadmin start-domain domain1
```

### 6.2 Configure the HTTP/HTTPS listener ports

```powershell
asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080
asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181
```

(The admin console listener stays on GlassFish's default, `4848` — nothing to configure there.)

### 6.3 Create the JDBC connection pool

Substitute your actual DB host/port/database/user/password if you didn't follow §2 exactly (host `localhost`, port `5432`, database `global_trade_log_corp`, user/password `gtl_app`/`gtl_app`).

```powershell
asadmin create-jdbc-connection-pool `
  --datasourceclassname org.postgresql.ds.PGSimpleDataSource `
  --restype javax.sql.DataSource `
  --steadypoolsize 2 `
  --maxpoolsize 32 `
  --maxwait 10000 `
  --property "serverName=localhost:portNumber=5432:databaseName=global_trade_log_corp:user=gtl_app:password=gtl_app:stringType=unspecified" `
  globalTradeLogisticsPool
```

**`stringType=unspecified` is not optional** — without it, pgjdbc binds Java `String` parameters as `varchar`, which Postgres refuses to implicitly cast into native enum columns (e.g. `users.role`). This one property is what lets JPA's `@Enumerated(STRING)` work against Postgres enum columns with no custom `AttributeConverter`.

### 6.4 Create the JDBC resource (JNDI binding)

```powershell
asadmin create-jdbc-resource --connectionpoolid globalTradeLogisticsPool jdbc/globalTradeLogisticsDS
```

This exact JNDI name (`jdbc/globalTradeLogisticsDS`) is hardcoded in `persistence.xml`'s `<jta-data-source>` — don't rename it unless you also change that file and rebuild.

### 6.5 Create the JMS resources

Three connection-factory/destination pairs, all riding on GlassFish's built-in OpenMQ (`jmsra`) — no external broker needed.

```powershell
# Notification email topic (notification-svc)
asadmin create-jms-resource --restype jakarta.jms.ConnectionFactory jms/notification.email.send.factory
asadmin create-jms-resource --restype jakarta.jms.Topic --property Name=notification.email.send jms/notification.email.send

# Audit-log topic (monitoring-svc)
asadmin create-jms-resource --restype jakarta.jms.ConnectionFactory jms/monitoring.audit.log.factory
asadmin create-jms-resource --restype jakarta.jms.Topic --property Name=monitoring.audit.log jms/monitoring.audit.log

# Idempotency-check queue (monitoring-svc)
asadmin create-jms-resource --restype jakarta.jms.ConnectionFactory jms/monitoring.idempotency.check.factory
asadmin create-jms-resource --restype jakarta.jms.Queue --property Name=monitoring.idempotency.check jms/monitoring.idempotency.check
```

If you customized any of the `*_JNDI`/`*_CF_JNDI` env vars in §5 away from their defaults, use those names here instead — they must match exactly, since `AppConfig` is what the app code looks up by.

---

## 7. Deploy

```powershell
asadmin deploy --force=true "app\target\glolabl-trade-logistics.ear"
```

`--force=true` lets you redeploy over an existing deployment without an explicit `undeploy` first — use this every time you rebuild after a code change.

**What happens automatically on first deploy** — no manual DB work needed beyond §2's legacy schema load:
- Hibernate's `hibernate.hbm2ddl.auto=update` creates every table/column this project added on top of the legacy schema (`users`, `otp_codes`, `shipments.purchase_orders_po_id`, etc.) — additive only, never destructive.
- `AdminSeedBean` inserts the bootstrap ADMIN row (§5's `ADMIN_EMAIL`/`ADMIN_FULL_NAME`) if `users` is empty.
- `CountrySeedBean` seeds the 45 countries.
- `CatalogSeedBean` seeds 5 demo products + 1 warehouse + inventory, if `products` is empty.
- `ShipmentSeedBean` seeds 1 demo shipment, if `shipments` is empty.

---

## 8. Verify

```powershell
curl http://localhost:8080/api/v1/healthz
# expect: Up and running
```

Tail the server log to watch startup and confirm no `SEVERE` lines:

```powershell
Get-Content "$env:GLASSFISH_HOME\glassfish\domains\domain1\logs\server.log" -Wait -Tail 50
```

Since `IS_PROD=false`, OTP codes and onboarding "emails" are logged, not sent — to grab a login code:

```powershell
Select-String -Path "$env:GLASSFISH_HOME\glassfish\domains\domain1\logs\server.log" -Pattern "OTP_AUTHENTICATION"
```

Take the most recent `code=NNNNNN` for the email you requested.

---

## 9. URLs

| What | URL |
|---|---|
| Customer frontend | `http://localhost:8080/` |
| Seller portal | `http://localhost:8080/seller/` |
| Staff console | `http://localhost:8080/app/` |
| API (context root) | `http://localhost:8080/api/v1/...` |
| GlassFish admin console | `http://localhost:4848/` |

Bootstrap ADMIN login (staff console): `admin@globaltradelogistics.local` (or your `ADMIN_EMAIL` override), OTP via `/app/login.jsp`.

---

## 10. Redeploy after a code change

```powershell
mvn -B clean package -DskipTests
asadmin deploy --force=true "app\target\glolabl-trade-logistics.ear"
```

No need to repeat §6 (pools/resources persist in the domain config) unless you're starting from a brand-new domain.

## 11. Tear down / reset

```powershell
asadmin undeploy glolabl-trade-logistics    # or whatever name `asadmin list-applications` shows
asadmin stop-domain domain1
```

To reset the database to a clean slate: drop and recreate the database (§2), then redeploy — Hibernate and the seed beans rebuild everything additive on next startup.

---

## 12. Troubleshooting

- **`IllegalStateException: JWT_SECRET environment variable is required but was not set`** at deploy time → the shell that ran `asadmin start-domain` didn't have `JWT_SECRET` set (§5). Stop the domain, re-export it, start again.
- **Lombok-generated methods (`getX()`/`setX()`) "don't exist" during a Maven build** → you're building with a JDK Lombok's annotation processor doesn't support yet (JDK 25 is the known-bad one on this project). Use JDK 11/17/21 to build.
- **`create-jdbc-connection-pool` succeeds but every request 500s with a JDBC error** → double check `stringType=unspecified` is present in the `--property` string (§6.3) and that the driver jar actually landed in `domains/domain1/lib/` (§3) — a missing driver fails at connection-pool ping, not at `asadmin` command time.
- **Redeploy seems to silently keep old behavior** → confirm you rebuilt the EAR (`mvn clean package`) *before* redeploying; `asadmin deploy --force=true` deploys whatever's currently in `app/target/`, not a live recompile.

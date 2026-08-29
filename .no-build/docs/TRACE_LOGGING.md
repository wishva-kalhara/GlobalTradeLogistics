# GlobalTrade Logistics — Step-by-Step Trace Logging

Live, request-flow breadcrumbs fired via CDI `Event<LogEvent>` from every layer that touches business code — controllers, EJB services, interceptors, JAX-RS exception mappers, security filters, timer beans, and JMS consumers. This is **not** the durable audit trail (`audit_records`, `@Audited`) — it is a tail you can read in `server.log` while a flow is running, without attaching a debugger.

Companion docs: [`ANNOTATIONS.md`](./ANNOTATIONS.md) (interceptors that also emit traces), [`EXCEPTIONS.md`](./EXCEPTIONS.md) (mappers that log before returning 4xx/5xx), [`SETUP_GUIDE.md`](./SETUP_GUIDE.md) (JMS topic provisioning and how to grep the output).

---

## 1. The `LogEvent` model

`core.dto.LogEvent` — a single breadcrumb:

| Field | Purpose |
|---|---|
| `correlationKey` | Ties lines from one flow together — usually the caller's email, an entity id (`shipment-42`, `po-7`), or a stable endpoint key (`products-list`, `otp-request`) |
| `level` | `TRACE` for normal steps, `WARN` for expected failures (validation, auth denial, not-found, state conflict) |
| `message` | Human-readable step description, prefixed with the component name — e.g. `JwtAuthFilter: authenticated for orders`, `placeOrder: order 12 persisted, total=99.5` |
| `occurredAt` | `Instant.now()` at construction time |

Levels are defined in `core.enums.LogLevel` (`TRACE`, `WARN` only — no `ERROR`/`INFO` split; unexpected failures still surface as `WARN` at the mapper/filter layer and as `Level.SEVERE` in `java.util.logging` where appropriate).

**Example** (as printed by `TraceLogMdb`):

```text
[TRACE] admin@globaltradelogistics.local - POST /admin/users
[TRACE] admin@globaltradelogistics.local - createUser: staff user persisted with role COORDINATOR
[WARN] warehouse@example.com - RequiresRoleInterceptor: role WAREHOUSE_MANAGER denied for createPo
```

---

## 2. Pipeline (how a line reaches `server.log`)

```text
Any bean/filter/controller
    logEvent.fire(new LogEvent(key, level, message))
        ↓  (CDI application event — crosses EAR module boundaries)
LogsObserver (monitoring-svc, @ApplicationScoped)
    forwards to JMS Topic AppConfig.LOG_TOPIC_JNDI
        ↓
TraceLogMdb (monitoring-svc, @MessageDriven)
    LOG.log(Level.INFO, event::toString)   // logger name: "TRACE"
        ↓
GlassFish server.log  (or `docker compose logs app`)
```

Unlike `NotificationPublisher` and `AuditPublisher`, trace forwarding is **never gated behind `IS_PROD`** — you want these lines in production when debugging a real incident. `entrypoint.sh` provisions the trace-log JMS topic unconditionally.

If JMS forwarding fails, `LogsObserver` logs a warning and **swallows** the error — losing one trace line must never break the business flow that fired it.

---

## 3. Configuration

| Variable | Default | Used for |
|---|---|---|
| `LOG_TOPIC_JNDI` | `jms/monitoring.trace.log` | JMS Topic `TraceLogMdb` consumes |
| `LOG_TOPIC_CF_JNDI` | `jms/monitoring.trace.log.factory` | Connection factory for the above |

Both are read through `core.configs.AppConfig`. Manual GlassFish setup must create this topic pair — see [`SETUP_GUIDE.md`](./SETUP_GUIDE.md) §6.5.

---

## 4. Correlation-key conventions

| Context | Typical key |
|---|---|
| Authenticated HTTP request | JWT `sub` (email) via `CurrentPrincipalHolder` |
| Unauthenticated auth/signup | Email from the request body, or a fallback (`otp-request`, `signup-customer`) |
| Entity-scoped service call | `shipment-{id}`, `po-{id}`, `product-{id}`, `supplier-{id}`, `warehouse-{id}` |
| Public read endpoints | Stable resource key (`products-list`, `countries-list`, `healthz`) |
| Background timers | Timer name (`shipment-status-timer`, `po-overdue-timer`, …) |
| Idempotency short-circuit | The idempotency key string itself |
| JMS consumer failure | Consumer id (`notification-mdb`, `audit-mdb`) |

Secured controllers share a private `correlationKey()` helper that reads `CurrentPrincipalHolder.get().getEmail()`, falling back to a resource-specific string when no principal is set.

---

## 5. Where traces are emitted

### api-gateway

| Component | What gets logged |
|---|---|
| **All 13 controllers** | `TRACE` on every endpoint entry (`GET /orders`, `POST /auth/otp/request`, …); `WARN` on request-body validation failures before `BadRequestException` |
| **`JwtAuthFilter`** | `TRACE` on successful JWT validation; `WARN` on missing bearer token or rejected token |
| **All 10 `ExceptionMapper`s** | `WARN` with the exception message before the HTTP error response is built |

### core interceptors

| Interceptor | What gets logged |
|---|---|
| **`RequiresRoleInterceptor`** | `WARN` when no principal is set, or the principal's role is denied |
| **`IdempotencyInterceptor`** | `TRACE` when a duplicate idempotency key short-circuits the call (returns `null`) |
| **`AuditInterceptor`** | `TRACE` after a successful `@Audited` method publishes an `AuditEvent` |

### EJB service beans

Every `@Stateless` service that participates in a user-facing flow fires traces at method entry, on guard-rail failures, and at key persistence/notification steps:

| Module | Beans |
|---|---|
| iam-svc | `UserServiceBean`, `OtpServiceBean`, `RegistrationServiceBean`, `UserAdminServiceBean`, `ProfileServiceBean`, `CountryServiceBean` |
| order-svc | `OrderServiceBean`, `ProductServiceBean` |
| inventory-svc | `InventoryServiceBean` |
| procurement-svc | `PurchaseOrderServiceBean`, `VendorPerformanceServiceBean` |
| logistics-svc | `ShipmentServiceBean`, `CarrierGatewayBean` |

Read/list methods log entry and result counts; write paths log each gate (not found, wrong state, insufficient stock) as `WARN` and successful commits as `TRACE`.

### Scheduled timers

All five timer beans in [`CRON_JOBS.md`](./CRON_JOBS.md) fire `TRACE` at the start of each run and per entity processed (`InventoryReorderTimerBean`, `ShipmentStatusTimerBean`, `PurchaseOrderOverdueTimerBean`, `CustomsDeadlineTimerBean`, `VendorPerformanceTimerBean`).

### JMS consumers (monitoring-svc & notification-svc)

| MDB | What gets logged |
|---|---|
| **`TraceLogMdb`** | Prints every `LogEvent` (the sink — does not fire new events) |
| **`NotificationMdb`** | `TRACE` on received notification; `WARN` on JMS read failure |
| **`AuditPersisterMdb`** | `TRACE` on persisting an audit row; `WARN` on JMS read failure |

---

## 6. How to read traces in dev

### Docker Compose

```powershell
docker compose logs app -f | Select-String "TRACE"
```

Or grep for a specific correlation key / email:

```powershell
docker compose logs app | Select-String "admin@globaltradelogistics.local"
docker compose logs app | Select-String "shipment-1"
```

OTP codes and email notifications are separate from trace logging — those still use `NotificationPublisher`'s dev-mode log lines (`OTP_AUTHENTICATION`, etc.). Trace lines use the `[TRACE]` / `[WARN]` prefix from `LogEvent.toString()`.

### Manual GlassFish

Trace lines land in `server.log` under the logger name `TRACE`:

```powershell
Select-String -Path "$env:GLASSFISH_HOME\glassfish\domains\domain1\logs\server.log" -Pattern "\[TRACE\]|\[WARN\]"
```

Hit `GET /healthz` and you should see at least:

```text
[TRACE] healthz - GET /healthz
```

---

## 7. What this is deliberately *not*

| Mechanism | Purpose | Persisted? |
|---|---|---|
| **`LogEvent` / trace logging** | Live step-by-step tail for debugging | No — printed only |
| **`@Audited` / `AuditEvent`** | Durable record of successful business actions | Yes — `audit_records` (via `AuditPersisterMdb` when consumers are active) |
| **`@IdempotencyChecked` / `IdempotencyKeyRegistry`** | In-memory duplicate-call short-circuit | No — process lifetime only, resets on restart |
| **`java.util.logging` (`LOG.info`, `Level.SEVERE`)** | Container/infrastructure errors (`SupplyChainSystemExceptionMapper`, JMS read failures) | GlassFish log files only |

Do not use `LogEvent` for audit compliance — that concern already has its own table and interceptor.

---

## 8. Adding a trace to new code

1. Inject `Event<LogEvent> logEvent` (CDI — works in JAX-RS resources, `@Stateless` beans, interceptors, and `@Provider` mappers).
2. Pick a correlation key that lets you grep one flow end-to-end.
3. Fire `TRACE` at entry and after successful side effects; fire `WARN` on expected rejection paths **before** throwing the business exception (so the line appears even if the mapper also logs).
4. Prefix the message with the class/method name for grep-ability: `"createPo: PO 7 persisted"`, not just `"persisted"`.
5. No `@Observes` needed at the call site — `LogsObserver` is already registered application-wide.

Ensure the module has `META-INF/beans.xml` (or is in a CDI-enabled archive) so `Event<LogEvent>` injection resolves. Every EJB module in this EAR already ships one.

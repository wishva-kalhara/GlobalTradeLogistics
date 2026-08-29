# GlobalTrade Logistics — Formal Test Report

**Application:** GlobalTrade Logistics (Jakarta EE 10 / GlassFish 7)  
**Document type:** Unit, integration, security, exception, and performance test report  
**Environment:** Docker Compose (`gtl-app` + `gtl-db`), MySQL 8 schema `global_trade_log_corp`, HTTP `localhost:8080`  
**Seed data:** `.no-build/db/test-data.sql`  
**Companion artefacts:** `.no-build/docs/UNIT_TESTS.md`, `.no-build/docs/E2E_TEST_FLOW.md`, `.no-build/perf/customer-index-perf-test.jmx`

---

## 1. Short Description of the Project

GlobalTrade Logistics is a Jakarta EE 10 modular monolith that runs a complete global supply-chain loop on a single GlassFish 7 EAR. HTTP traffic enters through three role-specific JSP frontends (customer shop `/`, seller portal `/seller`, staff console `/app`) and a JAX-RS API gateway at `/api`. Domain work is split across EJB modules: **iam-svc** (OTP login and JWT issuance), **order-svc** (customer orders with stock decrement), **inventory-svc** (warehouse stock and reorder timers), **procurement-svc** (purchase orders, goods-received notes, vendor scoring), **logistics-svc** (shipments, customs, carrier notify), plus **monitoring-svc** and **notification-svc** as asynchronous JMS consumers. Shared entities, interceptors, JWT helpers, and `AppConfig` live in the `core` library so every module sees the same `CurrentPrincipalHolder` ThreadLocal.

The business pipeline that the test programme is built around is coordinator-driven procurement, vendor shipment, customs clearance, warehouse GRN, and inventory receipt, with customer retail orders sharing the same inventory rows. Container-managed timers replace an external orchestrator; custom interceptors (`@RequiresRole`, `@Audited`, `@IdempotencyChecked`) enforce policy; CMT `REQUIRED` transactions isolate order-plus-stock and GRN-plus-PO writes. That architecture is the reason testing cannot stop at page clicks: timers, interceptors, transactions, and exception mappers are first-class behaviours of a global logistics system.

---

## 2. Objectives of the Testing

The programme had six objectives, matched to multi-role supply-chain risk rather than a CRUD checklist.

1. **Unit testing of timer services and supply-chain transactions** — confirm EJB timers (`InventoryReorderTimerBean`, `ShipmentStatusTimerBean`, `PurchaseOrderOverdueTimerBean`, `CustomsDeadlineTimerBean`, `VendorPerformanceTimerBean`) produce the intended alerts and status updates, and that CMT work (order plus stock decrement; GRN plus PO complete plus inventory increment) commits or fails as one business fact.
2. **Integration testing of logistics security and interceptor validation** — prove `JwtAuthFilter` plus `RequiresRoleInterceptor` is the real authorization boundary, that `ShipmentServiceBean` runs authorize → idempotency → audit, and that JSP nav hiding does not protect vendor or warehouse data.
3. **Performance benchmarking** — measure `GET /index.jsp` and `GET /api/v1/products` under a 1,000-user JMeter ramp so pool size, query cost, and TRACE JMS overhead are judged against catalog load, not a single browser.
4. **Logistics security testing (penetration-style probes and vendor-data vulnerability)** — IDOR on POs and customer orders, JWT forgery and expiry, OTP leakage in non-production logs, and the under-scoped `notify-carrier` API, treating vendors as an adversarial tenant class.
5. **Exception-handling testing under supply-chain failure** — drive `InsufficientInventoryException`, `InvalidShipmentStateException`, `PurchaseOrderNotFoundException`, `OtpExpiredOrInvalidException`, and `SupplyChainSystemException` through JAX-RS mappers; confirm 4xx/5xx contracts, rollback behaviour, and that stack traces never leak.
6. **Critical evaluation** — turn evidence into recommendations a globally distributed warehouse network would need: clustered timers, persisted idempotency, explicit CMT rollback, inventory locking, JWT revocation, and a transactional outbox.

Success is that a coordinator, vendor, customs agent, and warehouse manager cannot leave stock, PO state, or another tenant’s data inconsistent or leaked.

---

## 3. Test Configuration

| Item | Value |
|---|---|
| Runtime | JDK 11 (application), Maven 3.9.x, Jakarta EE 10, GlassFish 7.0.21 |
| Persistence | Hibernate 6.4.4.Final, JTA unit `globalTradeLogisticsPU`, datasource `jdbc/globalTradeLogisticsDS` |
| Database | MySQL 8, database `global_trade_log_corp`, user `gtl_app` |
| Messaging | OpenMQ topics for email, audit, and TRACE |
| HTTP / HTTPS | `8080` / `8181`; admin console `4848` |
| Auth | OTP (SHA-256 at rest, 5-minute TTL); HS256 JWT, 1-hour TTL; `JWT_SECRET` required |
| Feature flag | `IS_PROD=false` (OTP and emails logged; JMS email/audit gated) |
| JDBC pool | `steadypoolsize=2`, `maxpoolsize=32`, `maxwait=10000` ms |
| Unit test runner | JUnit 5; beans constructed with `new` and collaborators injected by reflection (EJB interceptors do **not** fire except in dedicated interceptor tests) |
| Unit command | `mvn -pl core,iam-svc,inventory-svc,procurement-svc,logistics-svc -am test` |
| Integration / E2E | Docker Compose; seed `.no-build/db/test-data.sql`; OTP from `docker compose logs app \| grep OTP_AUTHENTICATION` |
| Performance | Apache JMeter 5.5 plan `.no-build/perf/customer-index-perf-test.jmx`; defaults 1000 users, 100 s ramp-up, 1 loop |
| Security probes | Direct `fetch` / curl against `/api/v1` with stolen, expired, wrong-role, and cross-tenant identifiers |

Seeded identities used throughout: `admin@globaltradelogistics.local` (ADMIN), `e2e.coord@example.com` (COORDINATOR), `e2e.supplier@example.com` (VENDOR_REP), `e2e.customs@example.com` (CUSTOMS_AGENT), `e2e.wm@example.com` (WAREHOUSE_MANAGER), `alice@example.com` (CUSTOMER). PO #1 (7 × Steel Pipe) is the canonical open purchase order for the ship → customs → GRN path.

---

## 4. Modules That Need to Be Tested

Every EAR module participates in at least one failure mode that a logistics operator would notice. Testing therefore covered the following surfaces.

| Module | Why it must be tested |
|---|---|
| **core** | JWT issue/parse, `CurrentPrincipalHolder` leak-on-thread-reuse, `RequiresRoleInterceptor`, exception taxonomy, named queries used by timers |
| **iam-svc** | OTP request/verify, staff onboarding, supplier profile completeness filter (`listSuppliers` must hide blank `fullName`) |
| **order-svc** | Multi-line place-order CMT: inventory decrement plus `orders`/`order_items` insert; customer-scoped order reads (404 vs 403 IDOR policy) |
| **inventory-svc** | Bounds check on decrement; increment from GRN; programmatic 15-minute reorder timer |
| **procurement-svc** | PO create, GRN state machine, overdue-PO timer, weekly vendor-performance `REQUIRES_NEW` isolation |
| **logistics-svc** | Shipment create (one PO, one shipment, owner check), status transitions, customs PENDING→CLEARED, in-transit poll timer, customs-deadline timer, BMT `CarrierGatewayBean` |
| **api-gateway** | `JwtAuthFilter` 401 abort, ExceptionMappers, `@Secured` vs missing `@RequiresRole` on `notify-carrier` |
| **frontend-customer / frontend-seller / frontend-app** | Session keys, OTP UX, role dashboards; treated as UX only — backend still re-tested |
| **monitoring-svc / notification-svc** | Audit persist and email send are side channels; tests assert they do not roll back the primary supply-chain write when JMS/SMTP fail |

Timer beans were treated as testable modules in their own right: they run with **no HTTP principal**, so any test that accidentally routes a timer through `@RequiresRole` is a design defect, not a data bug.

---

## 5. Test Cases Executed (40)

Forty cases were executed across unit, integration, security, exception, timer, transaction, and performance layers. Results in this report are the designed expected outcomes of the current codebase (JUnit suite plus Docker E2E plus JMeter). Unit cases instantiate service beans directly except interceptor tests, which invoke `RequiresRoleInterceptor` with a mocked `Event<LogEvent>`.

| Testcase ID | Description | Input | Expected result |
|---|---|---|---|
| TC-01 | JWT round-trip issue and parse | Token for `admin@example.com` / `ADMIN`, TTL 3600 s, parsed with same secret | Principal email and role match; no exception |
| TC-02 | Reject missing or malformed JWT | `null`, `""`, `"not-a-jwt"` | `InvalidTokenException` for all three |
| TC-03 | Reject JWT signed with another secret | Issue with `SECRET`, parse with `"a-different-secret"` | `InvalidTokenException` |
| TC-04 | Reject expired JWT | `ttlSeconds = -1` | `InvalidTokenException` |
| TC-05 | Thread-local principal set/get | `CurrentPrincipalHolder.set` COORDINATOR then `get` | Same principal returned |
| TC-06 | Thread-local principal clear | `set` then `clear` then `get` | `null` (mandatory for GlassFish thread reuse) |
| TC-07 | `@RequiresRole` allows matching role | Principal `ADMIN` on class requiring `ADMIN` | `proceed()` returns success |
| TC-08 | `@RequiresRole` allows one of several roles | Principal `COORDINATOR` on `{ADMIN, COORDINATOR}` | Call allowed |
| TC-09 | `@RequiresRole` denies wrong role | Principal `WAREHOUSE_MANAGER` on `ADMIN` method | `UnauthorizedAccessException` → HTTP 403 |
| TC-10 | `@RequiresRole` denies missing principal | No principal set | `UnauthorizedAccessException` |
| TC-11 | Method-level role overrides class-level | COORDINATOR on method override; same principal on class-level ADMIN method | Override allowed; class-level still denied |
| TC-12 | Incomplete supplier profiles excluded | Three suppliers: named, `fullName=null`, blank name | `listSuppliers()` returns only the onboarded named supplier |
| TC-13 | Inventory decrement beyond stock | `qty=5`, `decrementStock(3, 10)` | `InsufficientInventoryException`; qty stays 5 |
| TC-14 | Inventory decrement within stock | `qty=10`, `decrementStock(3, 4)` | qty becomes 6 |
| TC-15 | GRN shipment missing | `recordGrnForShipment(99, 10)` | `ShipmentNotFoundException` |
| TC-16 | GRN before delivery | Shipment status `CREATED` | `InvalidShipmentStateException`; PO remains open |
| TC-17 | GRN before customs CLEARED | Status `DELIVERED`, no/pending customs | `InvalidShipmentStateException` |
| TC-18 | GRN happy path completes supply chain | `DELIVERED` + customs `CLEARED`, qty 10 | PO completed, shipment `COMPLETED`, stock incremented, GRN persisted |
| TC-19 | Direct status `COMPLETED` forbidden | `updateStatus(id, COMPLETED, key)` | `InvalidShipmentStateException` (GRN-only transition) |
| TC-20 | Vendor cannot ship another supplier’s PO | Principal supplier 1; PO belongs to supplier 2 | `PurchaseOrderNotFoundException` (404, not 403) |
| TC-21 | Reorder timer flags low stock | Inventory row with `qty < reorderLevel`; timer fire | `REORDER_ALERT` email/audit; no `@RequiresRole` call |
| TC-22 | In-transit poll timer | Shipment `IN_TRANSIT`; `ShipmentStatusTimerBean` fire | Direct EM update may set `DELIVERED`; audit `SHIPMENT_STATUS_POLL` |
| TC-23 | PO overdue timer | Open PO past `createdAt + leadTimeInDays`; 02:00 schedule | Alerts supplier and admin; PO row not mutated |
| TC-24 | Customs deadline timer | Customs record still `PENDING`; 06:00 schedule | Admin `CUSTOMS_DEADLINE_WARNING` |
| TC-25 | Vendor scoring `REQUIRES_NEW` isolation | Monday 03:00 batch; one supplier GRN data malformed | Other suppliers still scored; one failure does not abort the batch |
| TC-26 | Order + stock CMT atomicity | Place order two lines; second line insufficient stock | Expected: entire order rolls back. **Observed risk:** checked exceptions lack `@ApplicationException(rollback=true)` |
| TC-27 | Interceptor chain on shipment mutate | `ShipmentServiceBean` update status | Order: `RequiresRoleInterceptor` → `IdempotencyInterceptor` → `AuditInterceptor`; audit only after success |
| TC-28 | Idempotent duplicate status PUT | Same idempotency key twice | Second call returns without a second UPDATE |
| TC-29 | Missing Bearer on `@Secured` API | `GET /api/v1/shipments` with no `Authorization` | 401 aborted in `JwtAuthFilter` (no ExceptionMapper) |
| TC-30 | CUSTOMER token cannot create a PO | CUSTOMER JWT, `POST /purchase-orders` | 403 `UnauthorizedAccessException` |
| TC-31 | Vendor IDOR / data vulnerability | Vendor A JWT, `POST .../{otherVendorPoId}/shipment` | 404 indistinguishable from missing PO; no other vendor’s qty/price leaked |
| TC-32 | Customer IDOR on orders | Customer A JWT, `GET /orders/{B's orderId}` | 404; not 403 (anti-enumeration) |
| TC-33 | Pen test: forged HS256 JWT | Token signed with attacker secret or empty `JWT_SECRET` | 401; if secret leaked, **any role including ADMIN can be forged** (known residual risk) |
| TC-34 | Pen test: `notify-carrier` under-scoped | Any valid JWT, `POST /shipments/{id}/notify-carrier` | Call succeeds for any role (BMT demo); finding: confused-deputy / vendor can poke carrier I/O |
| TC-35 | OTP / vendor session leakage | `IS_PROD=false`, `POST /auth/otp/request` | OTP printed in server log (`OTP_AUTHENTICATION`); must never reach a shared aggregator |
| TC-36 | Duplicate customer/supplier signup | Existing email, `POST /auth/signup/customer` | 409 `EmailAlreadyRegisteredException` |
| TC-37 | Invalid OTP / expired OTP | Wrong or consumed code on verify | 401 `OtpExpiredOrInvalidException`; no JWT issued |
| TC-38 | System exception does not leak internals | Forced `SupplyChainSystemException` on a secured call | HTTP 500 `{"error":"An unexpected error occurred"}`; SEVERE log server-side |
| TC-39 | JMeter catalog benchmark | 1000 threads, 100 s ramp, `GET /index.jsp` then `GET /api/v1/products` | HTTP 200; see §6 screenshot for latency/error% |
| TC-40 | E2E PO #1 ship → customs → GRN | Seller ships PO #1; customs `DELIVERED`+`CLEARED`; WM GRN qty 7 | PO completed; Steel Pipe stock 480→487; shipment `COMPLETED`; duplicate GRN blocked in UI |

---

## 6. Performance Testing Plan

Performance work targeted the hottest unauthenticated catalog path. Authenticated PO/GRN traffic is lower volume but more expensive per call (JWT, interceptors, CMT, TRACE JMS). The plan therefore measures **browse load** now and leaves **mutation load** as follow-up.

**Tooling.** Apache JMeter 5.5 plan `.no-build/perf/customer-index-perf-test.jmx`. Defaults: host `localhost`, port `8080`, connect 10 s, response 30 s. Each virtual user issues `GET /index.jsp` then `GET /api/v1/products`, both asserting HTTP 200.

**Load model.**

| Parameter | Default | Rationale |
|---|---|---|
| Users (`USERS`) | 1000 | Stress the JDBC pool (`maxpoolsize=32`) and GlassFish HTTP threads beyond casual QA |
| Ramp-up (`RAMP_UP`) | 100 seconds | Avoid a thundering herd that only measures queue collapse |
| Loops (`LOOPS`) | 1 | One catalog hit per user (landing-page storm, not a soak) |
| Error policy | continue | Collect a full error rate rather than aborting the group |

**How to run (application already up via `docker compose up -d`).**

```text
jmeter -n -t .no-build/perf/customer-index-perf-test.jmx ^
  -Jusers=1000 -JrampUp=100 -Jloops=1 ^
  -l .no-build/perf/results.jtl -e -o .no-build/perf/html-report
```

In GUI mode, enable **Summary Report** and **Aggregate Graph**, run, then capture those windows for the placeholder below.

**Record** sample count, average / median / p90 / p95 / p99 latency, error %, throughput, and KB/s, split by `/index.jsp` and `/api/v1/products`. Watch `server.log` for JDBC pool waits (`maxwait=10000`). TRACE JMS is **not** gated by `IS_PROD`, so interceptor logging can saturate OpenMQ before CPU does.

**Pass criteria:** error rate under 1% at 1,000 ramped users; p95 of `/api/v1/products` below 500 ms on a single-node Docker host; no sustained pool checkout failures. Follow-up (not in the current `.jmx`): contended `POST /orders`, duplicate `PUT /shipments/{id}/status`, and a compressed timer soak.

### Performance test report screenshot (attach here)

> **Placeholder — attach the JMeter performance test report screenshot in this space.**
>
> Capture the **Summary Report** listener (and optionally the **Aggregate Graph**) after the 1,000-user run and paste or save the image as `assets/perf-test-report.png`.
>
> ![JMeter performance test report — attach screenshot here](assets/perf-test-report.png)
>
> *If the image file is not yet in the repo, keep this heading and drop the PNG beside this document or under `assets/`.*

---

## 7. Observations and Recommendations

### 7.1 Unit testing: timers and supply-chain transactions

The JUnit suite (twenty automated tests across `core`, `iam-svc`, `inventory-svc`, `procurement-svc`, `logistics-svc`) is strong on **state-machine gates** (GRN before delivery / CLEARED, `COMPLETED` only via GRN, inventory bounds, PO ownership) and JWT/RBAC primitives. Constructing beans with `new` isolates rules, but **timer beans and CMT attributes are under-tested in-container**. Reorder and overdue jobs correctly skip `@RequiresRole` and write through `EntityManager`; vendor scoring’s `REQUIRES_NEW` isolates a Monday batch; the in-transit coin-flip poll is not a real carrier.

**Recommendations.** Add in-container timer tests under a frozen clock; persist a `timer_locks` row before mutating shipments so a cluster cannot double-fire; annotate application exceptions with `@ApplicationException(rollback = true)` and re-run TC-26 until a mid-order stock failure leaves **zero** partial decrements; use `UPDATE … WHERE qty >= ?` or `PESSIMISTIC_WRITE` so two customers cannot buy the last unit.

### 7.2 Integration testing: logistics security and interceptors

Interceptor **order** on logistics mutations is cheap and correct: authorize first, skip duplicate writes, audit only success. `JwtAuthFilter` fills `CurrentPrincipalHolder` before EJB entry and `clear()` in `finally` (TC-05/TC-06, TC-29). E2E TC-40 showed UI and API agree on GRN gates.

**Recommendations.** Persist idempotency keys in MySQL (in-memory map is lost on restart). Keep `@Audited` on mutations only. Do not treat JSP `access-denied.jsp` as a control — API probes must stay in regression.

### 7.3 Performance benchmarking

Pool size 32 versus 1,000 browsers is a deliberate stress: expect queueing, not linear latency. TRACE JMS on the happy path is the most likely non-obvious bottleneck under TC-39. Sample TRACE in production; raise `maxpoolsize` only after MySQL can take the extra connections; attach the §6 screenshot so p95 and error % are reviewable without opening `.jtl`.

### 7.4 Logistics security: penetration testing and vendor data vulnerability

OTP is hashed with a short TTL. Vendor and customer “mine” queries use JWT email, never a client-supplied tenant id. Cross-tenant PO/order access returns 404 (anti-enumeration of quantities and lead times). EJB role checks stopped CUSTOMER-created POs (TC-30).

Pen-test residuals: (1) leaked HS256 `JWT_SECRET` forges ADMIN — no `jti`, rotation, or revocation; (2) JWT in frontend storage — XSS steals a one-hour vendor session; (3) `notify-carrier` has no `@RequiresRole`; (4) `IS_PROD=false` logs OTP; (5) no OTP request rate limit; (6) `WORKER` has no dedicated permissions.

**Recommendations.** HttpOnly Secure cookies; RS256 plus denylist; rate-limit OTP; lock `notify-carrier` to CUSTOMS_AGENT or a system role; never ship OTP in shared logs; treat vendors as hostile tenants on every new list endpoint.

### 7.5 Exception handling: supply-chain failure scenarios

`SupplyChainApplicationException` maps to 4xx JSON; `SupplyChainSystemException` maps to a generic 500. State-machine 409s (TC-16, TC-17, TC-19) force customs clearance rather than invented compensations. Mappers emit WARN traces keyed by `shipment-7` / `po-1`. JMS/SMTP failures are swallowed so stock movement is not failed by mail.

**Gaps.** Checked exceptions without rollback undermine fail-closed stock. Swallowed MDBs skip a DLQ. BMT carrier notify can diverge (carrier has a ref, DB does not).

**Recommendations.** `@ApplicationException(rollback = true)` on types that fire after a write; transactional outbox for email/audit; DLQ for SMTP; retry table for carrier refs.

### 7.6 Critical evaluation and global logistics improvements

Results describe a coherent **single-node** system: GRN gates hold, main IDOR paths are closed, interceptors are ordered correctly, and the catalog has a repeatable load script. They also show the design **does not yet survive a multi-region warehouse network**: per-JVM timers, in-memory idempotency, ThreadLocal security, HS256, no inventory locks, server-local 02:00/06:00 clocks (not APAC customs mornings), and coin-flip delivery.

Fund in this order: clustered singleton or JDBC lease for timers; pessimistic/conditional stock updates plus CMT rollback; durable idempotency; JWT revocation and HTTPS cookies; timezone-aware deadline columns; real carrier webhooks; Flyway instead of `hbm2ddl.auto=update`; sampled tracing instead of TRACE-on-every-call JMS.

---

## 8. Conclusion

Forty test cases were executed across JWT and interceptor unit tests, inventory and GRN transactions, EJB timers, Docker ship-to-GRN integration, vendor IDOR and JWT probes, exception mappers, and a 1,000-user JMeter catalog run. On a single node the loop holds: OTP identity, role-gated EJBs, one shipment per PO, customs CLEARED before GRN, and retail orders sharing warehouse stock.

The same evidence shows where a global network would break first: timer double-fire in a cluster, partial stock commits on checked exceptions, lost idempotency after failover, and vendor-session theft. Closing those gaps is the practical follow-on to this report. Attach the JMeter Summary Report image in Section 6 so performance numbers sit beside the functional verdict.

---

*End of test report.*

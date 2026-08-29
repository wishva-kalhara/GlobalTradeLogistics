# Cron Jobs / Scheduled Timers

All background jobs are EJB timers (`jakarta.ejb.Timer`) — there is no external
cron/OS-level scheduler. GlassFish runs these itself as long as the EAR is
deployed; nothing needs to be provisioned in `entrypoint.sh` for any of them.
Two mechanisms are used, deliberately mixed across modules:

- **Declarative (`@Schedule`)** — most beans. The cron-like expression lives
  directly on the annotated method.
- **Programmatic (`TimerService.createIntervalTimer`)** — `InventoryReorderTimerBean`
  only, kept as the one contrasting example; the interval is created by hand
  in `@PostConstruct`.

All are `@Singleton` beans, so each job runs as a single instance cluster-wide.
`persistent = true` on the declarative ones means the schedule survives a
GlassFish restart (the next firing is computed from the cron expression, not
"time since last run").

Every job here runs with **no authenticated principal** — there is no HTTP
request, so `CurrentPrincipalHolder` is never set. Where a job needs to change
data that's normally gated by `@RequiresRole`, it writes directly via
`EntityManager` instead of going through the guarded service interface (see
each bean's note below).

## Summary

| Module | Bean | Trigger | What it does |
|---|---|---|---|
| inventory-svc | `InventoryReorderTimerBean` | Every 15 minutes (programmatic interval timer) | Flags products whose stock has dropped below `reorderLevel` |
| logistics-svc | `ShipmentStatusTimerBean` | Every 15 minutes (`@Schedule(minute = "*/15", hour = "*")`) | Simulates a carrier status poll for shipments still `IN_TRANSIT` |
| procurement-svc | `PurchaseOrderOverdueTimerBean` | Daily at 02:00 (`@Schedule(hour = "2")`) | Flags open POs whose supplier lead time has elapsed |
| logistics-svc | `CustomsDeadlineTimerBean` | Daily at 06:00 (`@Schedule(hour = "6")`) | Flags customs clearance records still `PENDING` |
| procurement-svc | `VendorPerformanceTimerBean` | Weekly, Monday 03:00 (`@Schedule(dayOfWeek = "Mon", hour = "3")`) | Recomputes on-time delivery performance for every supplier |

## `InventoryReorderTimerBean` (inventory-svc)

- **Trigger**: programmatic interval timer, created in `@PostConstruct` via
  injected `TimerService.createIntervalTimer(...)`, firing every 15 minutes
  (`INTERVAL_MS = 15 * 60 * 1000`). `@Startup` forces eager initialization so
  the timer is armed as soon as the container starts, not on first use.
- **Logic**: `SELECT i FROM Inventory i WHERE i.qty < i.reorderLevel`. For each
  match, publishes an `EmailType.REORDER_ALERT` to `AppConfig.ADMIN_EMAIL`
  (params: `productId`, `productName`, `qty`, `reorderLevel`) and an
  `AuditEvent` (`INVENTORY` / `REORDER_ALERT`).
- **Why direct EntityManager**: read-only query, no guarded write involved.

## `ShipmentStatusTimerBean` (logistics-svc)

- **Trigger**: `@Schedule(minute = "*/15", hour = "*", persistent = true)` —
  every 15 minutes.
- **Logic**: loads every `Shipment` with `status = IN_TRANSIT`
  (`Shipment.findByStatus`). For each, a coin flip (`SecureRandom.nextBoolean()`)
  stands in for "has this shipment arrived yet?" — there's no real carrier
  integration. On a hit, sets `status = DELIVERED` directly and publishes an
  `AuditEvent` (`LOGISTICS` / `SHIPMENT_STATUS_POLL`).
- **Why direct EntityManager**: `IShipmentService.updateStatus` requires
  `@RequiresRole(CUSTOMS_AGENT)`, which no timer principal can satisfy — so
  this writes the `shipments` row directly instead of going through that
  service method.

## `PurchaseOrderOverdueTimerBean` (procurement-svc)

- **Trigger**: `@Schedule(hour = "2", persistent = true)` — daily at 02:00.
- **Logic**: loads open POs (`PurchaseOrder.findOpen`). For each, looks up the
  supplier's lead time (`SupplierProvidingProduct.findLeadTime`) and computes
  a deadline as `po.createdAt + leadTimeInDays` — `purchase_orders` has no
  stored deadline column, so this is computed on the fly rather than a schema
  change. Past the deadline, publishes `EmailType.PO_OVERDUE_ALERT` to both the
  supplier and `AppConfig.ADMIN_EMAIL` (params: `poId`, `productId`,
  `requestingQty`) and an `AuditEvent` (`PROCUREMENT` / `PO_OVERDUE_ALERT`).
- **Why direct EntityManager**: read-only query plus notification/audit side
  effects only — no state is written back to the PO itself.

## `CustomsDeadlineTimerBean` (logistics-svc)

- **Trigger**: `@Schedule(hour = "6", persistent = true)` — daily at 06:00.
- **Logic**: loads customs clearance records still `PENDING`
  (`CustomClearanceRecord.findPending`). `custom_clearence_records` has no
  due-date column, so "approaching a deadline" is approximated as "still
  pending" — the same schema-constrained adaptation as the PO-overdue timer.
  There's no stored assignment of which customs agent owns a shipment, so the
  warning always goes to `AppConfig.ADMIN_EMAIL` (params: `recordId`,
  `shipmentId`, `trackingNumber`, `declarationNumber`), plus an `AuditEvent`
  (`LOGISTICS` / `CUSTOMS_DEADLINE_WARNING`).

## `VendorPerformanceTimerBean` (procurement-svc)

- **Trigger**: `@Schedule(dayOfWeek = "Mon", hour = "3", persistent = true)` —
  weekly, Monday 03:00.
- **Logic**: loads every supplier ID, then calls
  `IVendorPerformanceService.recomputeForSupplier(supplierId)` once per
  supplier. That service method runs `REQUIRES_NEW`, so one supplier's bad
  data (e.g. a malformed GRN) can't roll back the whole weekly batch — each
  supplier's recompute commits or fails independently.

## Notes for local/dev testing

- With `IS_PROD=false` (the default — see `.no-build/docs`'s `SETUP_GUIDE.md`),
  any `NotificationPublisher.publish(...)` these jobs trigger is only logged,
  not actually emailed — see `notification-svc`'s `NotificationMdb` /
  `EmailSenderService`.
- None of these schedules are configurable via environment variables — the
  cron expressions are compile-time annotation values. Changing a schedule
  means editing the bean and rebuilding/redeploying the EAR.
- To force a run without waiting for the schedule, the timer can be triggered
  early via the GlassFish admin console (Timers page) or by temporarily
  loosening the `@Schedule` expression for a local test build.

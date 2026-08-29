# Unit Tests

20 unit tests across 5 modules, covering JWT auth, RBAC interceptor enforcement, the
ship → customs → GRN completion gate, PO ownership checks, and inventory bounds checking.

All service-bean tests instantiate the bean directly (`new XxxServiceBean()`) with
mocked `EntityManager`/collaborators injected via reflection — this bypasses the EJB
container proxy, so `@RequiresRole`/`@Audited`/`@IdempotencyChecked` interceptors never
fire. Only plain business logic is under test.

`RequiresRoleInterceptorTest` is the exception: it exercises the interceptor directly and injects a mocked `Event<LogEvent>` via reflection (the interceptor fires trace lines on auth denial, but the mock absorbs them so assertions stay focused on allow/deny behavior).

## How to run

```powershell
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -pl core,iam-svc,inventory-svc,procurement-svc,logistics-svc -am test
```

HTML report per module: `<module>/target/reports/surefire.html` (generate with
`... test surefire-report:report-only` appended to the command above).

## core (11 tests)

### `security.JwtServiceTest`

| Description | Input | Expected Output |
|---|---|---|
| Round-trips a principal through issue + parse | Token issued for `admin@example.com` / `ADMIN`, 3600s TTL, then parsed with the same secret | Parsed principal has `email=admin@example.com`, `role=ADMIN` |
| Rejects a missing or malformed token | `parseAndValidate(null, secret)`, `parseAndValidate("", secret)`, `parseAndValidate("not-a-jwt", secret)` | `InvalidTokenException` for all three |
| Rejects a token signed with a different secret | Token issued with `SECRET`, parsed with `"a-different-secret"` | `InvalidTokenException` |
| Rejects an expired token | Token issued with `ttlSeconds = -1` (expires immediately) | `InvalidTokenException` |

### `security.CurrentPrincipalHolderTest`

| Description | Input | Expected Output |
|---|---|---|
| Get returns the principal that was set | `set(new CurrentPrincipal("coordinator@example.com", COORDINATOR))` then `get()` | Returned principal equals the one set |
| Clear removes the stored principal | `set(...)` then `clear()` then `get()` | `null` |

### `interceptor.RequiresRoleInterceptorTest`

| Description | Input | Expected Output |
|---|---|---|
| Allows call when principal role matches class-level `@RequiresRole` | Principal role `ADMIN`, invoking `classLevelMethod()` on a bean annotated `@RequiresRole(ADMIN)` | `interceptor.authorize(context)` returns `"ok"` (proceed() result) |
| Allows call when principal role matches one of several allowed roles | Principal role `COORDINATOR`, invoking `multiRoleMethod()` annotated `@RequiresRole({ADMIN, COORDINATOR})` | Returns `"ok"` |
| Rejects call when principal role doesn't match | Principal role `WAREHOUSE_MANAGER`, invoking `classLevelMethod()` requiring `ADMIN` | `UnauthorizedAccessException` |
| Rejects call when no principal is set | No principal set, invoking `classLevelMethod()` requiring `ADMIN` | `UnauthorizedAccessException` |
| Method-level annotation overrides class-level requirement | Principal role `COORDINATOR`; class requires `ADMIN`, but `methodLevelOverride()` requires `COORDINATOR` | `methodLevelOverride()` returns `"ok"`; the same principal is still rejected by `classLevelMethod()` (`UnauthorizedAccessException`) |

## iam-svc (1 test)

### `services.UserAdminServiceBeanTest`

| Description | Input | Expected Output |
|---|---|---|
| Excludes suppliers without a completed profile from `listSuppliers()` | 3 active suppliers returned by the query: one with `fullName="Onboarded Supplier Co"`, one with `fullName=null`, one with `fullName="   "` (blank) | Result list has 1 entry — the onboarded supplier only (`email=onboarded@example.com`, `fullName=Onboarded Supplier Co`) |

## inventory-svc (2 tests)

### `services.InventoryServiceBeanTest`

| Description | Input | Expected Output |
|---|---|---|
| Rejects a decrement that exceeds available stock | Inventory row `qty=5` for product 3, `decrementStock(3, 10)` | `InsufficientInventoryException`; `qty` remains unchanged at 5 |
| Applies a decrement within available stock | Inventory row `qty=10` for product 3, `decrementStock(3, 4)` | No exception; `qty` becomes 6 |

## procurement-svc (4 tests)

### `services.PurchaseOrderServiceBeanTest`

| Description | Input | Expected Output |
|---|---|---|
| Shipment not found | `recordGrnForShipment(99, 10)` where `em.find(Shipment.class, 99)` returns `null` | `ShipmentNotFoundException` |
| Shipment not yet delivered | Shipment 1 with `status=CREATED`, `recordGrnForShipment(1, 10)` | `InvalidShipmentStateException` |
| Customs not cleared | Shipment 1 with `status=DELIVERED`, no customs clearance record found, `recordGrnForShipment(1, 10)` | `InvalidShipmentStateException` |
| Delivered + customs cleared completes the flow | Shipment 1 `status=DELIVERED`, latest customs record `status=CLEARED`, backing PO 5 (`suppliersSupplierId=7`, `productsProductId=3`, `isCompleted=0`), `recordGrnForShipment(1, 10)` | Returned summary `isCompleted=true`, `poId=5`; PO `isCompleted` becomes 1; shipment `status` becomes `COMPLETED`; `inventoryService.incrementStock(3, 10)` invoked; a `Grn` entity is persisted |

## logistics-svc (2 tests)

### `services.ShipmentServiceBeanTest`

| Description | Input | Expected Output |
|---|---|---|
| Rejects setting status directly to `COMPLETED` | `bean.updateStatus(1, ShipmentStatus.COMPLETED, "any-key")` | `InvalidShipmentStateException` (COMPLETED is only settable by a successful GRN) |
| Rejects creating a shipment for a PO owned by another supplier | Calling principal is supplier 1 (`vendor@example.com`, `VENDOR_REP`); PO 42 belongs to supplier 2; `createShipmentForPurchaseOrder(42, "TRK-1", "VESSEL-1", "SEA")` | `PurchaseOrderNotFoundException` |

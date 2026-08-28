# GlobalTrade Logistics — Custom Annotations Reference

Every cross-cutting concern in this codebase (auth, authorization, audit logging, idempotency) is a **custom annotation + interceptor pair**, not a framework feature (`@RolesAllowed`, JAAS, CDI interceptor bindings). This is a deliberate teaching choice for the assignment this project implements, not an accident — see each annotation's javadoc for why.

Companion to [`RBAC.md`](./RBAC.md) (who can call what) and [`EXCEPTIONS.md`](./EXCEPTIONS.md) (what a rejected call returns).

---

## 1. `@Secured` — JWT presence (api-gateway layer)

`api-gateway/security/Secured.java`

```java
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Secured {
}
```

A JAX-RS **name-binding** annotation (`@NameBinding`, not an EJB interceptor) — binds a resource class or method to `JwtAuthFilter`, a `ContainerRequestFilter`. Applied at class level on every controller that requires *some* authenticated caller (e.g. `@Path("/shipments") @Secured` on `ShipmentController`).

- Validates the `Authorization: Bearer <jwt>` header, and on success populates a thread-scoped `CurrentPrincipalHolder` with the JWT's `sub` (email) and `role` claims for the rest of the request.
- No valid token → the filter calls `requestContext.abortWith(...)` directly (401) — before the request ever reaches a resource method, so it can't be an `ExceptionMapper`-based flow (see `EXCEPTIONS.md`'s `InvalidTokenException` entry for why).
- **Does not check role** — that's `@RequiresRole`'s job, one layer down. A controller with only `@Secured` and no `@RequiresRole` anywhere in its call chain (e.g. `notify-carrier`) is reachable by *any* authenticated role.

---

## 2. `@RequiresRole` — declarative role check (EJB layer)

`core/interceptor/RequiresRole.java` + `RequiresRoleInterceptor.java`

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    Role[] value();
}
```

The actual authorization boundary (see `RBAC.md` §1) — every meaningful permission check in this app traces back to one of these. Takes one or more `Role` values (`OR` semantics — any one is sufficient):

```java
@RequiresRole(Role.WAREHOUSE_MANAGER)
public PurchaseOrderSummary recordGrnForShipment(...) { ... }

@RequiresRole({Role.ADMIN, Role.COORDINATOR})
public List<SupplierSummary> listSuppliers() { ... }
```

- Applicable at **class level** (every business method on the bean requires one of the given roles) or **method level** (overrides the class-level requirement for just that method — lets one bean mix several role requirements, e.g. `PurchaseOrderServiceBean.createPo` is `COORDINATOR`-only while its `listForSupplier` is `VENDOR_REP`-only, both on the same class-level-unannotated bean).
- Enforced by `RequiresRoleInterceptor.authorize()`: reads `CurrentPrincipalHolder` (populated by `@Secured`'s `JwtAuthFilter` upstream); no principal, or a principal whose role isn't in the annotation's set, throws `UnauthorizedAccessException` → mapped to **403**.
- Method-level binding takes precedence over class-level when both exist on the same call.
- Bound to a bean via classic `@Interceptors(RequiresRoleInterceptor.class)` — **not** a CDI interceptor binding — because authorization here is JWT-derived, not container-managed (JAAS) security, so there's no `@RolesAllowed` to piggyback on.

---

## 3. `@Audited` — audit logging (EJB layer)

`core/interceptor/Audited.java` + `AuditInterceptor.java`

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String resource();
    String type() default ""; // defaults to resource() when omitted
}
```

```java
@Interceptors({RequiresRoleInterceptor.class, AuditInterceptor.class})
@Audited(resource = "PROCUREMENT")
public class PurchaseOrderServiceBean implements IPurchaseOrderService { ... }
```

- `resource` names the domain entity being acted on (`"ORDER"`, `"PROCUREMENT"`, `"LOGISTICS"`); the *action* is taken from the intercepted method's name automatically — no need to restate it.
- `type` is `audit_records.type`, distinct from `resource` only when one bean records more than one kind of event — e.g. `VendorPerformanceServiceBean` uses `type = "VENDOR_PERFORMANCE"` on its weekly-recompute method to separate it from plain `PROCUREMENT` PO/GRN activity, even though both live in the same audited class.
- `AuditInterceptor.audit()` runs the business method first (`context.proceed()`), and **only on normal return** publishes an `AuditEvent` (async, via `AuditPublisher`) — a thrown business exception (e.g. `InsufficientInventoryException`) skips the audit entry entirely, since nothing was actually committed.
- If the method's return type implements the `Auditable` marker interface (`core.dto.Auditable`, default methods `getAuditReference()`/`getAuditDetails()`, both nullable), those values feed the published event — e.g. `PurchaseOrderSummary`, `OrderSummary`, `ShipmentSummary`, `VendorPerformanceResult` all implement it. Any other return type still gets a plain "this happened" audit entry.
- Same class-level/method-level precedence rule as `@RequiresRole`.
- Applicable at class level (`OrderServiceBean`'s canonical example — every method on the bean gets audited, including read methods like `getOrder`) or method level.

---

## 4. `@IdempotencyChecked` — duplicate-call short-circuiting (EJB layer)

`core/interceptor/IdempotencyChecked.java` + `IdempotencyInterceptor.java`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyChecked {
}
```

```java
@IdempotencyChecked
@Audited(resource = "LOGISTICS")
public ShipmentSummary updateStatus(Integer shipmentId, ShipmentStatus newStatus, String idempotencyKey)
```

- **Method-level only** — unlike `@RequiresRole`/`@Audited`, there's no class-level form, since an idempotency key is inherently a per-call argument, not a class-wide concern.
- **Convention, not enforced by the compiler**: the annotated method's **last parameter must be the `String` idempotency key**. `IdempotencyInterceptor` grabs it positionally (`params[params.length - 1]`) — get the parameter order wrong and it'll silently read the wrong argument as the key.
- `IdempotencyInterceptor.checkIdempotency()`: before running the business method, checks `logs` (`LogEntry.countByIdempotencyKey`) for that key. Already seen → short-circuits, returning `null` without calling the business method at all. First-seen → proceeds, then asynchronously publishes an `IdempotencyEvent` so `monitoring-svc` can durably record the key.
- **Dev-mode caveat** (see `EXCEPTIONS.md`/`API_DOCS.md`): the durable "have I seen this key" store is only populated once `monitoring-svc`'s consumer is active under `IS_PROD=true`. Under the default `IS_PROD=false`, `logs` never actually gets the row written, so reusing a key does **not** currently prevent a second write in practice — a known, documented gap, not a defect in the interceptor itself.
- The controller must handle a `null` result: `ShipmentController.updateStatus` re-fetches the shipment's current state via `getShipment` when `null` comes back, since a short-circuited call has nothing fresh to return.
- **Ordering matters** when combined with other interceptors on the same bean: list `IdempotencyInterceptor` *before* `AuditInterceptor` in `@Interceptors({...})` — a short-circuited (already-seen) call should never reach the business method, and therefore should never get audited either. `ShipmentServiceBean` is the one bean that uses all three together, in this exact order:

  ```java
  @Interceptors({RequiresRoleInterceptor.class, IdempotencyInterceptor.class, AuditInterceptor.class})
  ```

  Read left to right as: authenticate/authorize first → short-circuit a repeat call before it does anything → only audit if the business method actually ran.

---

## 5. Which bean uses which interceptors

| Bean | `@Interceptors(...)` | Class-level `@Audited` |
|---|---|---|
| `OrderServiceBean` (order-svc) | `RequiresRoleInterceptor`, `AuditInterceptor` | `resource = "ORDER"` |
| `PurchaseOrderServiceBean` (procurement-svc) | `RequiresRoleInterceptor`, `AuditInterceptor` | *(method-level only — mixed COORDINATOR/WAREHOUSE_MANAGER/VENDOR_REP roles per method)* |
| `VendorPerformanceServiceBean` (procurement-svc) | `RequiresRoleInterceptor`, `AuditInterceptor` | *(method-level; the weekly recompute uses `type = "VENDOR_PERFORMANCE"`)* |
| `ShipmentServiceBean` (logistics-svc) | `RequiresRoleInterceptor`, `IdempotencyInterceptor`, `AuditInterceptor` | *(method-level; `resource = "LOGISTICS"`)* |
| `InventoryServiceBean` (inventory-svc) | `RequiresRoleInterceptor` | — (no audit trail on stock reads/adjusts) |
| `ProfileServiceBean` (iam-svc) | `RequiresRoleInterceptor` | — |
| `UserAdminServiceBean` (iam-svc) | `RequiresRoleInterceptor` | — |

No bean uses `@Audited` without also using `RequiresRoleInterceptor` — every audited action is also a role-gated one.

---

## 6. Adding a new cross-cutting concern

If you need a new one (rate limiting, a second kind of idempotency, etc.), follow the same shape as the four above:

1. Define the annotation in `core.interceptor` (or `api-gateway.security` if it's JAX-RS-filter-level like `@Secured`, rather than EJB-business-method-level).
2. Write the interceptor/filter that enforces it.
3. Decide class-level, method-level, or both (`@Target`), and where it needs to sit in `@Interceptors({...})` relative to the existing three — think about what should short-circuit what, same reasoning as `@IdempotencyChecked` vs. `@AuditInterceptor` above.
4. Document it here, and cross-reference from `RBAC.md`/`EXCEPTIONS.md` if it changes who-can-do-what or what a rejected call returns.

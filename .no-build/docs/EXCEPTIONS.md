# GlobalTrade Logistics — Exception Reference

All custom exception types, where they're thrown, and how `api-gateway` maps them to an HTTP response. Every error response is `{"error": "<message>"}` (see [`API_DOCS.md`](./API_DOCS.md) §12 for the wire-level contract).

Every custom `ExceptionMapper` in `api-gateway/exception` also fires a `LogEvent` at `WARN` with the exception message before building the HTTP response — so 4xx/5xx outcomes appear in the live trace tail (see [`TRACE_LOGGING.md`](./TRACE_LOGGING.md)). `InvalidTokenException` is the one auth failure that bypasses mappers entirely; `JwtAuthFilter` logs it inline instead.

---

## 1. Base types

Every custom exception in `core.exception` extends one of these two roots — never `Exception`/`RuntimeException` directly.

### `SupplyChainApplicationException` (checked)
Base type for recoverable, expected business conditions. Checked, because the caller is meant to handle it — typically by letting it propagate up to a JAX-RS `ExceptionMapper` that turns it into a 4xx response. Every exception in §2 extends this.

### `SupplyChainSystemException` (unchecked)
Base type for unexpected/system failures. Unchecked (a `RuntimeException`), so it always rolls back a container-managed transaction per EJB exception-handling semantics, and needs no `throws` clause threaded through every method signature. Mapped to a generic `500` by `SupplyChainSystemExceptionMapper`, which fires a `LogEvent` `WARN`, logs the real exception at `Level.SEVERE`, and never leaks details to the client (`{"error": "An unexpected error occurred"}`).

---

## 2. Application exceptions (`SupplyChainApplicationException` subtypes)

| Exception | HTTP status | Mapper | Thrown by |
|---|---|---|---|
| `OtpExpiredOrInvalidException` | 401 | `OtpExpiredOrInvalidExceptionMapper` | `POST /auth/otp/verify` — code doesn't match, is expired, or was already consumed |
| `UnauthorizedAccessException` | 403 | `UnauthorizedAccessExceptionMapper` | `RequiresRoleInterceptor`, when the current principal's role isn't permitted to call a `@RequiresRole`-annotated method |
| `UnknownPrincipalException` | 404 | `UnknownPrincipalExceptionMapper` | OTP request/verify, profile get/update — a JWT/email doesn't resolve to an active row in `users`, `customers`, or `suppliers` |
| `EmailAlreadyRegisteredException` | 409 | `EmailAlreadyRegisteredExceptionMapper` | `POST /auth/signup/customer`, `POST /auth/signup/supplier` — the email already has a customer or supplier account |
| `InsufficientInventoryException` | 409 | `InsufficientInventoryExceptionMapper` | `POST /orders` — not enough stock for one or more requested items |
| `OrderNotFoundException` | 404 | `OrderNotFoundExceptionMapper` | `GET /orders/{orderId}` — the order doesn't exist, **or** exists but belongs to a different customer (deliberately indistinguishable, so a customer can't probe for other customers' order ids) |
| `PurchaseOrderNotFoundException` | 404 | `PurchaseOrderNotFoundExceptionMapper` | `POST /purchase-orders/{poId}/shipment` (PO doesn't exist or belongs to another supplier, indistinguishable), `POST /shipments/{shipmentId}/grn` (shipment's linked PO vanished) |
| `ShipmentNotFoundException` | 404 | `ShipmentNotFoundExceptionMapper` | Any `/shipments/{shipmentId}*` endpoint, `POST /shipments/{shipmentId}/grn` — no shipment with that id |
| `InvalidShipmentStateException` | 409 | `InvalidShipmentStateExceptionMapper` | `POST /purchase-orders/{poId}/shipment` — PO already completed or already has a shipment; `POST /shipments/{shipmentId}/grn` — shipment not linked to a PO, not yet `DELIVERED`, its latest customs record isn't `CLEARED`, or its PO already completed; `PUT /shipments/{shipmentId}/status` — `status` was `COMPLETED` (settable only by a successful GRN); `PUT /shipments/{shipmentId}/customs/status` — no customs record exists yet for that shipment |
| `InvalidTokenException` | 401 | *(none — handled inline)* | `JwtAuthFilter` / `JwtService.parseAndValidate` — bearer token missing, malformed, expired, or fails signature verification. Caught directly in the filter's `catch` block rather than via an `ExceptionMapper`, since the filter needs to `abortWith(...)` before JAX-RS resource matching even happens |

Two more 4xx cases are **not** custom exceptions — they use JAX-RS's own `jakarta.ws.rs.BadRequestException` (mapped to 400 automatically by the JAX-RS runtime, no custom mapper needed):
- Any controller's own request-body validation (missing/malformed field)
- `Role.valueOf(...)` failing on an unknown role string (`POST /admin/users`)

---

## 3. System exception

| Exception | HTTP status | Mapper | Thrown by |
|---|---|---|---|
| `SupplyChainSystemException` | 500 | `SupplyChainSystemExceptionMapper` | Anything unexpected — not raised deliberately by feature code; it's the catch-all root so a stray runtime failure still returns a clean `{"error": ...}` shape instead of a raw stack trace |

---

## 4. Adding a new exception

1. Extend `SupplyChainApplicationException` (recoverable — the caller/gateway should turn it into a specific 4xx) or let an unexpected failure surface as `SupplyChainSystemException` directly (no new subtype needed for that path).
2. For a new `SupplyChainApplicationException` subtype, add a matching `@Provider`-annotated `ExceptionMapper<YourException>` in `api-gateway`'s `exception` package, following the existing ones's shape: fire a `LogEvent` `WARN`, then `Response.status(...).entity(Map.of("error", exception.getMessage())).build()`.
3. Add a row to §2 above and to `API_DOCS.md` §12.

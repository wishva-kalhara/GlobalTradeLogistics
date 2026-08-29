# GlobalTrade Logistics — API Documentation

REST API for `api-gateway`, mounted at context root `/api` with all resources under `/v1`. Every endpoint below is `http://localhost:8080/api/v1/<path>` in a local `docker compose` deployment.

Companion docs: [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) (manual test cases), [`E2E_FLOWS.md`](./E2E_FLOWS.md) (mermaid diagrams), and [`TRACE_LOGGING.md`](./TRACE_LOGGING.md) (step-by-step `LogEvent` tail for debugging API calls).

---

## 1. Conventions

- **Content type**: request and response bodies are `application/json` unless noted (a few endpoints return no body — `204 No Content` / `201 Created`).
- **Authentication**: endpoints marked 🔒 require `Authorization: Bearer <jwt>`. The JWT is obtained from `POST /auth/otp/verify` or either `POST /auth/signup/*` endpoint. Tokens are signed HS256, carry `sub` (email) and `role` claims, and expire 1 hour after issuance (`iat`/`exp`).
- **Authorization**: some 🔒 endpoints are further gated to specific roles (noted per endpoint). Role enforcement happens at the EJB layer via a custom `@RequiresRole` interceptor — **not** container-managed security — so a valid-but-wrong-role token gets a genuine `403`, not just a UI-level block.
- **Trace logging**: every endpoint, service method, interceptor, and exception mapper fires CDI `LogEvent` breadcrumbs (`TRACE` for normal steps, `WARN` for expected failures). Lines appear in `server.log` under the `TRACE` logger — grep `[TRACE]` or a correlation key (usually the caller's email). Not persisted; see [`TRACE_LOGGING.md`](./TRACE_LOGGING.md).
- **Error shape**: every error response is `{"error": "<message>"}`, mapped from a Java exception via a JAX-RS `ExceptionMapper`. See §12 for the full exception→status table.
- **Roles** (`core.enums.Role`): `ADMIN`, `WORKER`, `COORDINATOR`, `CUSTOMS_AGENT`, `WAREHOUSE_MANAGER`, `VENDOR_REP`, `CUSTOMER`.

---

## 2. Authentication & Registration

### `POST /auth/otp/request`
Request a one-time login code for any existing principal (staff user, customer, or supplier).

- **Auth**: none.
- **Body**: `{ "email": "string" }`
- **200**: `{ "status": "otp_sent" }`
- **404**: no active user/customer/supplier found for that email (`UnknownPrincipalException`).
- **Side effect**: inserts an `otp_codes` row (hashed code, 5-minute expiry) and publishes an `OTP_AUTHENTICATION` email notification. In dev (`IS_PROD=false`) the code is only ever logged server-side, never emailed — `docker compose logs app | grep OTP_AUTHENTICATION`.

### `POST /auth/otp/verify`
Exchange a valid OTP code for a JWT.

- **Auth**: none.
- **Body**: `{ "email": "string", "code": "string" }`
- **200**: `{ "token": "string", "email": "string", "role": "ADMIN|WORKER|COORDINATOR|CUSTOMS_AGENT|WAREHOUSE_MANAGER|VENDOR_REP|CUSTOMER" }`
- **400**: email or code missing.
- **401**: code doesn't match, is expired, or was already consumed (`OtpExpiredOrInvalidException`).
- **404**: email doesn't resolve to an active principal (`UnknownPrincipalException`).
- **Role resolution order**: checked against `users`, then `customers`, then `suppliers` — first match wins.

### `POST /auth/signup/customer`
Self-service customer account creation — deliberately minimal (email + country only; the rest is collected via `PUT /me/customer` afterward). Auto-logs in on success.

- **Auth**: none.
- **Body**: `{ "email": "string", "country": "string" }`
- **200**: `AuthResponseBody` — `{ "token": "string", "email": "string", "role": "CUSTOMER" }`
- **400**: email or country missing.
- **409**: email already registered as a customer (`EmailAlreadyRegisteredException`).
- **Side effect**: inserts a `customers` row (`full_name`/`mobile_1`/`mobile_2`/`address` left `NULL`), publishes a `CUSTOMER_ONBOARDING` email notification.

### `POST /auth/signup/supplier`
Same shape as above, for suppliers.

- **Auth**: none.
- **Body**: `{ "email": "string", "country": "string" }`
- **200**: `AuthResponseBody` — role will be `VENDOR_REP`.
- **400** / **409**: same as customer signup.

---

## 3. Profile 🔒

All four endpoints resolve **which row to read/update from the caller's JWT** (`sub` claim) — there is no client-supplied id, so a principal can only ever see/edit their own row.

### `GET /me/customer` 🔒 *(role: `CUSTOMER`)*
Pre-fills `me/update-profile.jsp`.

- **200**: `ProfileSummary` — `{ "email": "string", "fullName": "string|null", "mobile1": "string|null", "mobile2": "string|null", "address": "string|null", "country": "string|null" }`. Fields are `null` until the profile's been completed at least once.
- **403**: caller isn't a `CUSTOMER`.
- **404**: JWT's email doesn't resolve to an active customer (`UnknownPrincipalException`).

### `PUT /me/customer` 🔒 *(role: `CUSTOMER`)*
- **Body**: `{ "fullName": "string", "mobile1": "string", "mobile2": "string", "address": "string", "country": "string" }` — `country` is a country **code** (matches `GET /countries`' `code` field, not `name`).
- **204**: profile updated, no body.
- **403**: caller isn't a `CUSTOMER`.
- **404**: JWT's email doesn't resolve to an active customer (`UnknownPrincipalException`).

### `GET /me/supplier` 🔒 *(role: `VENDOR_REP`)*
Same shape as `GET /me/customer`, for the `suppliers` table. Used both to pre-fill `frontend-seller/me/update-profile.jsp` and by `auth/login.jsp` to decide whether a returning supplier's profile is complete (non-blank `fullName`) — if so, it redirects straight to the dashboard instead of the profile-completion page.

### `PUT /me/supplier` 🔒 *(role: `VENDOR_REP`)*
- Same body shape and status codes as `PUT /me/customer`, for the `suppliers` table.

---

## 4. Countries

### `GET /countries`
- **Auth**: none (needed by sign-up/profile pages before any identity exists).
- **200**: `[{ "code": "string", "name": "string" }, ...]` — 45 seeded countries.

---

## 5. Admin (staff & direct customer/supplier provisioning) 🔒 *(role: `ADMIN`)*

### `GET /admin/users` 🔒
- **200**: `[{ "email": "string", "fullName": "string", "role": "<Role>" }, ...]` — every row in `users`, ordered by full name.

### `POST /admin/users` 🔒
Provision an internal staff account (no self-signup exists for staff).

- **Body**: `{ "email": "string", "fullName": "string", "role": "<Role>" }`
- **201**: created, no body.
- **400**: missing field, or `role` isn't a valid `Role` enum value.
- **403**: caller isn't `ADMIN`.
- **Side effect**: inserts a `users` row, publishes a `WORKER_ONBOARDING` email notification.

### `POST /admin/customers` 🔒
Admin-driven alternative to customer self-signup — collects full details in one call (no separate profile-completion step).

- **Body**: `{ "email": "string", "fullName": "string", "mobile1": "string", "address": "string", "country": "string" }`
- **201**: created, no body.
- **400**: `email`/`fullName` missing.
- **403**: caller isn't `ADMIN`.
- **Side effect**: inserts a `customers` row, publishes `CUSTOMER_ONBOARDING`.

### `POST /admin/suppliers` 🔒
Same as above, for suppliers.

- **Body**: `{ "email": "string", "fullName": "string", "mobile1": "string", "address": "string", "country": "string" }` — all five fields required.
- **201** / **400** / **403**: same shape as `/admin/customers`.

### `GET /admin/suppliers` 🔒 *(roles: `ADMIN`, `COORDINATOR`)*
Suppliers to populate a supplier-picking dropdown — used by `POST /purchase-orders`'s create-PO page.

- **Query param**: `productId` (optional, integer).
- **200** (no `productId`): `[SupplierSummary, ...]` — every active supplier with a **completed profile** (non-blank `fullName`). A supplier who's only ever self-signed-up is deliberately excluded — showing a bare email in place of a business name would read as a generic account, not a real seller.
- **200** (with `productId`): same shape, further filtered to only suppliers who've registered a product offering (`POST /suppliers/me/products`) for that product — i.e. "who can actually fulfill this."
- **403**: caller isn't `ADMIN`/`COORDINATOR`.

### `GET /admin/sales-summary` 🔒 *(roles: `ADMIN`, `COORDINATOR`)*
Store-wide sales aggregate for the staff dashboard's Analytics section — aggregates the `orders`/`order_items` tables (customer orders), unrelated to `purchase_orders`.

- **200**: `SalesSummary` — `{ "totalSales": 1234.5, "totalOrders": 12, "ordersByStatus": {"PLACED": 10, ...}, "topProducts": [{ "productId": 1, "productName": "string", "qtySold": 20, "revenue": 250.0 }, ...] }` — `topProducts` is the top 5 by revenue.
- **403**: caller isn't `ADMIN`/`COORDINATOR`.

---

## 6. Catalog & Orders

### `GET /products`
Read-only product catalog with live stock — unprotected so it can be browsed before login.

- **Auth**: none.
- **200**: `[{ "productId": 1, "name": "string", "description": "string", "productImage": "string", "availableQty": 500, "unitPrice": 12.5 }, ...]`
- `availableQty`/`unitPrice` come from that product's highest-stock `inventory` row (single-warehouse deployment, so effectively warehouse `1`).

### `POST /orders` 🔒 *(role: `CUSTOMER`)*
Place a multi-item order. Stock check + decrement happens in the same transaction as the order write.

- **Body**: `{ "items": [{ "productId": 1, "qty": 2 }, ...] }` — at least one item, each with a positive `qty`.
- **200**: `OrderSummary` — `{ "orderId": 1, "orderedAt": "2026-01-01T00:00:00Z", "totalPrice": 25.0, "status": "PLACED", "items": [{ "productId": 1, "productName": "string", "qty": 2, "unitPrice": 12.5 }] }`
- **400**: no items, or an item missing `productId`/positive `qty`.
- **403**: caller isn't `CUSTOMER`.
- **409**: insufficient stock for one or more items (`InsufficientInventoryException`) — nothing is written, including for the other items in the same request.
- **Side effects**: `inventory.qty` decremented per item; publishes `ORDER_CONFIRMATION` email; publishes an `ORDER` audit event (`@Audited` on `OrderServiceBean`).

### `GET /orders` 🔒 *(role: `CUSTOMER`)*
- **200**: `[OrderSummary, ...]` — every order for the calling customer (resolved from the JWT, never a client-supplied id), most recent first.

### `GET /orders/{orderId}` 🔒 *(role: `CUSTOMER`)*
- **200**: `OrderSummary` for that order.
- **404**: the order doesn't exist, **or** exists but belongs to a different customer — deliberately indistinguishable, so one customer can't probe for other customers' order ids (`OrderNotFoundException`).

---

## 7. Purchase Orders & Supplier Catalog 🔒

### `POST /purchase-orders` 🔒 *(role: `COORDINATOR`)*
Coordinator orders more stock from a supplier.

- **Body**: `{ "supplierId": 1, "productId": 1, "qty": 30 }`
- **200**: `PurchaseOrderSummary` — `{ "poId": 1, "supplierId": 1, "productId": 1, "productName": "string", "requestingQty": 30, "totalPrice": 247.5, "completed": false, "createdAt": "..." }`
- **400**: missing field or non-positive `qty`.
- **403**: caller isn't `COORDINATOR`.
- `totalPrice` is computed as `qty × that product's current Inventory.unitPrice` (no independent PO pricing exists in this schema).
- **Side effect**: publishes a `PROCUREMENT` audit event.

### `GET /purchase-orders` 🔒 *(role: `VENDOR_REP`)*
- **200**: `[PurchaseOrderSummary, ...]` — every PO placed against the calling supplier (resolved from the JWT), most recent first.

### `GET /purchase-orders/shippable` 🔒 *(role: `VENDOR_REP`)*
- **200**: `[PurchaseOrderSummary, ...]` — the calling supplier's open (`completed: false`) POs that don't have a shipment yet. Feeds the create-shipment dropdown.

### `POST /purchase-orders/{poId}/shipment` 🔒 *(role: `VENDOR_REP`)*
Supplier ships one of their own open purchase orders — the first step of the ship → customs → GRN flow.

- **Body**: `{ "trackingNumber": "string", "vesselId": "string", "type": "string" }` — all three required, non-blank.
- **200**: `ShipmentSummary` — see §9 for shape; `status` starts `CREATED`, `poId` is the given PO.
- **400**: missing/blank field.
- **403**: caller isn't `VENDOR_REP`.
- **404**: the PO doesn't exist, **or** exists but belongs to a different supplier (deliberately indistinguishable, same convention as `OrderNotFoundException`) (`PurchaseOrderNotFoundException`).
- **409**: the PO is already completed, or already has a shipment (`InvalidShipmentStateException`).
- **Side effect**: inserts a `shipments` row linked to the PO (`purchase_orders_po_id`); `warehouse_id` defaults to this single-warehouse deployment's one warehouse; publishes a `LOGISTICS` audit event.

### `POST /suppliers/me/products` 🔒 *(role: `VENDOR_REP`)*
Register which products a supplier can provide, from which warehouse, and their lead time.

- **Body**: `{ "productId": 1, "warehouseId": 1, "leadTimeInDays": 3 }` — `leadTimeInDays` ≥ 0.
- **201**: created, no body.
- **400**: missing field or negative lead time.
- **403**: caller isn't `VENDOR_REP`.
- **Side effect**: inserts a `supplier_providing_products` row; `suppliers_supplier_id` is resolved from the JWT, not client-supplied.

---

## 8. Inventory 🔒

### `GET /inventory/warehouses` 🔒 *(roles: `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`, `VENDOR_REP`)*
Warehouses to populate a warehouse-picking dropdown — the staff inventory console's, plus `VENDOR_REP` for picking a delivery warehouse when registering a product offering.

- **200**: `[{ "warehouseId": 1, "country": "string" }, ...]` — this deployment seeds exactly one (`id 1`, `US`).
- **403**: caller's role isn't one of the four above.

### `GET /inventory/{warehouseId}` 🔒 *(roles: `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`)*
- **200**: `[{ "inventoryId": 1, "warehouseId": 1, "productId": 1, "productName": "string", "qty": 500, "reorderLevel": 20, "unitPrice": 12.5, "lastUpdatedAt": "..." }, ...]`
- **403**: caller's role isn't one of the three above.
- Empty array (not an error) if the warehouse has no inventory rows.

---

## 9. Shipments & Customs 🔒 *(role: `CUSTOMS_AGENT`, except where noted)*

The full lifecycle is now **ship → customs → GRN**: a supplier creates the shipment (§7), a customs agent clears it, and only once cleared *and* `DELIVERED` can a warehouse manager record its GRN — which is the only thing that ever sets a shipment to `COMPLETED`.

### `GET /shipments` 🔒 *(roles: `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`, `CUSTOMS_AGENT`)*
- **200**: `[ShipmentSummary, ...]` — every shipment, most recent first. Staff-wide visibility into shipments in progress.
- **403**: caller's role isn't one of the four above.

### `GET /shipments/mine` 🔒 *(role: `VENDOR_REP`)*
- **200**: `[ShipmentSummary, ...]` — shipments created for any of the calling supplier's purchase orders, most recent first.
- **403**: caller isn't `VENDOR_REP`.

### `GET /shipments/{shipmentId}` 🔒
- **200**: `ShipmentSummary` — `{ "shipmentId": 1, "trackingNumber": "string", "vesselId": "string", "type": "string", "warehouseId": 1, "status": "CREATED|IN_TRANSIT|DELIVERED|DELAYED|COMPLETED", "shipmentType": "string|null", "ref": "string|null", "poId": 1, "customsStatus": "PENDING|CLEARED|HELD|null" }`
- **404**: no shipment with that id (`ShipmentNotFoundException`).
- `poId` is the purchase order this shipment was created for (§7's `POST /purchase-orders/{poId}/shipment`) — `null` for the one legacy seeded shipment that predates the ship → customs → GRN flow.
- `customsStatus` reflects the shipment's most recent customs clearance record (see below) — `null` if none has been created yet.

### `GET /shipments/awaiting-grn` 🔒 *(role: `WAREHOUSE_MANAGER`)*
- **200**: `[ShipmentSummary, ...]` — shipments that are `DELIVERED`, linked to a PO, and whose PO isn't completed yet. Feeds the record-GRN dropdown. (Does **not** additionally filter on customs status — a shipment can appear here and still get rejected at GRN time if customs isn't `CLEARED` yet; the customs check happens at `POST /shipments/{shipmentId}/grn`.)
- **403**: caller isn't `WAREHOUSE_MANAGER`.

### `POST /shipments/{shipmentId}/grn` 🔒 *(role: `WAREHOUSE_MANAGER`)*
Record goods received for a delivered, customs-cleared shipment — the last step of the ship → customs → GRN flow. Replaces the old PO-id-based GRN endpoint now that GRN is gated on shipment state.

- **Body**: `{ "qty": 30 }` — positive.
- **200**: `PurchaseOrderSummary` (the shipment's linked PO) with `"completed": true`.
- **400**: missing/non-positive `qty`.
- **403**: caller isn't `WAREHOUSE_MANAGER`.
- **404**: no shipment with that id (`ShipmentNotFoundException`), or its linked PO vanished (`PurchaseOrderNotFoundException`).
- **409**: the shipment isn't linked to a PO, isn't yet `DELIVERED`, its latest customs record isn't `CLEARED`, or its PO was already completed (`InvalidShipmentStateException`).
- **Side effects**: inserts a `grns` row; increments `inventory.qty`; sets `purchase_orders.is_completed = 1`; sets the shipment's `status = COMPLETED`; publishes a `PROCUREMENT` audit event.

### `PUT /shipments/{shipmentId}/status` 🔒
- **Body**: `{ "status": "CREATED|IN_TRANSIT|DELIVERED|DELAYED", "idempotencyKey": "string" }` — both required, `idempotencyKey` non-blank. `COMPLETED` is **not** a settable value here — see 409 below.
- **200**: `ShipmentSummary` reflecting the new status. If the exact same `idempotencyKey` had already been processed, the interceptor short-circuits the write and the endpoint transparently re-fetches and returns the shipment's current state instead (see caveat below).
- **400**: missing field, or `status` isn't a valid `ShipmentStatus` value.
- **403**: caller isn't `CUSTOMS_AGENT`.
- **404**: shipment not found.
- **409**: `status` was `COMPLETED` (`InvalidShipmentStateException`) — that status is set automatically by `POST /shipments/{shipmentId}/grn`, never directly.
- **Idempotency caveat**: the durable "have I seen this key" store (`logs` table) is only populated once `monitoring-svc`'s consumer is active under `IS_PROD=true`. Under the default dev config (`IS_PROD=false`), reusing a key does **not** actually prevent a second write — this is a known, documented gap, not a defect in this endpoint.

### `POST /shipments/{shipmentId}/customs` 🔒
Creates a new customs clearance record, starting at `PENDING`.

- **Body**: `{ "declarationNumber": "string" }` (declaration number may be omitted).
- **201**: created, no body.
- **403** / **404**: same as above.
- **Side effect**: inserts a `custom_clearence_records` row with `status = PENDING`.

### `PUT /shipments/{shipmentId}/customs/status` 🔒
"Handles customs" for the shipment — advances its most recent customs record to `CLEARED` (or `HELD`/back to `PENDING`). A GRN can't be recorded until this reaches `CLEARED`.

- **Body**: `{ "status": "PENDING|CLEARED|HELD" }`.
- **200**: `ShipmentSummary` with `customsStatus` reflecting the update.
- **400**: missing `status`, or not a valid `CustomsClearanceStatus` value.
- **403**: caller isn't `CUSTOMS_AGENT`.
- **404**: no shipment with that id.
- **409**: no customs record exists yet for this shipment — create one first via `POST /shipments/{shipmentId}/customs` (`InvalidShipmentStateException`).

### `POST /shipments/{shipmentId}/notify-carrier`
Simulates notifying an external carrier system. **Not role-gated** beyond requiring a valid JWT (any authenticated staff role can call it) — it's the project's bean-managed-transaction (BMT) example: the simulated external call runs with no database transaction open, sandwiched between two short read/write transactions.

- **Auth**: 🔒 (valid JWT only, no `@RequiresRole`).
- **Body**: none.
- **200**: `ShipmentSummary` with `ref` populated (`"CARRIER-<uuid>"`).
- **404**: shipment not found.

---

## 10. Vendor Performance 🔒 *(roles: `ADMIN`, `COORDINATOR`)*

### `GET /admin/vendor-performance` 🔒
Read-only view of the weekly vendor-performance recompute's audit trail.

- **200**: `[{ "id": 1, "createdAt": "...", "resource": "PROCUREMENT", "action": "recomputeForSupplier", "reference": "<supplierId>", "details": "5/10 deliveries on time (50.0%)" }, ...]`
- **403**: caller isn't `ADMIN`/`COORDINATOR`.
- Under the default dev config (`IS_PROD=false`), this is expected to return `[]` — the weekly timer's audit event is only durably persisted once `monitoring-svc`'s consumer is active.

---

## 11. Health

### `GET /healthz`
- **Auth**: none.
- **200**: plain text `Up and running` (not JSON — the one endpoint that isn't).

---

## 12. Error Reference

Every error response is `{"error": "<message>"}`. Status code is determined by which exception was thrown:

| Exception | Status | Thrown by |
|---|---|---|
| Missing/malformed body field | 400 | Any controller's own validation (`BadRequestException`) |
| `OtpExpiredOrInvalidException` | 401 | OTP verify |
| Missing/invalid/expired JWT (`InvalidTokenException`) | 401 | `JwtAuthFilter`, on any 🔒 endpoint |
| `UnauthorizedAccessException` | 403 | `RequiresRoleInterceptor`, on any role-gated endpoint |
| `OrderNotFoundException` | 404 | `GET /orders/{id}` |
| `PurchaseOrderNotFoundException` | 404 | `POST /purchase-orders/{id}/shipment`, `POST /shipments/{id}/grn` |
| `ShipmentNotFoundException` | 404 | Any `/shipments/{id}*` endpoint, `POST /shipments/{id}/grn` |
| `UnknownPrincipalException` | 404 | OTP request/verify, profile update (JWT resolves to no active row) |
| `EmailAlreadyRegisteredException` | 409 | Self-service signup |
| `InsufficientInventoryException` | 409 | `POST /orders` |
| `InvalidShipmentStateException` | 409 | `POST /purchase-orders/{id}/shipment`, `POST /shipments/{id}/grn` |
| `SupplyChainSystemException` (anything unexpected) | 500 | Any endpoint |

---

## 13. Full Endpoint Index

| Method | Path | Auth | Role | Request body | Response |
|---|---|---|---|---|---|
| POST | `/auth/otp/request` | — | — | `OtpRequestBody` | `{status}` |
| POST | `/auth/otp/verify` | — | — | `OtpVerifyBody` | `AuthResponseBody` |
| POST | `/auth/signup/customer` | — | — | `SignUpCustomerBody` | `AuthResponseBody` |
| POST | `/auth/signup/supplier` | — | — | `SignUpSupplierBody` | `AuthResponseBody` |
| GET | `/me/customer` | 🔒 | CUSTOMER | — | `ProfileSummary` |
| PUT | `/me/customer` | 🔒 | CUSTOMER | `UpdateCustomerProfileBody` | 204 |
| GET | `/me/supplier` | 🔒 | VENDOR_REP | — | `ProfileSummary` |
| PUT | `/me/supplier` | 🔒 | VENDOR_REP | `UpdateSupplierProfileBody` | 204 |
| GET | `/countries` | — | — | — | `[CountrySummary]` |
| GET | `/admin/users` | 🔒 | ADMIN | — | `[UserSummary]` |
| POST | `/admin/users` | 🔒 | ADMIN | `CreateUserBody` | 201 |
| POST | `/admin/customers` | 🔒 | ADMIN | `RegisterCustomerBody` | 201 |
| POST | `/admin/suppliers` | 🔒 | ADMIN | `RegisterSupplierBody` | 201 |
| GET | `/products` | — | — | — | `[ProductSummary]` |
| POST | `/orders` | 🔒 | CUSTOMER | `PlaceOrderBody` | `OrderSummary` |
| GET | `/orders` | 🔒 | CUSTOMER | — | `[OrderSummary]` |
| GET | `/orders/{orderId}` | 🔒 | CUSTOMER | — | `OrderSummary` |
| POST | `/purchase-orders` | 🔒 | COORDINATOR | `CreatePurchaseOrderBody` | `PurchaseOrderSummary` |
| GET | `/purchase-orders` | 🔒 | VENDOR_REP | — | `[PurchaseOrderSummary]` |
| GET | `/purchase-orders/shippable` | 🔒 | VENDOR_REP | — | `[PurchaseOrderSummary]` |
| POST | `/purchase-orders/{poId}/shipment` | 🔒 | VENDOR_REP | `CreateShipmentBody` | `ShipmentSummary` |
| POST | `/suppliers/me/products` | 🔒 | VENDOR_REP | `AddProductOfferingBody` | 201 |
| GET | `/inventory/{warehouseId}` | 🔒 | ADMIN, COORDINATOR, WAREHOUSE_MANAGER | — | `[InventorySummary]` |
| GET | `/inventory/warehouses` | 🔒 | ADMIN, COORDINATOR, WAREHOUSE_MANAGER, VENDOR_REP | — | `[WarehouseSummary]` |
| GET | `/shipments` | 🔒 | ADMIN, COORDINATOR, WAREHOUSE_MANAGER, CUSTOMS_AGENT | — | `[ShipmentSummary]` |
| GET | `/shipments/mine` | 🔒 | VENDOR_REP | — | `[ShipmentSummary]` |
| GET | `/shipments/{shipmentId}` | 🔒 | CUSTOMS_AGENT | — | `ShipmentSummary` |
| GET | `/shipments/awaiting-grn` | 🔒 | WAREHOUSE_MANAGER | — | `[ShipmentSummary]` |
| POST | `/shipments/{shipmentId}/grn` | 🔒 | WAREHOUSE_MANAGER | `RecordGrnBody` | `PurchaseOrderSummary` |
| PUT | `/shipments/{shipmentId}/status` | 🔒 | CUSTOMS_AGENT | `UpdateShipmentStatusBody` | `ShipmentSummary` |
| POST | `/shipments/{shipmentId}/customs` | 🔒 | CUSTOMS_AGENT | `CreateCustomsRecordBody` | 201 |
| PUT | `/shipments/{shipmentId}/customs/status` | 🔒 | CUSTOMS_AGENT | `UpdateCustomsStatusBody` | `ShipmentSummary` |
| POST | `/shipments/{shipmentId}/notify-carrier` | 🔒 | any authenticated | — | `ShipmentSummary` |
| GET | `/admin/sales-summary` | 🔒 | ADMIN, COORDINATOR | — | `SalesSummary` |
| GET | `/admin/suppliers` | 🔒 | ADMIN, COORDINATOR | — | `[SupplierSummary]` |
| GET | `/admin/vendor-performance` | 🔒 | ADMIN, COORDINATOR | — | `[AuditRecordSummary]` |
| GET | `/healthz` | — | — | — | text |

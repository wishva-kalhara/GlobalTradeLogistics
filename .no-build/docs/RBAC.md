# GlobalTrade Logistics — Role-Based Access Control (RBAC)

Companion to [`API_DOCS.md`](./API_DOCS.md) (full endpoint reference), [`EXCEPTIONS.md`](./EXCEPTIONS.md) (error mapping), and [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) / [`E2E_TEST_FLOW.md`](./E2E_TEST_FLOW.md) (test cases per role). This document is the single place to look up **who can do what**.

---

## 1. How enforcement works

Authorization is **not** container-managed (`@RolesAllowed`/JAAS) — every role check goes through a custom stack:

1. `JwtAuthFilter` (`api-gateway`, `@Secured`-bound) validates the `Authorization: Bearer <jwt>` header and populates a thread-scoped `CurrentPrincipalHolder` with the JWT's `sub` (email) and `role` claims. No valid token → **401**.
2. `RequiresRoleInterceptor` (`core`, applied via `@RequiresRole({...})` on EJB business methods) reads `CurrentPrincipalHolder` and rejects the call with `UnauthorizedAccessException` if the caller's role isn't in the annotation's allowed set. Mapped to **403** at the gateway.
3. Every method that resolves "the caller's own record" (profile, orders, purchase orders, shipments, product offerings) does so from `CurrentPrincipalHolder`'s email — **never** a client-supplied id. This means the authorization boundary is enforced at the EJB layer regardless of what the frontend does; a frontend's own role checks (redirect to `access-denied.jsp`, hiding a nav link, disabling a form control) are UX convenience only.

**Roles** (`core.enums.Role`): `ADMIN`, `WORKER`, `COORDINATOR`, `CUSTOMS_AGENT`, `WAREHOUSE_MANAGER`, `VENDOR_REP`, `CUSTOMER`.

- `CUSTOMER` and `VENDOR_REP` are **external** roles — obtained via self-signup (`POST /auth/signup/customer|supplier`) or admin-driven direct registration (`POST /admin/customers|suppliers`). Backed by the `customers`/`suppliers` tables.
- The other five are **internal staff** roles — backed by the `users` table, provisioned only by an `ADMIN` (`POST /admin/users`), never self-signup.

---

## 2. Public (unauthenticated) endpoints

No `@Secured`, no role check — anyone can call these:

| Endpoint | Purpose |
|---|---|
| `POST /auth/otp/request` | Request a login code (any existing principal) |
| `POST /auth/otp/verify` | Exchange a code for a JWT |
| `POST /auth/signup/customer` | Customer self-signup |
| `POST /auth/signup/supplier` | Supplier self-signup |
| `GET /countries` | Country list (needed by sign-up/profile forms before any identity exists) |
| `GET /products` | Product catalog browsing |
| `GET /healthz` | Health check |

---

## 3. Role → Accessible Flows Matrix

| Role | Endpoint | Method | What it does |
|---|---|---|---|
| **CUSTOMER** | `/me/customer` | GET | Read own profile (pre-fill the profile form) |
| | `/me/customer` | PUT | Complete/update own profile |
| | `/orders` | POST | Place a multi-item order |
| | `/orders` | GET | List own order history |
| | `/orders/{orderId}` | GET | View one own order (404 if not yours) |
| **VENDOR_REP** | `/me/supplier` | GET | Read own profile (pre-fill `me/update-profile.jsp`; also drives the profile-aware login redirect) |
| | `/me/supplier` | PUT | Complete/update own profile |
| | `/purchase-orders` | GET | List POs placed against own supplier account |
| | `/purchase-orders/shippable` | GET | List own open POs that don't have a shipment yet |
| | `/purchase-orders/{poId}/shipment` | POST | Create a shipment for one of own open POs |
| | `/shipments/mine` | GET | List shipments created for any of own POs |
| | `/suppliers/me/products` | POST | Register a product offering (own supplier) |
| **COORDINATOR** | `/purchase-orders` | POST | Create a purchase order |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| | `/inventory/warehouses` | GET | List warehouses (dropdown) |
| | `/admin/vendor-performance` | GET | View vendor performance reports |
| | `/admin/sales-summary` | GET | View store-wide sales aggregate |
| | `/admin/suppliers` | GET | List suppliers (optionally filtered by `?productId=`) — populates the create-PO supplier dropdown |
| | `/shipments` | GET | View every shipment in progress |
| **WAREHOUSE_MANAGER** | `/shipments/{shipmentId}/grn` | POST | Record goods received for a delivered, customs-cleared shipment; completes its PO |
| | `/shipments/awaiting-grn` | GET | List shipments ready for a GRN |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| | `/inventory/warehouses` | GET | List warehouses (dropdown) |
| | `/shipments` | GET | View every shipment in progress |
| **CUSTOMS_AGENT** | `/shipments/{id}` | GET | View a shipment |
| | `/shipments` | GET | View every shipment in progress (dropdown) |
| | `/shipments/{id}/status` | PUT | Update shipment status (`COMPLETED` rejected — system-set only) |
| | `/shipments/{id}/customs` | POST | Record a customs clearance (starts `PENDING`) |
| | `/shipments/{id}/customs/status` | PUT | Advance/hold customs clearance (`PENDING`/`CLEARED`/`HELD`) |
| **ADMIN** | `/admin/users` | GET | List internal staff users |
| | `/admin/users` | POST | Onboard a staff user (any of the 5 internal roles) |
| | `/admin/customers` | POST | Register a customer directly (admin-driven, skips self-signup) |
| | `/admin/suppliers` | GET / POST | List (optionally by product) / register a supplier directly |
| | `/admin/sales-summary` | GET | View store-wide sales aggregate |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| | `/inventory/warehouses` | GET | List warehouses (dropdown) |
| | `/admin/vendor-performance` | GET | View vendor performance reports |
| | `/shipments` | GET | View every shipment in progress |
| **Any authenticated role** | `/shipments/{id}/notify-carrier` | POST | Simulate carrier notification (valid JWT only — no `@RequiresRole`) |
| **WORKER** | *(none yet)* | — | Provisionable today, but no endpoint is gated to `WORKER` specifically — reserved for future flows |

Multiple roles on one endpoint (`OR`, not `AND` — any one of the listed roles is sufficient):
- `GET /inventory/{warehouseId}` → `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`
- `GET /inventory/warehouses` → `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`, `VENDOR_REP`
- `GET /admin/vendor-performance` → `ADMIN`, `COORDINATOR`
- `GET /admin/sales-summary` → `ADMIN`, `COORDINATOR`
- `GET /admin/suppliers` → `ADMIN`, `COORDINATOR`
- `GET /shipments` (staff-wide list) → `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`, `CUSTOMS_AGENT`

---

## 4. Customer-Accessible Flows (role: `CUSTOMER`)

Everything a signed-in customer can do, end to end:

1. **Sign up** — `POST /auth/signup/customer` (email + country only, unauthenticated) → auto-login.
2. **Log in** (returning) — `POST /auth/otp/request` → `POST /auth/otp/verify`.
3. **View own profile** — `GET /me/customer` (pre-fills `me/update-profile.jsp`, including the country select — the option `value` is the country **code**, matching the stored value).
4. **Complete/update own profile** — `PUT /me/customer` (full name, mobile 1/2, address, country).
5. **Browse the catalog** — `GET /products` (also reachable while logged out).
6. **Place an order** — `POST /orders`, one or more line items; fails with `409` if any item lacks sufficient stock.
7. **View order history** — `GET /orders` (own orders only, most recent first).
8. **View one order** — `GET /orders/{orderId}` (404 if it isn't yours — indistinguishable from a nonexistent id, so one customer can't probe another's order ids).

**Frontend**: all of this lives in `frontend-customer` (`index.jsp` = catalog + place-order, `orders.jsp`, `me/update-profile.jsp`, `auth/login.jsp`, `auth/sign-up.jsp`).

**What a customer cannot do**: anything staff- or supplier-scoped — create/view purchase orders, ship/receive goods, manage shipments, view inventory, view/manage other users, view vendor performance or sales figures. All of those endpoints reject a `CUSTOMER` token with `403`.

---

## 5. Supplier-Accessible Flows (role: `VENDOR_REP`)

Everything a signed-in supplier can do, end to end:

1. **Sign up** — `POST /auth/signup/supplier` (email + country only, unauthenticated) → auto-login.
2. **Log in** (returning) — `POST /auth/otp/request` → `POST /auth/otp/verify`. The frontend then calls `GET /me/supplier`: if `fullName` is already set, it lands on the seller dashboard (`index.jsp`); otherwise it's routed to `me/update-profile.jsp` to finish onboarding.
3. **View own profile** — `GET /me/supplier` (pre-fills `me/update-profile.jsp`, country select value is the country code).
4. **Complete/update own profile** — `PUT /me/supplier` (business name, mobile 1/2, address, country).
5. **Register a product offering** — `POST /suppliers/me/products` (which product, which warehouse — picked from a dropdown backed by `GET /inventory/warehouses` — and lead time in days). `supplierId` is always resolved from the JWT, never client-supplied.
6. **View purchase orders placed against them** — `GET /purchase-orders` (own supplier's POs only, most recent first, shows open/completed status).
7. **View which of those are ready to ship** — `GET /purchase-orders/shippable` (open, and not already linked to a shipment).
8. **Create a shipment for one of them** — `POST /purchase-orders/{poId}/shipment` (tracking number, vessel id, type). Rejects a PO that's already completed or already has a shipment (409), or one that isn't the caller's own (404, indistinguishable from nonexistent).
9. **Track shipments through customs/delivery** — `GET /shipments/mine`.

**Frontend**: `frontend-seller` — `auth/login.jsp`, `auth/sign-up.jsp`, `me/update-profile.jsp`, `products/add-offering.jsp`, `purchase-orders.jsp`, `shipments/create.jsp`, `shipments/list.jsp`.

**What a supplier cannot do**: create purchase orders (that's `COORDINATOR`), update shipment status/customs clearance or record a GRN (`CUSTOMS_AGENT`/`WAREHOUSE_MANAGER`), view/manage users (`ADMIN`), view inventory, sales, or vendor-performance reports. All reject a `VENDOR_REP` token with `403`.

---

## 6. The ship → customs → GRN pipeline (cross-role)

This is the one flow that genuinely spans four roles and can't be described from a single role's section. See [`E2E_TEST_FLOW.md`](./E2E_TEST_FLOW.md) for the full, verified step-by-step walkthrough. Summary of who does what and the state gates between steps:

1. **COORDINATOR** creates a PO (`POST /purchase-orders`).
2. **VENDOR_REP** ships it (`POST /purchase-orders/{poId}/shipment`) — shipment starts `CREATED`.
3. **CUSTOMS_AGENT** advances it: `CREATED → IN_TRANSIT` (only transition allowed from `CREATED`), then — while `IN_TRANSIT` — creates a customs record (`PENDING`) and advances it to `CLEARED`. Once a declaration number is set or the customs status is `CLEARED`, those respective controls lock (can't be re-entered/reverted through this endpoint). Then advances shipment status to `DELIVERED`.
4. **WAREHOUSE_MANAGER** records the GRN (`POST /shipments/{shipmentId}/grn`) — but only once the shipment is `DELIVERED` **and** its customs status is `CLEARED`; otherwise `409`. A successful GRN completes the PO and sets the shipment to `COMPLETED` — the *only* way that status is ever set (`PUT /shipments/{id}/status` rejects `COMPLETED` directly, also `409`).

All four roles can see the shipment's progress at any point: `GET /shipments` (staff, all four roles) or `GET /shipments/mine` (the supplier who created it).

---

## 7. Known Gaps / Asymmetries

- **`WORKER` has no dedicated endpoint**: the role can be provisioned via `POST /admin/users`, and `frontend-app`'s dashboard correctly shows "no console actions yet" for it, but no backend flow is currently gated specifically to `WORKER`.
- **`notify-carrier` is intentionally under-scoped**: any authenticated user (any role) can call it — it's the project's bean-managed-transaction (BMT) teaching example, not meant to model a real permission boundary.
- **Admin-driven vs. self-service registration are genuinely separate paths**: `POST /admin/customers|suppliers` (full details in one call, admin-only) and `POST /auth/signup/customer|supplier` (email+country only, unauthenticated, auto-login) both create the same kind of row but serve different flows — an admin onboarding someone on their behalf vs. that person signing themselves up.
- **`GET /admin/suppliers` (no `productId`) and `GET /admin/suppliers?productId=`  both filter to completed profiles**: a supplier who's only ever self-signed-up (no `fullName` saved yet) never appears in either — intentional, so PO/create-shipment dropdowns never show a bare email in place of a business name.

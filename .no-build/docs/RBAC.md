# GlobalTrade Logistics — Role-Based Access Control (RBAC)

Companion to [`API_DOCS.md`](./API_DOCS.md) (full endpoint reference) and [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) (manual test cases per role). This document is the single place to look up **who can do what**.

---

## 1. How enforcement works

Authorization is **not** container-managed (`@RolesAllowed`/JAAS) — every role check goes through a custom stack:

1. `JwtAuthFilter` (`api-gateway`, `@Secured`-bound) validates the `Authorization: Bearer <jwt>` header and populates a thread-scoped `CurrentPrincipalHolder` with the JWT's `sub` (email) and `role` claims. No valid token → **401**.
2. `RequiresRoleInterceptor` (`core`, applied via `@RequiresRole({...})` on EJB business methods) reads `CurrentPrincipalHolder` and rejects the call with `UnauthorizedAccessException` if the caller's role isn't in the annotation's allowed set. Mapped to **403** at the gateway.
3. Every method that resolves "the caller's own record" (profile, orders, purchase orders, product offerings) does so from `CurrentPrincipalHolder`'s email — **never** a client-supplied id. This means the authorization boundary is enforced at the EJB layer regardless of what the frontend does; a frontend's own role checks (redirect to `access-denied.jsp`, hiding a nav link) are UX convenience only.

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
| **VENDOR_REP** | `/me/supplier` | PUT | Complete/update own profile |
| | `/purchase-orders` | GET | List POs placed against own supplier account |
| | `/suppliers/me/products` | POST | Register a product offering (own supplier) |
| **COORDINATOR** | `/purchase-orders` | POST | Create a purchase order |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| | `/admin/vendor-performance` | GET | View vendor performance reports |
| **WAREHOUSE_MANAGER** | `/purchase-orders/{poId}/grn` | POST | Record goods received, complete a PO |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| **CUSTOMS_AGENT** | `/shipments/{id}` | GET | View a shipment |
| | `/shipments/{id}/status` | PUT | Update shipment status |
| | `/shipments/{id}/customs` | POST | Record a customs clearance |
| **ADMIN** | `/admin/users` | GET | List internal staff users |
| | `/admin/users` | POST | Onboard a staff user (any of the 5 internal roles) |
| | `/admin/customers` | POST | Register a customer directly (admin-driven, skips self-signup) |
| | `/admin/suppliers` | POST | Register a supplier directly |
| | `/inventory/{warehouseId}` | GET | View warehouse stock levels |
| | `/admin/vendor-performance` | GET | View vendor performance reports |
| **Any authenticated role** | `/shipments/{id}/notify-carrier` | POST | Simulate carrier notification (valid JWT only — no `@RequiresRole`) |
| **WORKER** | *(none yet)* | — | Provisionable today, but no endpoint is gated to `WORKER` specifically — reserved for future flows |

Multiple roles on one endpoint (`OR`, not `AND` — any one of the listed roles is sufficient):
- `GET /inventory/{warehouseId}` → `ADMIN`, `COORDINATOR`, `WAREHOUSE_MANAGER`
- `GET /admin/vendor-performance` → `ADMIN`, `COORDINATOR`

---

## 4. Customer-Accessible Flows (role: `CUSTOMER`)

Everything a signed-in customer can do, end to end:

1. **Sign up** — `POST /auth/signup/customer` (email + country only, unauthenticated) → auto-login.
2. **Log in** (returning) — `POST /auth/otp/request` → `POST /auth/otp/verify`.
3. **View own profile** — `GET /me/customer` (pre-fills `me/update-profile.jsp`).
4. **Complete/update own profile** — `PUT /me/customer` (full name, mobile 1/2, address, country).
5. **Browse the catalog** — `GET /products` (also reachable while logged out).
6. **Place an order** — `POST /orders`, one or more line items; fails with `409` if any item lacks sufficient stock.
7. **View order history** — `GET /orders` (own orders only, most recent first).
8. **View one order** — `GET /orders/{orderId}` (404 if it isn't yours — indistinguishable from a nonexistent id, so one customer can't probe another's order ids).

**Frontend**: all of this lives in `frontend-customer` (`index.jsp` = catalog + place-order, `orders.jsp`, `me/update-profile.jsp`, `auth/login.jsp`, `auth/sign-up.jsp`).

**What a customer cannot do**: anything staff- or supplier-scoped — create/view purchase orders, record GRNs, manage shipments, view inventory, view/manage other users, view vendor performance. All of those endpoints reject a `CUSTOMER` token with `403`.

---

## 5. Supplier-Accessible Flows (role: `VENDOR_REP`)

Everything a signed-in supplier can do, end to end:

1. **Sign up** — `POST /auth/signup/supplier` (email + country only, unauthenticated) → auto-login.
2. **Log in** (returning) — `POST /auth/otp/request` → `POST /auth/otp/verify`.
3. **Complete/update own profile** — `PUT /me/supplier` (business name, mobile 1/2, address, country). *(No `GET /me/supplier` read-back endpoint exists yet — unlike the customer side, this frontend's profile page does not pre-fill; see §6's gap note.)*
4. **Register a product offering** — `POST /suppliers/me/products` (which product, which warehouse, lead time in days) — `supplierId` is always resolved from the JWT, never client-supplied.
5. **View purchase orders placed against them** — `GET /purchase-orders` (own supplier's POs only, most recent first, shows open/completed status).

**Frontend**: all of this lives in `frontend-seller` (`auth/login.jsp`, `auth/sign-up.jsp`, `me/update-profile.jsp`, `products/add-offering.jsp`, `purchase-orders.jsp`).

**What a supplier cannot do**: create purchase orders (that's `COORDINATOR`), record GRNs (`WAREHOUSE_MANAGER`), manage shipments (`CUSTOMS_AGENT`), view/manage users (`ADMIN`), view inventory or vendor-performance reports (`ADMIN`/`COORDINATOR`[/`WAREHOUSE_MANAGER` for inventory]). All reject a `VENDOR_REP` token with `403`.

---

## 6. Known Gaps / Asymmetries

- **No `GET /me/supplier`**: the customer side got a profile read-back endpoint (`GET /me/customer`) so `me/update-profile.jsp` can pre-fill the form; the supplier side never got the equivalent, so `frontend-seller/me/update-profile.jsp` still loads blank every visit, even after saving. Same fix pattern as the customer side would apply if requested.
- **`WORKER` has no dedicated endpoint**: the role can be provisioned via `POST /admin/users`, and `frontend-app`'s dashboard correctly shows "no console actions yet" for it, but no backend flow is currently gated specifically to `WORKER`.
- **`notify-carrier` is intentionally under-scoped**: any authenticated user (any role) can call it — it's the project's bean-managed-transaction (BMT) teaching example, not meant to model a real permission boundary.
- **Admin-driven vs. self-service registration are genuinely separate paths**: `POST /admin/customers|suppliers` (full details in one call, admin-only) and `POST /auth/signup/customer|supplier` (email+country only, unauthenticated, auto-login) both create the same kind of row but serve different flows — an admin onboarding someone on their behalf vs. that person signing themselves up.

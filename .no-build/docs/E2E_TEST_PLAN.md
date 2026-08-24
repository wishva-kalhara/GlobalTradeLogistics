# GlobalTrade Logistics — End-to-End Test Plan

Manual, browser-driven test plan covering all three frontends (`frontend-customer`, `frontend-seller`, `frontend-app`) and every backend flow they call. Each test case (`TC-*`) lists preconditions, steps, and expected results. A traceability table at the end maps every TC back to its frontend page(s) and backend endpoint(s).

---

## 1. Environment Setup

1. From the repo root: `docker compose up --build` (add `-d` to detach). First run pulls/builds images; subsequent runs are faster.
2. Wait for `docker compose logs app` to show `[entrypoint] GlassFish is up. Tailing server log...` and no `SEVERE` lines.
3. Base URLs:
   | Frontend | Base URL |
   |---|---|
   | Customer | `http://localhost:8080/` |
   | Seller | `http://localhost:8080/seller/` |
   | Staff console | `http://localhost:8080/app/` |
   | API (for reference) | `http://localhost:8080/api/v1/` |
4. **Reading OTP codes**: the stack runs with `IS_PROD=false` by default, so no real email is sent — every OTP/onboarding email is logged instead. After requesting a code, run:
   ```
   docker compose logs app | grep OTP_AUTHENTICATION
   ```
   and take the `code=NNNNNN` value from the most recent matching line for the email you used.
5. **Seed data already present on a fresh database**:
   - One ADMIN user: `admin@globaltradelogistics.local` / full name "System Administrator".
   - Catalog: 5 products (Steel Pipe (3m), Industrial Bearing, Hydraulic Hose (5m), Circuit Breaker 32A, Pallet Wrap Roll) all in warehouse `1`, reorder level 20.
   - One demo shipment: id `1`, tracking `TRK-0001`, vessel `VESSEL-ALPHA`, type `SEA`, status `IN_TRANSIT`, warehouse `1`.
6. To reset to a clean slate between test runs: `docker compose down -v` (drops the Postgres volume) then `docker compose up --build` again.

---

## 2. Role / Account Setup Matrix

| Role | How to obtain a session |
|---|---|
| CUSTOMER | Self-signup at `/auth/sign-up.jsp` (customer frontend) |
| VENDOR_REP | Self-signup at `/seller/auth/sign-up.jsp` |
| ADMIN | Pre-seeded — OTP login at `/app/login.jsp` with `admin@globaltradelogistics.local` |
| WORKER / COORDINATOR / CUSTOMS_AGENT / WAREHOUSE_MANAGER | Must be onboarded by an ADMIN first, via `/app/app-user-management.jsp` → "Onboard User" → pick the role. The new user then logs in themselves via OTP at `/app/login.jsp`. |

Session storage (for reference when checking `localStorage` in devtools): `gtl.customer.session`, `gtl.seller.session`, `gtl.app.session` — each holds `{ token, email, role }`.

---

## 3. Cross-Cutting Checks (CC)

These apply across many test cases below; each TC references the relevant `CC-*` IDs instead of restating them.

- **CC-1 (guest redirect)**: visiting a page that requires a session while logged out redirects to that module's login page.
- **CC-2 (session persistence)**: after login/signup, refreshing any page in the same module keeps you signed in (nav shows email + role, no re-login prompt).
- **CC-3 (logout)**: clicking "Log out" in the nav clears `localStorage` for that module's session key and redirects to that module's home page; a subsequent visit to a protected page redirects to login again.
- **CC-4 (expired/invalid token)**: if a page's fetch call gets a 401, it clears the stored session and redirects to that module's login page.
- **CC-5 (wrong-role access)**: a page gated to a specific role redirects a signed-in user with the wrong role to that module's `access-denied.jsp` (customer/seller frontends don't have role-gated pages beyond "must be signed in", so CC-5 only applies to `frontend-app`).
- **CC-6 (backend is the real gate)**: client-side role checks are UX only. For at least one page per role, confirm that calling the underlying endpoint directly with a wrong-role token (e.g. via browser devtools `fetch` in the console) still returns 401/403 — the JSP check is not what's actually protecting the data.

---

## 4. `frontend-customer` Test Cases

### TC-C01 — Guest landing page
- **Pre:** logged out, no `gtl.customer.session`.
- **Steps:** visit `/index.jsp`.
- **Expected:** `index.jsp` *is* the product catalog (no separate placeholder/dashboard page) — the product grid loads and is browsable while logged out. Placing an order still requires being signed in (TC-C09).

### TC-C02 — Customer self-signup (happy path)
- **Pre:** an email not already registered as a customer.
- **Steps:** `/auth/sign-up.jsp` → enter email + select a country → submit.
- **Expected:** signup succeeds, session is stored automatically (auto-login, no OTP step), browser redirects to `/me/update-profile.jsp`.

### TC-C03 — Duplicate customer signup
- **Pre:** the email from TC-C02 already exists.
- **Steps:** repeat TC-C02 with the same email.
- **Expected:** error alert shown (something like "account already exists"); no session created; page not redirected.

### TC-C04 — Returning customer OTP login
- **Pre:** an existing customer account (e.g. from TC-C02).
- **Steps:** `/auth/login.jsp` → enter email → "Send OTP" → look up code (§1.4) → enter code → "Verify".
- **Expected:** two-step form transitions from email entry to code entry with a confirmation message; on verify, redirects to `/index.jsp` (the product catalog) — see CC-2. My Orders/Update Profile remain reachable via the nav's Account menu.

### TC-C05 — Profile completion
- **Pre:** signed in as a customer.
- **Steps:** `/me/update-profile.jsp` → fill full name, mobile 1, mobile 2 (optional), address, country → "Save profile".
- **Expected:** country dropdown is populated from the countries list on page load; success message shown after save; reloading the page keeps you signed in (CC-2).
- **Also verify (CC-4):** if the stored token is deleted/corrupted in devtools before submitting, the save attempt redirects to login.

### TC-C06 — Browse products (grid/search detail)
- **Pre:** none (page is public — works logged out too).
- **Steps:** `/index.jsp` (same page as TC-C01).
- **Expected:** product grid loads (skeleton placeholders briefly, then real cards); each card shows name, description, price, and a stock badge ("In stock" / "Only N left" / "Out of stock"). Search box filters the grid client-side by name/description. Out-of-stock items have a disabled quantity control.

### TC-C07 — Place an order (single item)
- **Pre:** signed in as a customer.
- **Steps:** `/index.jsp` → increment one product's quantity via the +/− stepper or type a number → "Place Order" (bottom cart bar).
- **Expected:** success message with the new order id and total; quantities reset to 0 after success; the sticky cart bar total updates live as quantities change before submitting.

### TC-C08 — Place an order (multi-item) and insufficient stock
- **Pre:** signed in as a customer.
- **Steps (a):** select quantities for 2+ different products → place order → expect success with a combined total.
- **Steps (b):** set a quantity higher than a product's available stock → place order.
- **Expected (b):** error shown (insufficient inventory), no order created, stock unchanged.

### TC-C09 — Place order while logged out
- **Pre:** logged out.
- **Steps:** on `/index.jsp`, pick a quantity and click "Place Order".
- **Expected:** redirected to `/auth/login.jsp` (CC-1) instead of an API error.

### TC-C10 — Order history
- **Pre:** a customer with at least one placed order (from TC-C07/C08).
- **Steps:** `/orders.jsp`.
- **Expected:** most-recent-first list; each card shows order id, status badge (color varies by `PLACED`/`SHIPPED`/`DELIVERED`/`CANCELLED`), item lines with qty × product name, and total. A brand-new customer with zero orders instead sees an empty state with a "Browse products" call-to-action.

### TC-C11 — Nav bar and logout
- **Steps:** while signed in, open the "Account" menu → confirm "Update Profile" and "My Orders" links work → click "Log out".
- **Expected:** per CC-3.

---

## 5. `frontend-seller` Test Cases

> **Known inconsistency (not a defect to "fix" during testing, just expected current behavior):** unlike the customer frontend (whose `index.jsp` is the product catalog itself) and the staff frontend (whose `index.jsp` is a role-based dashboard), the seller frontend's `index.jsp` is still an unbuilt placeholder — **both** login and signup redirect to `me/update-profile.jsp` every time (not just on first signup). Treat this as the current expected result, not a bug, when executing TC-SE04 below.

### TC-SE01 — Guest landing page
- **Steps:** visit `/seller/index.jsp` while logged out.
- **Expected:** placeholder landing page (no functional content yet — just confirm it loads without error).

### TC-SE02 — Supplier self-signup
- **Steps:** `/seller/auth/sign-up.jsp` → email + country → submit.
- **Expected:** auto-login, redirect to `/seller/me/update-profile.jsp` — same shape as TC-C02.

### TC-SE03 — Duplicate supplier signup
- **Steps:** repeat TC-SE02 with the same email.
- **Expected:** 409-style error alert, same shape as TC-C03.

### TC-SE04 — Returning seller OTP login
- **Steps:** `/seller/auth/login.jsp` → OTP flow → verify.
- **Expected:** redirects to `/seller/me/update-profile.jsp` (see the callout above — the customer frontend instead redirects returning logins to its product-catalog landing page, `/index.jsp`).

### TC-SE05 — Profile completion
- **Steps:** `/seller/me/update-profile.jsp` → business/full name, mobile 1/2, address, country → save.
- **Expected:** same shape as TC-C05 (country dropdown populated, success message, session persists).

### TC-SE06 — Add a product offering
- **Pre:** signed in as VENDOR_REP.
- **Steps:** Account menu → "Add Product Offering" (`/seller/products/add-offering.jsp`) → select a product, warehouse id (defaults to `1`), lead time in days → submit.
- **Expected:** success message; form resets (warehouse id resets back to `1`).
- **Validation:** submitting with lead time left blank/negative is rejected by the browser's `min="0"` constraint before it ever reaches the server.

### TC-SE07 — My Purchase Orders (empty state)
- **Pre:** a brand-new VENDOR_REP with no POs placed against them yet.
- **Steps:** Account menu → "My Purchase Orders" (`/seller/purchase-orders.jsp`).
- **Expected:** empty state — "No purchase orders yet".

### TC-SE08 — My Purchase Orders (populated)
- **Pre:** at least one PO has been created against this supplier by a COORDINATOR (see §7, TC-X01).
- **Steps:** reload `/seller/purchase-orders.jsp`.
- **Expected:** list shows PO id, status pill ("Open" / "Completed"), quantity × product name, total price, most recent first.

### TC-SE09 — Nav and logout
- Same shape as TC-C11, using `gtl.seller.session` and the `/seller/...` paths.

---

## 6. `frontend-app` (Staff Console) Test Cases

### 6.1 — Guest / general

#### TC-A01 — Guest landing page
- **Steps:** visit `/app/index.jsp` while logged out.
- **Expected:** hero card explaining accounts are admin-provisioned, with a "Log in" button — no self-signup link anywhere in this frontend.

#### TC-A02 — Staff OTP login
- **Steps:** `/app/login.jsp` → OTP flow with any provisioned staff email → verify.
- **Expected:** redirects to `/app/index.jsp`, which renders a **role-specific dashboard** (see 6.2–6.6 below for what each role sees).

### 6.2 — ADMIN

**Setup:** log in as `admin@globaltradelogistics.local`.

#### TC-A03 — ADMIN dashboard
- **Expected:** `/app/index.jsp` shows 3 cards: "Application User Management", "Vendor Performance Report", "Warehouse Inventory". Same 3 links appear in the nav's Account menu.

#### TC-A04 — Onboard a staff user (all 5 roles)
- **Steps:** `/app/app-user-management.jsp` → "Onboard User" → fill email/full name → for each of ADMIN/WORKER/COORDINATOR/CUSTOMS_AGENT/WAREHOUSE_MANAGER, submit once.
- **Expected:** each submission succeeds, modal closes, success message shown, and the users table refreshes to include the new row with the correct role badge. Repeat once per role so every role below has a test account.

#### TC-A05 — Register a customer directly
- **Steps:** "Register Customer" → email, full name, mobile, address, country → submit.
- **Expected:** success message ("...an onboarding email has been queued"); modal closes. (This customer does **not** appear in the staff users table — that table is staff-only. Verify separately via the customer frontend that this account can now log in with OTP.)

#### TC-A06 — Register a supplier directly
- Same shape as TC-A05, using "Register Supplier" and verifying via the seller frontend afterward.

#### TC-A07 — Users table
- **Steps:** reload `/app/app-user-management.jsp`.
- **Expected:** table lists every onboarded staff user (email, full name, role badge) — persists across reloads.

#### TC-A08 — Vendor Performance Report (as ADMIN)
- **Steps:** open "Vendor Performance Report".
- **Expected under default dev config:** empty state ("No vendor performance reports yet") — this report is only populated once the weekly recompute timer has run *and* the app is deployed with `IS_PROD=true` (monitoring-svc's consumer isn't active otherwise). An empty result here is correct, not a bug.

#### TC-A09 — Warehouse Inventory (as ADMIN)
- **Steps:** open "Warehouse Inventory", leave warehouse id at `1`, "Load".
- **Expected:** table of all 5 seeded products with qty/reorder level/unit price/last-updated; any row where qty < reorder level is highlighted and qty shows "(low)". Try a nonexistent warehouse id (e.g. `999`) → expect the empty state.

### 6.3 — COORDINATOR

**Setup:** log in as the COORDINATOR account from TC-A04.

#### TC-A10 — COORDINATOR dashboard
- **Expected:** cards for "Create Purchase Order", "Vendor Performance Report", "Warehouse Inventory" (no "Application User Management").

#### TC-A11 — Create a purchase order
- **Pre:** know a valid supplier id (e.g. from a seller signup — check via `psql` if needed, or just use `1` if it's the first supplier registered).
- **Steps:** `/app/purchase-orders/create.jsp` → supplier id, pick a product, quantity → "Create Purchase Order".
- **Expected:** result card appears with PO id, product name, quantity, and a computed total price; form resets. Hand the PO id to a WAREHOUSE_MANAGER for TC-A13.

#### TC-A12 — Access denied for other roles' pages
- **Steps:** while logged in as COORDINATOR, navigate directly to `/app/purchase-orders/record-grn.jsp` and `/app/shipments/manage.jsp`.
- **Expected:** both redirect to `/app/access-denied.jsp` (CC-5).

### 6.4 — WAREHOUSE_MANAGER

**Setup:** log in as the WAREHOUSE_MANAGER account from TC-A04.

#### TC-A13 — Record a GRN
- **Pre:** an open PO id from TC-A11.
- **Steps:** `/app/purchase-orders/record-grn.jsp` → PO id, quantity received (matching or less than the PO's requested qty) → "Record GRN".
- **Expected:** result card shows the PO marked "Completed"; cross-check `/app/inventory.jsp` afterward — the received product's qty increased by the recorded amount.

#### TC-A14 — WAREHOUSE_MANAGER dashboard and page access
- **Expected:** dashboard shows "Record GRN" and "Warehouse Inventory" only. Direct navigation to `/app/purchase-orders/create.jsp` and `/app/shipments/manage.jsp` → access-denied.

### 6.5 — CUSTOMS_AGENT

**Setup:** log in as the CUSTOMS_AGENT account from TC-A04.

#### TC-A15 — CUSTOMS_AGENT dashboard
- **Expected:** only "Manage Shipments" card/link.

#### TC-A16 — Shipment lookup and status update
- **Steps:** `/app/shipments/manage.jsp` → enter shipment id `1` → "Load" → change status dropdown (e.g. to `DELAYED`) → "Update".
- **Expected:** shipment card populates with tracking number, vessel, type, warehouse, and current status; after update, the status badge changes and a success message shows.

#### TC-A17 — Create a customs clearance record
- **Steps:** with shipment `1` loaded, enter a declaration number → "Create Record".
- **Expected:** success message; form resets.

#### TC-A18 — Notify carrier system
- **Steps:** with shipment `1` loaded, click "Notify Carrier".
- **Expected:** success message including a carrier reference (`CARRIER-<uuid>` shape); the "Carrier ref" field on the shipment card updates to that value.

#### TC-A19 — Access denied for other roles' pages
- **Steps:** navigate to `/app/purchase-orders/create.jsp`, `/app/purchase-orders/record-grn.jsp`, `/app/inventory.jsp`.
- **Expected:** all three redirect to access-denied.

### 6.6 — WORKER

**Setup:** log in as the WORKER account from TC-A04.

#### TC-A20 — WORKER dashboard
- **Expected:** dashboard shows "Your role doesn't have any console actions yet" — WORKER has no pages built (this is intentional per the current scope, not a bug). Nav's Account menu likewise shows "No actions available".

### 6.7 — Backend-enforced authorization (CC-6)

#### TC-A21 — Direct API calls bypassing the frontend
- **Steps:** open browser devtools console while signed in as a low-privilege role (e.g. WAREHOUSE_MANAGER) and run a `fetch()` against an endpoint that role shouldn't reach, e.g.:
  ```js
  fetch("/api/v1/admin/vendor-performance", { headers: { Authorization: "Bearer " + JSON.parse(localStorage.getItem("gtl.app.session")).token } }).then(r => r.status)
  ```
- **Expected:** `403`, proving the JSP's client-side role check isn't what's actually protecting the endpoint.

---

## 7. Cross-Actor Integration Chains

These exercise a full business flow across multiple roles/frontends in sequence — run them in order.

### TC-X01 — Full procurement chain
1. As a VENDOR_REP (seller frontend), add a product offering for one of the 5 catalog products (TC-SE06).
2. As a COORDINATOR (staff console), create a PO against that supplier for the same product (TC-A11). Note the PO id and the product's current inventory qty (check via ADMIN/COORDINATOR's Warehouse Inventory page first).
3. As a WAREHOUSE_MANAGER, record a GRN for that PO (TC-A13).
4. Back as the VENDOR_REP, reload "My Purchase Orders" (TC-SE08) — confirm it now shows "Completed".
5. As ADMIN/COORDINATOR, reload Warehouse Inventory — confirm the product's qty increased by the GRN quantity.

### TC-X02 — Full customer order chain
1. As a CUSTOMER, note a product's current stock on `/index.jsp`.
2. Place an order for a few units of that product (TC-C07).
3. Reload `/index.jsp` — confirm the displayed stock decreased by the ordered quantity.
4. Check `/orders.jsp` — the new order appears with status `PLACED`.
5. As ADMIN/COORDINATOR, check Warehouse Inventory for the same product — confirm the same qty decrease is reflected there too (same underlying `inventory` table).

### TC-X03 — Shipment lifecycle
1. As a CUSTOMS_AGENT, load shipment `1` (initially `IN_TRANSIT`).
2. Update its status to `DELAYED`, then create a customs record, then notify the carrier (TC-A16–A18 in sequence).
3. Reload the shipment lookup — confirm the status and carrier ref both persisted.
4. *(Backend-only, not directly UI-testable)*: a 15-minute declarative timer also polls `IN_TRANSIT` shipments and may flip status to `DELIVERED` on its own — if this test is left running long enough, a re-load of the same shipment could show a status change with no user action. Not something to actively wait for during a manual pass; just don't be surprised if the demo shipment's status has changed between test sessions.

---

## 8. Environment-Dependent Notes (not defects)

- **Vendor Performance Report** (TC-A08) is expected to be empty under the default `IS_PROD=false` dev configuration — the weekly recompute timer's audit trail only becomes durable once `monitoring-svc`'s consumer is active in a `IS_PROD=true` deployment.
- **Idempotency key reuse** on `PUT /shipments/{id}/status`: submitting the exact same `idempotencyKey` twice does not currently short-circuit the second call under the default dev config, for the same `IS_PROD=false` reason above. Not testable as a "duplicate is ignored" behavior in the UI today (the UI always generates a fresh key per click anyway, so this is only reachable via manual devtools replay).
- **Notification emails**: nothing is actually sent (Mailtrap integration is pending credentials) — every "onboarding email queued" success message reflects a queued/logged notification, not a delivered email. Confirming actual delivery is out of scope until that's wired up.

---

## 9. Traceability Matrix

| TC | Frontend page(s) | Backend endpoint(s) | Role required |
|---|---|---|---|
| TC-C01 | `/index.jsp` | `GET /products` | none |
| TC-C02 | `/auth/sign-up.jsp` | `POST /auth/signup/customer` | none |
| TC-C03 | `/auth/sign-up.jsp` | `POST /auth/signup/customer` | none |
| TC-C04 | `/auth/login.jsp`, `/index.jsp` | `POST /auth/otp/request`, `POST /auth/otp/verify` | none → CUSTOMER |
| TC-C05 | `/me/update-profile.jsp` | `GET /countries`, `PUT /me/customer` | CUSTOMER |
| TC-C06 | `/index.jsp` | `GET /products` | none |
| TC-C07/C08/C09 | `/index.jsp` | `POST /orders` | CUSTOMER |
| TC-C10 | `/orders.jsp` | `GET /orders` | CUSTOMER |
| TC-C11 | nav include | — | CUSTOMER |
| TC-SE01 | `/seller/index.jsp` | — | none |
| TC-SE02/03 | `/seller/auth/sign-up.jsp` | `POST /auth/signup/supplier` | none |
| TC-SE04 | `/seller/auth/login.jsp` | `POST /auth/otp/request`, `/verify` | none → VENDOR_REP |
| TC-SE05 | `/seller/me/update-profile.jsp` | `GET /countries`, `PUT /me/supplier` | VENDOR_REP |
| TC-SE06 | `/seller/products/add-offering.jsp` | `GET /products`, `POST /suppliers/me/products` | VENDOR_REP |
| TC-SE07/08 | `/seller/purchase-orders.jsp` | `GET /purchase-orders` | VENDOR_REP |
| TC-SE09 | nav include | — | VENDOR_REP |
| TC-A01/02 | `/app/index.jsp`, `/app/login.jsp` | `POST /auth/otp/request`, `/verify` | none |
| TC-A03 | `/app/index.jsp` | — | ADMIN |
| TC-A04 | `/app/app-user-management.jsp` | `GET /admin/users`, `POST /admin/users` | ADMIN |
| TC-A05 | `/app/app-user-management.jsp` | `POST /admin/customers` | ADMIN |
| TC-A06 | `/app/app-user-management.jsp` | `POST /admin/suppliers` | ADMIN |
| TC-A07 | `/app/app-user-management.jsp` | `GET /admin/users` | ADMIN |
| TC-A08 | `/app/vendor-performance.jsp` | `GET /admin/vendor-performance` | ADMIN, COORDINATOR |
| TC-A09 | `/app/inventory.jsp` | `GET /inventory/{warehouseId}` | ADMIN, COORDINATOR, WAREHOUSE_MANAGER |
| TC-A10/12 | `/app/index.jsp`, other `/app/*` pages | — | COORDINATOR |
| TC-A11 | `/app/purchase-orders/create.jsp` | `GET /products`, `POST /purchase-orders` | COORDINATOR |
| TC-A13/14 | `/app/purchase-orders/record-grn.jsp` | `POST /purchase-orders/{id}/grn` | WAREHOUSE_MANAGER |
| TC-A15 | `/app/index.jsp` | — | CUSTOMS_AGENT |
| TC-A16 | `/app/shipments/manage.jsp` | `GET /shipments/{id}`, `PUT /shipments/{id}/status` | CUSTOMS_AGENT |
| TC-A17 | `/app/shipments/manage.jsp` | `POST /shipments/{id}/customs` | CUSTOMS_AGENT |
| TC-A18 | `/app/shipments/manage.jsp` | `POST /shipments/{id}/notify-carrier` | CUSTOMS_AGENT |
| TC-A19 | various `/app/*` | — | CUSTOMS_AGENT |
| TC-A20 | `/app/index.jsp` | — | WORKER |
| TC-A21 | devtools console | any role-gated endpoint | any |
| TC-X01 | seller + staff pages above | `POST /suppliers/me/products`, `POST /purchase-orders`, `POST /purchase-orders/{id}/grn`, `GET /purchase-orders`, `GET /inventory/{id}` | VENDOR_REP, COORDINATOR, WAREHOUSE_MANAGER |
| TC-X02 | `/index.jsp`, `/orders.jsp`, `/app/inventory.jsp` | `POST /orders`, `GET /orders`, `GET /products`, `GET /inventory/{id}` | CUSTOMER, (ADMIN/COORDINATOR to verify) |
| TC-X03 | `/app/shipments/manage.jsp` | all 4 shipment endpoints | CUSTOMS_AGENT |

**Endpoint not covered by any TC (intentionally):** `GET /orders/{orderId}` — no dedicated detail page exists because `orders.jsp`'s list already renders every order's full line-item detail inline; a drill-down page would show nothing new.

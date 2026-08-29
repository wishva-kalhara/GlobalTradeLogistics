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
6. To reset to a clean slate between test runs: `docker compose down -v` (drops the MySQL volume) then `docker compose up --build` again.

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

`frontend-seller/index.jsp` is a role-based dashboard: a guest card with a "Log in" link, or — signed in — a welcome header, purchase-order stats/charts, and cards for "My Purchase Orders", "Create Shipment", "My Shipments", and "Add Product Offering" (the last two cards, and the profile-aware login redirect in TC-SE04, are new).

### TC-SE01 — Guest landing page
- **Steps:** visit `/seller/index.jsp` while logged out.
- **Expected:** guest card with a "Log in" link.

### TC-SE02 — Supplier self-signup
- **Steps:** `/seller/auth/sign-up.jsp` → email + country → submit.
- **Expected:** auto-login, redirect to `/seller/me/update-profile.jsp` — same shape as TC-C02.

### TC-SE03 — Duplicate supplier signup
- **Steps:** repeat TC-SE02 with the same email.
- **Expected:** 409-style error alert, same shape as TC-C03.

### TC-SE04 — Returning seller OTP login (profile-aware redirect)
- **Pre (a):** a VENDOR_REP whose profile has never been completed (fresh signup, no `fullName` saved).
- **Steps (a):** `/seller/auth/login.jsp` → OTP flow → verify.
- **Expected (a):** the page calls `GET /api/v1/me/supplier`; since `fullName` is empty, redirects to `/seller/me/update-profile.jsp`.
- **Pre (b):** a VENDOR_REP with a completed profile (e.g. after TC-SE05).
- **Steps (b):** repeat the login.
- **Expected (b):** redirects straight to `/seller/index.jsp` (the dashboard) instead.

### TC-SE05 — Profile completion
- **Steps:** `/seller/me/update-profile.jsp` → business/full name, mobile 1/2, address, country → save.
- **Expected:** same shape as TC-C05 (country dropdown populated using country **codes** as option values — matching the fix applied to the customer side — success message, session persists). Reloading the page pre-fills every field from `GET /api/v1/me/supplier`, including the country select.

### TC-SE06 — Add a product offering
- **Pre:** signed in as VENDOR_REP.
- **Steps:** Account menu → "Add Product Offering" (`/seller/products/add-offering.jsp`) → select a product, select a warehouse from the **Warehouse** dropdown (populated from `GET /api/v1/inventory/warehouses`, labeled "Warehouse {id} ({country})"), lead time in days → submit.
- **Expected:** success message; form resets, including the warehouse dropdown back to "Select a warehouse…".

### TC-SE10 — Create Shipment
- **Pre:** signed in as VENDOR_REP, with at least one open PO placed against them with no shipment yet (see TC-A11).
- **Steps:** Account menu (or dashboard card) → "Create Shipment" (`/seller/shipments/create.jsp`) → the **Purchase order** dropdown is populated from `GET /api/v1/purchase-orders/shippable` → select the PO, enter Tracking number / Vessel ID, pick a Type → "Create Shipment".
- **Expected:** result card shows a new shipment id, the PO id, and status `CREATED`. Reloading the page — that PO no longer appears in the dropdown (it now has a shipment).
- **Also verify:** if no POs are shippable, the empty state shows and the form is hidden.

### TC-SE11 — My Shipments
- **Pre:** at least one shipment created (TC-SE10).
- **Steps:** Account menu → "My Shipments" (`/seller/shipments/list.jsp`).
- **Expected:** a card per shipment — id, linked PO id, tracking/vessel/type, a color-coded status badge, and its customs status ("not yet filed" if none recorded). Updates made by staff (status changes, customs clearance) are visible here on reload without the supplier needing any staff-console access.

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
- **Expected:** `/app/index.jsp` shows 3 cards: "Application User Management", "Warehouse Inventory", "Shipments" — the "Vendor Performance Report" **card** was intentionally removed from the dashboard, but the link still exists in the nav's Account menu (which lists 4 items: those 3 plus "Vendor Performance Report") — this asymmetry is intentional, not a bug. Below the cards, an "Analytics" section shows total sales/orders stat tiles and orders-by-status / top-products-by-revenue / vendor-on-time-rate charts (backed by `GET /admin/sales-summary` and `GET /admin/vendor-performance`).

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
- **Steps:** open "Warehouse Inventory" — the **Warehouse** field is a dropdown (populated from `GET /inventory/warehouses`, labeled "Warehouse {id} ({country})"), not a numeric input — it auto-loads that warehouse's stock as soon as the page loads (no separate "Load" button).
- **Expected:** table of all 5 seeded products with qty/reorder level/unit price/last-updated; any row where qty < reorder level is highlighted and qty shows "(low)". (With only one seeded warehouse, there's nothing to switch to for the empty-state check — the dropdown itself would need a second warehouse to exist.)

### 6.3 — COORDINATOR

**Setup:** log in as the COORDINATOR account from TC-A04.

#### TC-A10 — COORDINATOR dashboard
- **Expected:** cards for "Create Purchase Order", "Warehouse Inventory", "Shipments" (no "Application User Management"; "Vendor Performance Report" is nav-only, per TC-A03's note), plus the same Analytics section as ADMIN.

#### TC-A11 — Create a purchase order
- **Pre:** a supplier with a completed profile who has registered at least one product offering (TC-SE06) for the product you'll pick.
- **Steps:** `/app/purchase-orders/create.jsp` → pick a **Product** first (the Supplier dropdown starts disabled, showing "Select a product first…") → the Supplier dropdown then loads from `GET /admin/suppliers?productId={id}` and enables, listing only suppliers who've registered an offering for that product → pick one → enter Quantity → "Create Purchase Order".
- **Expected:** result card appears with PO id, product name, quantity, and a computed total price; form resets (Supplier dropdown returns to its disabled "Select a product first…" state). Hand the PO id to the supplier for TC-SE10.
- **Also verify:** picking a product no supplier has registered an offering for shows the hint "No supplier has registered an offering for this product yet." and leaves the Supplier dropdown showing "No suppliers for this product".

#### TC-A12 — Access denied for other roles' pages
- **Steps:** while logged in as COORDINATOR, navigate directly to `/app/purchase-orders/record-grn.jsp` and `/app/shipments/manage.jsp`.
- **Expected:** both redirect to `/app/access-denied.jsp` (CC-5).

### 6.4 — WAREHOUSE_MANAGER

**Setup:** log in as the WAREHOUSE_MANAGER account from TC-A04.

#### TC-A13 — Record a GRN
- **Pre:** a shipment that's `DELIVERED` **and** whose customs status is `CLEARED` — i.e. the full ship → customs → GRN pipeline from [`E2E_TEST_FLOW.md`](./E2E_TEST_FLOW.md) has reached that point for a PO from TC-A11.
- **Steps:** `/app/purchase-orders/record-grn.jsp` → the **Delivered shipment** dropdown is populated from `GET /shipments/awaiting-grn` (shows "Shipment #{id} (PO #{poId}, {trackingNumber})") — pick it, enter quantity received (matching or less than the PO's requested qty) → "Record GRN".
- **Expected:** result card shows the PO marked "Completed". Cross-check: `/app/inventory.jsp` — the received product's qty increased by the recorded amount; the shipment no longer appears in this page's dropdown (its option is removed on success, and its PO is now complete so `awaiting-grn` won't return it again — this is what makes a duplicate GRN impossible from the UI).
- **If the dropdown is empty** ("No delivered shipments are awaiting a GRN right now"): that's correct if no shipment has reached `DELIVERED` yet — see TC-A16 for advancing one there. Selecting one that's `DELIVERED` but not yet customs-`CLEARED` and submitting shows an error ("Customs clearance must be completed (CLEARED)...") rather than succeeding.

#### TC-A14 — WAREHOUSE_MANAGER dashboard and page access
- **Expected:** dashboard shows "Record GRN", "Warehouse Inventory", and "Shipments" (read-only list). Direct navigation to `/app/purchase-orders/create.jsp` and `/app/shipments/manage.jsp` → access-denied.

#### TC-A14b — Shipments (read-only, staff-wide)
- **Pre:** signed in as ADMIN, COORDINATOR, WAREHOUSE_MANAGER, or CUSTOMS_AGENT.
- **Steps:** open "Shipments" (`/app/shipments/list.jsp`).
- **Expected:** a table of every shipment (`GET /shipments`), most recent first — shipment id, linked PO id, tracking number, a color-coded status badge (including green for `COMPLETED`), and customs status. Gives non-CUSTOMS_AGENT roles visibility into shipments without needing lookup-by-id access to `/app/shipments/manage.jsp`.

### 6.5 — CUSTOMS_AGENT

**Setup:** log in as the CUSTOMS_AGENT account from TC-A04.

#### TC-A15 — CUSTOMS_AGENT dashboard
- **Expected:** two cards/links: "Manage Shipments" (interactive) and "All Shipments" (read-only list, same page as TC-A14b).

#### TC-A16 — Shipment lookup and status update, with state-gated controls
- **Steps:** `/app/shipments/manage.jsp` → the **Shipment** field is a dropdown (populated from `GET /shipments`, labeled "Shipment #{id} — {trackingNumber} ({status})") — selecting one auto-loads it (no separate "Load" button).
- **Expected on load:** shipment card populates with tracking number, vessel, type, warehouse, linked PO id, customs status, and carrier ref.
- **Status dropdown gating:** if the loaded shipment's status is `CREATED`, the "New status" dropdown offers **only** `IN_TRANSIT` — no other option. For any other status, it offers `IN_TRANSIT`/`DELIVERED`/`DELAYED` (never back to `CREATED`).
- **Customs section gating:** the "Declaration number" input, "Create Record" button, "Update customs status" select, and its "Update" button are all **disabled** unless the shipment's status is currently `IN_TRANSIT`.
- **Steps:** select `IN_TRANSIT` → "Update".
- **Expected:** status badge changes to `IN_TRANSIT`, success message shows, and the customs-section controls become enabled, and the dropdown's own label for this shipment updates to reflect the new status.

#### TC-A17 — Create and clear a customs record (with lock-in behavior)
- **Pre:** shipment status is `IN_TRANSIT` (TC-A16).
- **Steps:** enter a declaration number → "Create Record".
- **Expected:** success message; the shipment reloads and now shows customs status `PENDING`. The "Declaration number" input now shows that value **prefilled**, and — along with "Create Record" — is now **disabled**: once a declaration number is set, it can't be re-entered or replaced through this form.
- **Steps:** "Update customs status" → select `CLEARED` → "Update".
- **Expected:** success message; customs status shows `CLEARED`. The "Update customs status" select and its "Update" button are now **disabled** — once `CLEARED`, it can't be reverted through this form. The "Notify Carrier" button (previously disabled) becomes enabled.
- **Also verify:** attempting a GRN for this shipment before doing this (`/app/purchase-orders/record-grn.jsp`) fails with a "Customs clearance must be completed (CLEARED)..." error even while the shipment shows as `DELIVERED`.

#### TC-A18 — Notify carrier system
- **Pre:** customs status is `CLEARED` (TC-A17) — the button is disabled otherwise.
- **Steps:** click "Notify Carrier".
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

### TC-X01 — Full procurement chain (create PO → ship → customs → GRN)
This is the flow [`E2E_TEST_FLOW.md`](./E2E_TEST_FLOW.md) walks through in full detail, with exact field values and 29 numbered test points — run that document for a rigorous pass. Summarized here for the traceability matrix:
1. As a VENDOR_REP (seller frontend), add a product offering for one of the 5 catalog products (TC-SE06).
2. As a COORDINATOR (staff console), create a PO against that supplier for the same product (TC-A11). Note the PO id and the product's current inventory qty (check via ADMIN/COORDINATOR's Warehouse Inventory page first).
3. Back as the VENDOR_REP, create a shipment for that PO (TC-SE10).
4. As a CUSTOMS_AGENT, advance the shipment `CREATED → IN_TRANSIT`, create and clear its customs record, then advance it to `DELIVERED` (TC-A16/A17).
5. As a WAREHOUSE_MANAGER, record a GRN for that shipment (TC-A13) — only possible now that it's `DELIVERED` + `CLEARED`.
6. Back as the VENDOR_REP, reload "My Purchase Orders" (TC-SE08) — confirm it now shows "Completed" — and "My Shipments" (TC-SE11) — confirm the shipment shows `COMPLETED`.
7. As ADMIN/COORDINATOR, reload Warehouse Inventory — confirm the product's qty increased by the GRN quantity.

### TC-X02 — Full customer order chain
1. As a CUSTOMER, note a product's current stock on `/index.jsp`.
2. Place an order for a few units of that product (TC-C07).
3. Reload `/index.jsp` — confirm the displayed stock decreased by the ordered quantity.
4. Check `/orders.jsp` — the new order appears with status `PLACED`.
5. As ADMIN/COORDINATOR, check Warehouse Inventory for the same product — confirm the same qty decrease is reflected there too (same underlying `inventory` table).
6. As ADMIN/COORDINATOR, reload the dashboard — confirm the Analytics section's total sales/orders-by-status reflect this new order (this is the customer-`orders`-table analytics; it's unaffected by the PO/GRN chain in TC-X01, which is a separate `purchase_orders` table).

### TC-X03 — Shipment lifecycle and state-machine gating
1. As a CUSTOMS_AGENT, load the one legacy seeded shipment (id `1`, initially `IN_TRANSIT`, pre-dates the ship→customs→GRN link so it has no PO).
2. Update its status to `DELAYED`, then back to `IN_TRANSIT` (both valid from a non-`CREATED` state) — confirm the dropdown never offers `CREATED` as a target.
3. With it `IN_TRANSIT`, create a customs record with a declaration number — confirm the input then shows that value and is disabled (can't be edited/replaced).
4. Advance customs status to `CLEARED` — confirm the customs-status select and its Update button become disabled, and "Notify Carrier" (previously disabled) becomes enabled.
5. Click "Notify Carrier" — confirm success and a carrier reference is stored.
6. Reload the shipment lookup — confirm status, customs status, declaration number, and carrier ref all persisted, and the same controls are still disabled per steps 3–4.
7. **Negative check**: pick a *different*, freshly-created (`CREATED`-status) shipment (from TC-SE10) — confirm its status dropdown offers only `IN_TRANSIT`, and its customs-section controls are all disabled until you advance it there.
8. *(Backend-only, not directly UI-testable)*: a 15-minute declarative timer also polls `IN_TRANSIT` shipments and may flip status to `DELIVERED` on its own — don't be surprised if a shipment's status has changed between test sessions with no user action.

---

## 8. Environment-Dependent Notes (not defects)

- **Vendor Performance Report** (TC-A08) is expected to be empty under the default `IS_PROD=false` dev configuration — the weekly recompute timer's audit trail only becomes durable once `monitoring-svc`'s consumer is active in a `IS_PROD=true` deployment.
- **Idempotency key reuse** on `PUT /shipments/{id}/status`: submitting the exact same `idempotencyKey` twice in the same JVM session short-circuits the second call — the UI always generates a fresh UUID per click, so this is only reachable via manual devtools replay.
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
| TC-SE04 | `/seller/auth/login.jsp` | `POST /auth/otp/request`, `/verify`, `GET /me/supplier` | none → VENDOR_REP |
| TC-SE05 | `/seller/me/update-profile.jsp` | `GET /countries`, `GET /me/supplier`, `PUT /me/supplier` | VENDOR_REP |
| TC-SE06 | `/seller/products/add-offering.jsp` | `GET /products`, `GET /inventory/warehouses`, `POST /suppliers/me/products` | VENDOR_REP |
| TC-SE07/08 | `/seller/purchase-orders.jsp` | `GET /purchase-orders` | VENDOR_REP |
| TC-SE09 | nav include | — | VENDOR_REP |
| TC-SE10 | `/seller/shipments/create.jsp` | `GET /purchase-orders/shippable`, `POST /purchase-orders/{poId}/shipment` | VENDOR_REP |
| TC-SE11 | `/seller/shipments/list.jsp` | `GET /shipments/mine` | VENDOR_REP |
| TC-A01/02 | `/app/index.jsp`, `/app/login.jsp` | `POST /auth/otp/request`, `/verify` | none |
| TC-A03 | `/app/index.jsp` | `GET /admin/sales-summary`, `GET /admin/vendor-performance` | ADMIN |
| TC-A04 | `/app/app-user-management.jsp` | `GET /admin/users`, `POST /admin/users` | ADMIN |
| TC-A05 | `/app/app-user-management.jsp` | `POST /admin/customers` | ADMIN |
| TC-A06 | `/app/app-user-management.jsp` | `POST /admin/suppliers` | ADMIN |
| TC-A07 | `/app/app-user-management.jsp` | `GET /admin/users` | ADMIN |
| TC-A08 | `/app/vendor-performance.jsp` | `GET /admin/vendor-performance` | ADMIN, COORDINATOR |
| TC-A09 | `/app/inventory.jsp` | `GET /inventory/warehouses`, `GET /inventory/{warehouseId}` | ADMIN, COORDINATOR, WAREHOUSE_MANAGER |
| TC-A10/12 | `/app/index.jsp`, other `/app/*` pages | `GET /admin/sales-summary`, `GET /admin/vendor-performance` | COORDINATOR |
| TC-A11 | `/app/purchase-orders/create.jsp` | `GET /products`, `GET /admin/suppliers?productId=`, `POST /purchase-orders` | COORDINATOR |
| TC-A13 | `/app/purchase-orders/record-grn.jsp` | `GET /shipments/awaiting-grn`, `POST /shipments/{id}/grn` | WAREHOUSE_MANAGER |
| TC-A14/A14b | `/app/index.jsp`, `/app/shipments/list.jsp` | `GET /shipments` | WAREHOUSE_MANAGER (A14b: also ADMIN, COORDINATOR, CUSTOMS_AGENT) |
| TC-A15 | `/app/index.jsp` | — | CUSTOMS_AGENT |
| TC-A16 | `/app/shipments/manage.jsp` | `GET /shipments`, `GET /shipments/{id}`, `PUT /shipments/{id}/status` | CUSTOMS_AGENT |
| TC-A17 | `/app/shipments/manage.jsp` | `POST /shipments/{id}/customs`, `PUT /shipments/{id}/customs/status` | CUSTOMS_AGENT |
| TC-A18 | `/app/shipments/manage.jsp` | `POST /shipments/{id}/notify-carrier` | CUSTOMS_AGENT |
| TC-A19 | various `/app/*` | — | CUSTOMS_AGENT |
| TC-A20 | `/app/index.jsp` | — | WORKER |
| TC-A21 | devtools console | any role-gated endpoint | any |
| TC-X01 | seller + staff pages above | `POST /suppliers/me/products`, `POST /purchase-orders`, `GET /purchase-orders/shippable`, `POST /purchase-orders/{id}/shipment`, `PUT /shipments/{id}/status`, `POST /shipments/{id}/customs`, `PUT /shipments/{id}/customs/status`, `POST /shipments/{id}/grn`, `GET /purchase-orders`, `GET /shipments/mine`, `GET /inventory/{id}` | VENDOR_REP, COORDINATOR, CUSTOMS_AGENT, WAREHOUSE_MANAGER |
| TC-X02 | `/index.jsp`, `/orders.jsp`, `/app/inventory.jsp`, `/app/index.jsp` | `POST /orders`, `GET /orders`, `GET /products`, `GET /inventory/{id}`, `GET /admin/sales-summary` | CUSTOMER, (ADMIN/COORDINATOR to verify) |
| TC-X03 | `/app/shipments/manage.jsp` | `GET /shipments`, `GET /shipments/{id}`, `PUT /shipments/{id}/status`, `POST /shipments/{id}/customs`, `PUT /shipments/{id}/customs/status`, `POST /shipments/{id}/notify-carrier` | CUSTOMS_AGENT |

**Endpoint not covered by any TC (intentionally):** `GET /orders/{orderId}` — no dedicated detail page exists because `orders.jsp`'s list already renders every order's full line-item detail inline; a drill-down page would show nothing new.

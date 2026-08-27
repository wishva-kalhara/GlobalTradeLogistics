# GlobalTrade Logistics — Procurement-to-Fulfillment E2E Test Flow

A single, continuous, **browser-driven** walkthrough of the full **create PO → ship → customs → GRN** pipeline, across four roles and two frontends (`frontend-app` staff console, `frontend-seller` portal). Every step below is a real click/type/submit in the UI — the only non-UI action anywhere in this flow is reading a one-time login code out of the server log, which is an unavoidable stand-in for email delivery in this dev deployment (see §2.4), not a shortcut around the UI.

This complements [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) (broader per-page checklist across all three frontends) by focusing tightly on the shipment lifecycle that plan predates.

---

## 1. What this proves

By the end of this flow you will have, entirely through the UI:
1. Had an ADMIN onboard a COORDINATOR, a WAREHOUSE_MANAGER, and a CUSTOMS_AGENT.
2. Had a supplier self-sign-up and complete their profile.
3. Had the COORDINATOR place a purchase order against that supplier.
4. Had the supplier create a shipment for that PO.
5. Confirmed the shipment is **not** GRN-eligible until it's `DELIVERED`.
6. Confirmed it's still **not** GRN-eligible once delivered until customs is `CLEARED`.
7. Had the customs agent clear it, then the warehouse manager successfully record the GRN.
8. Confirmed the resulting state everywhere it should show up: the PO marked complete, stock increased, and the shipment's status auto-advanced to `COMPLETED` — with the UI itself preventing the two ways that could otherwise be forced (a duplicate GRN, or manually setting `COMPLETED`).

---

## 2. Prerequisites

1. From the repo root: `docker compose up -d` (or `--build` if you've changed code since the image was last built).
2. Confirm it's healthy: `curl http://localhost:8080/api/v1/healthz` → `Up and running`.
3. Base URLs: staff console `http://localhost:8080/app/`, seller portal `http://localhost:8080/seller/`.
4. **Reading one-time login codes**: this stack runs with `IS_PROD=false`, so no real email is sent — every OTP is logged instead of delivered. After clicking "Send OTP" for an email, run:
   ```
   docker compose logs app | grep OTP_AUTHENTICATION
   ```
   and take the `code=NNNNNN` from the most recent line for that email. This is the one step in the whole flow that isn't a browser action — everything else is a click, a type, or reading what's rendered on the page.
5. Seeded ADMIN account: `admin@globaltradelogistics.local`.
6. Use distinct emails for the accounts you create below (e.g. suffix them with today's date or a run number) if you're re-running this flow against a database that already has data from a prior run.

---

## 3. The Flow

### Phase A — ADMIN onboards the staff this flow needs

1. Go to `/app/login.jsp` → email `admin@globaltradelogistics.local` → **Send OTP** → look up the code (§2.4) → enter it → **Verify**.
   - **TP-1**: redirected to `/app/index.jsp`, showing the ADMIN dashboard (cards: "Application User Management", "Vendor Performance Report", "Warehouse Inventory", "Shipments").
2. Open **Application User Management** → **Onboard User** → Email `e2e.coord@example.com`, Full name `E2E Coordinator`, Role `COORDINATOR` → **Onboard User**.
   - **TP-2**: success message "User ... onboarded"; the users table now includes this row with a `COORDINATOR` badge.
3. Repeat step 2 twice more: `e2e.wm@example.com` / `E2E Warehouse Manager` / role `WAREHOUSE_MANAGER`, and `e2e.customs@example.com` / `E2E Customs Agent` / role `CUSTOMS_AGENT`.
   - **TP-3**: all three staff accounts now appear in the table with the correct role badges.

### Phase B — Supplier self-signup and profile completion

4. Go to `/seller/auth/sign-up.jsp` → Email `e2e.supplier@example.com`, Country `United States` → **Create account**.
   - **TP-4**: auto-logged-in; since this profile has no `fullName` yet, you land on `/seller/me/update-profile.jsp` (not the dashboard) — this is exactly the "redirect only if profile is incomplete" behavior added earlier in this project.
5. On that page: Business/full name `E2E Supplier Co`, Mobile 1 `555-0000`, Address `1 E2E Way`, Country `United States` → **Save profile**.
   - **TP-5**: "Profile updated." success message.
6. Log out (nav → **Log out**), then log back in via `/seller/auth/login.jsp` with the same email (OTP flow, §2.4).
   - **TP-6**: this time you land on `/seller/index.jsp` (the dashboard) — because the profile is now complete. This confirms the profile-aware login redirect from both directions.

### Phase C — COORDINATOR creates the purchase order

7. Log in at `/app/login.jsp` as `e2e.coord@example.com` (OTP flow).
   - **TP-7**: dashboard shows COORDINATOR's cards ("Create Purchase Order", "Vendor Performance Report", "Warehouse Inventory", "Shipments").
8. Open **Create Purchase Order** (`/app/purchase-orders/create.jsp`).
   - **TP-8**: the **Supplier** dropdown lists `E2E Supplier Co` (not a bare email, and not any staff user) — confirming the supplier list only includes suppliers with a completed profile.
9. Select Supplier `E2E Supplier Co`, Product `Steel Pipe (3m)`, Quantity `7` → **Create Purchase Order**.
   - **TP-9**: result card shows a PO id, product "Steel Pipe (3m)", quantity 7, and a computed total. **Note the PO id** — call it `<PO_ID>`.

### Phase D — Supplier ships the PO

10. Log in at `/seller/auth/login.jsp` as `e2e.supplier@example.com` (OTP flow) — lands on the seller dashboard now (per TP-6).
11. Open **Create Shipment** (`/seller/shipments/create.jsp`).
    - **TP-10**: the **Purchase order** dropdown includes an entry for `<PO_ID>` (labeled "PO #`<PO_ID>` — 7 × Steel Pipe (3m)").
12. Select that PO, Tracking number `TRK-E2E`, Vessel ID `VESSEL-E2E`, Type `Sea` → **Create Shipment**.
    - **TP-11**: result card shows a shipment id (call it `<SHIPMENT_ID>`), the PO id, and status `CREATED`.
13. Reload **Create Shipment**.
    - **TP-12**: the purchase-order dropdown no longer offers `<PO_ID>` — a PO can only be shipped once.
14. Open **My Shipments** (`/seller/shipments/list.jsp`).
    - **TP-13**: a card for shipment `<SHIPMENT_ID>` is shown, status `CREATED`, customs status "not yet filed".

### Phase E — Confirm GRN is blocked before delivery

15. Log in at `/app/login.jsp` as `e2e.wm@example.com`.
16. Open **Record GRN** (`/app/purchase-orders/record-grn.jsp`).
    - **TP-14**: shipment `<SHIPMENT_ID>` is **not** in the "Delivered shipment" dropdown (it's either absent, or you see the "No delivered shipments are awaiting a GRN right now" empty state if it's the only shipment in the system) — the UI itself makes a premature GRN impossible, since there's nothing to select.
17. Open **Shipments** (`/app/shipments/list.jsp`).
    - **TP-15**: shipment `<SHIPMENT_ID>` is listed with status `CREATED` and customs `-` — confirming staff-wide visibility into shipments that haven't reached the warehouse manager's queue yet.

### Phase F — Customs handles the shipment

18. Log in at `/app/login.jsp` as `e2e.customs@example.com`.
19. Open **Manage Shipments** (`/app/shipments/manage.jsp`) → Shipment ID `<SHIPMENT_ID>` → **Load**.
    - **TP-16**: card shows tracking `TRK-E2E`, vessel `VESSEL-E2E`, type `SEA`, PO `#<PO_ID>`, customs status `-`.
20. Under "Update status": select `DELIVERED` → **Update**.
    - **TP-17**: status badge updates to `DELIVERED`; success message shown.

### Phase G — Confirm GRN is still blocked pre-customs

21. Log in as `e2e.wm@example.com` again → **Record GRN**.
    - **TP-18**: shipment `<SHIPMENT_ID>` **now appears** in the dropdown (it only requires `DELIVERED` + an open PO — customs isn't checked at this listing level).
22. Select it, Quantity received `7` → **Record GRN**.
    - **TP-19**: an error is shown — *"Could not record GRN: Customs clearance must be completed (CLEARED) before a GRN can be recorded for shipment `<SHIPMENT_ID>`."* Nothing changes: the PO stays open, stock is untouched, and the shipment stays selectable in the dropdown for a retry.

### Phase H — Customs clearance, step by step

23. Back as `e2e.customs@example.com` → **Manage Shipments** → load `<SHIPMENT_ID>` again.
24. Under "Record customs clearance": Declaration number `DECL-E2E` → **Create Record**.
    - **TP-20**: success message; the card's "Customs status" field now shows `PENDING`.
25. Switch to `e2e.wm@example.com` → **Record GRN** → select `<SHIPMENT_ID>` → Quantity `7` → **Record GRN**.
    - **TP-21**: same error as TP-19 — `PENDING` still isn't good enough, proving the gate checks the *value* of the customs status, not just its existence.
26. Back as `e2e.customs@example.com` → **Manage Shipments** → load `<SHIPMENT_ID>` → under "Update customs status": select `CLEARED` → **Update**.
    - **TP-22**: success message; "Customs status" field updates to `CLEARED`.

### Phase I — GRN succeeds

27. Switch to `e2e.wm@example.com` → **Record GRN**.
    - **TP-23**: `<SHIPMENT_ID>` is still in the dropdown (customs status isn't part of this list's filter — only delivery + open-PO are).
28. Select it, Quantity `7` → **Record GRN**.
    - **TP-24**: result card shows the PO marked "Completed", product "Steel Pipe (3m)".
29. Reload **Record GRN**.
    - **TP-25**: `<SHIPMENT_ID>` is **gone** from the dropdown — its PO is now complete, so it can never be selected for a second GRN. This is the UI-level guarantee that a duplicate GRN can't even be attempted, let alone succeed.

### Phase J — Confirm the results everywhere they should land

30. Open **Warehouse Inventory** (`/app/inventory.jsp`), warehouse `Warehouse 1 (US)`.
    - **TP-26**: Steel Pipe (3m)'s qty is 7 higher than it was before Phase I.
31. Open **Shipments** (`/app/shipments/list.jsp`) (as any of ADMIN/COORDINATOR/WAREHOUSE_MANAGER/CUSTOMS_AGENT).
    - **TP-27**: shipment `<SHIPMENT_ID>` shows status `COMPLETED` (green badge) and customs `CLEARED`.
32. As `e2e.customs@example.com` → **Manage Shipments** → load `<SHIPMENT_ID>`.
    - **TP-28**: the "Update status" dropdown offers only `CREATED` / `IN_TRANSIT` / `DELIVERED` / `DELAYED` — `COMPLETED` is **not a selectable option anywhere in the UI**, matching the backend rule that only a successful GRN can set it.
33. Log back in as `e2e.supplier@example.com` → **My Shipments**.
    - **TP-29**: the shipment card shows status `COMPLETED`, customs status `CLEARED` — the supplier can see their shipment's full journey to completion without needing any staff-console access.

---

## 4. Test Point Reference

| TP | Page | Confirms |
|---|---|---|
| 1 | `/app/login.jsp` → `/app/index.jsp` | ADMIN OTP login and role-based dashboard |
| 2–3 | `/app/app-user-management.jsp` | ADMIN can onboard COORDINATOR / WAREHOUSE_MANAGER / CUSTOMS_AGENT |
| 4 | `/seller/auth/sign-up.jsp` | Incomplete-profile supplier is sent to `update-profile.jsp`, not the dashboard |
| 5 | `/seller/me/update-profile.jsp` | Profile save succeeds |
| 6 | `/seller/auth/login.jsp` | Complete-profile supplier is sent straight to the dashboard |
| 7 | `/app/index.jsp` | COORDINATOR role-based dashboard |
| 8 | `/app/purchase-orders/create.jsp` | Supplier dropdown only lists suppliers with a completed profile |
| 9 | `/app/purchase-orders/create.jsp` | PO creation succeeds |
| 10–11 | `/seller/shipments/create.jsp` | Shippable-PO dropdown is correct; shipment creation succeeds |
| 12 | `/seller/shipments/create.jsp` | A shipped PO drops out of the shippable dropdown |
| 13 | `/seller/shipments/list.jsp` | Supplier sees their own shipment |
| 14 | `/app/purchase-orders/record-grn.jsp` | Non-delivered shipment is unselectable — GRN impossible before delivery |
| 15 | `/app/shipments/list.jsp` | Staff-wide shipment visibility, pre-delivery |
| 16–17 | `/app/shipments/manage.jsp` | Customs agent can load a shipment and advance it to `DELIVERED` |
| 18–19 | `/app/purchase-orders/record-grn.jsp` | Delivered-but-not-cleared shipment is selectable but GRN submission is rejected |
| 20 | `/app/shipments/manage.jsp` | Customs record creation (starts `PENDING`) |
| 21 | `/app/purchase-orders/record-grn.jsp` | `PENDING` customs still blocks the GRN |
| 22 | `/app/shipments/manage.jsp` | Customs status can be advanced to `CLEARED` |
| 23–24 | `/app/purchase-orders/record-grn.jsp` | GRN succeeds once `DELIVERED` + `CLEARED`; PO marked complete |
| 25 | `/app/purchase-orders/record-grn.jsp` | UI prevents any duplicate GRN attempt |
| 26 | `/app/inventory.jsp` | Stock increased by the GRN quantity |
| 27 | `/app/shipments/list.jsp` | Shipment auto-advanced to `COMPLETED` |
| 28 | `/app/shipments/manage.jsp` | `COMPLETED` is never a selectable status in the UI |
| 29 | `/seller/shipments/list.jsp` | Supplier sees the completed shipment |

---

## 5. How this was verified

No browser automation was available in the session that wrote this document, so the steps above were **not** clicked through in an actual browser. What *was* verified, immediately before writing this, is every underlying API call this flow makes — same endpoints, same payloads, same sequence, same expected status codes and response fields — run live against this stack with a passing result at every step (login, onboarding, signup, profile completion, PO creation, shipment creation, the duplicate-shipment/premature-GRN/premature-customs rejections, clearing customs, the GRN success, and the final `COMPLETED` state). Each JSP page listed above was also re-read to confirm it calls that exact endpoint with that exact request shape.

So: the business logic behind every step is proven correct. What hasn't been separately confirmed is the browser-rendering layer itself — that a given button is clickable, a dropdown visually populates, an alert div actually becomes visible, etc. If you hit a UI-only snag while walking this (a JS error, a class not toggling), the underlying data/logic is not the suspect; report it as a frontend rendering bug specifically, not a business-logic one.

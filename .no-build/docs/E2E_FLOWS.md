# GlobalTrade Logistics — End-to-End Flow Diagrams

Companion to [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) — one diagram per flow (or group of closely related flows) from that document, in the same order. Flowcharts show the user-facing decision path; the three cross-actor chains (§7) also get a sequence diagram showing which frontend/actor talks to which endpoint, in order.

---

## 1. `frontend-customer` Flows

### Guest landing → sign-up → profile completion (TC-C02, TC-C03)

```mermaid
flowchart TD
    A["Visit /index.jsp as guest - it IS the product catalog, no separate placeholder page"] --> B[Click Log in in nav]
    B --> C["auth/login.jsp: click New here? Create an account"]
    C --> D[auth/sign-up.jsp: enter email + country]
    D --> E[POST /api/v1/auth/signup/customer]
    E --> F{Email already registered?}
    F -- Yes --> G[409 error shown, stay on sign-up page]
    F -- No --> H[customers row inserted, full_name/mobile/address left NULL]
    H --> I[JWT issued - auto-login]
    I --> J[Session stored in localStorage]
    J --> K[Redirect to me/update-profile.jsp]
    K --> L[Fill full name, mobile 1/2, address, country]
    L --> M[PUT /api/v1/me/customer]
    M --> N[Profile saved]
```

### Returning customer OTP login (TC-C04)

```mermaid
flowchart TD
    A[Visit /auth/login.jsp] --> B[Enter email, Send OTP]
    B --> C[POST /api/v1/auth/otp/request]
    C --> D[Code logged server-side - IS_PROD=false]
    D --> E[Enter code, Verify]
    E --> F[POST /api/v1/auth/otp/verify]
    F --> G{Code valid, unexpired, unconsumed?}
    G -- No --> H[401 shown, stay on login page]
    G -- Yes --> I[JWT issued, session stored]
    I --> J["Redirect to /index.jsp - the product catalog (My Orders/Update Profile reachable via nav Account menu)"]
```

### Browse products (TC-C06)

```mermaid
flowchart TD
    A["Visit /index.jsp - logged in or guest, same page either way"] --> B[GET /api/v1/products]
    B --> C[Skeleton placeholders shown while loading]
    C --> D[Product grid rendered with stock badges]
    D --> E{User types in search box?}
    E -- Yes --> F[Client-side filter by name/description]
    E -- No --> G[Full grid shown]
    F --> H{Any matches?}
    H -- No --> I[Empty search-results state]
    H -- Yes --> D
```

### Place an order (TC-C07, TC-C08, TC-C09)

```mermaid
flowchart TD
    A[On /index.jsp, adjust quantity via stepper] --> B[Sticky cart bar shows live item count + total]
    B --> C[Click Place Order]
    C --> D{Signed in?}
    D -- No --> E[Redirect to auth/login.jsp]
    D -- Yes --> F[POST /api/v1/orders with items]
    F --> G{Sufficient stock for every item?}
    G -- No --> H[409 Insufficient inventory - error shown, stock unchanged]
    G -- Yes --> I[inventory decremented; orders + order_items inserted, status=PLACED]
    I --> J[Success message with order id + total]
    J --> K[Quantities reset to 0; product grid stock reflects the decrement on next load]
```

### Order history (TC-C10)

```mermaid
flowchart TD
    A[Visit /orders.jsp - must be signed in] --> B[GET /api/v1/orders]
    B --> C{Any orders?}
    C -- No --> D[Empty state with Browse products CTA]
    C -- Yes --> E[List rendered most-recent-first]
    E --> F[Each card: order id, status badge, item lines, total]
```

---

## 2. `frontend-seller` Flows

### Supplier sign-up → profile completion (TC-SE02, TC-SE03, TC-SE05)

```mermaid
flowchart TD
    A[Visit /seller/auth/sign-up.jsp] --> B[Enter email + country]
    B --> C[POST /api/v1/auth/signup/supplier]
    C --> D{Email already registered?}
    D -- Yes --> E[409 error shown]
    D -- No --> F[suppliers row inserted, full_name/mobile/address left NULL]
    F --> G[JWT issued - auto-login]
    G --> H[Redirect to seller/me/update-profile.jsp]
    H --> I[Fill business name, mobile 1/2, address, country]
    I --> J[PUT /api/v1/me/supplier]
    J --> K[Profile saved]
```

### Returning seller OTP login — profile-aware redirect (TC-SE04)

```mermaid
flowchart TD
    A[Visit /seller/auth/login.jsp] --> B[Request + verify OTP - same shape as customer]
    B --> C[JWT issued, session stored]
    C --> D[GET /api/v1/me/supplier]
    D --> E{fullName already set?}
    E -- No --> F[Redirect to seller/me/update-profile.jsp]
    E -- Yes --> G[Redirect to seller/index.jsp - the dashboard]
```

### Add a product offering (TC-SE06)

```mermaid
flowchart TD
    A[Signed in as VENDOR_REP] --> B[Account menu -> Add Product Offering]
    B --> C[GET /api/v1/products - populate product dropdown]
    B --> C2[GET /api/v1/inventory/warehouses - populate warehouse dropdown]
    C --> D[Select product, select warehouse, lead time in days]
    C2 --> D
    D --> E[POST /api/v1/suppliers/me/products]
    E --> F{"@RequiresRole VENDOR_REP check"}
    F -- Fails --> G[403]
    F -- Passes --> H[supplier_providing_products row inserted, supplierId resolved from JWT]
    H --> I[Success message, form reset]
```

### My Purchase Orders (TC-SE07, TC-SE08)

```mermaid
flowchart TD
    A[Signed in as VENDOR_REP] --> B[Account menu -> My Purchase Orders]
    B --> C[GET /api/v1/purchase-orders - scoped to caller's supplierId]
    C --> D{Any POs placed against this supplier?}
    D -- No --> E[Empty state: No purchase orders yet]
    D -- Yes --> F[List rendered: PO id, Open/Completed pill, qty x product, total]
```

### Create Shipment (TC-SE10)

```mermaid
flowchart TD
    A[Signed in as VENDOR_REP] --> B[Account menu -> Create Shipment]
    B --> C[GET /api/v1/purchase-orders/shippable - own open POs with no shipment yet]
    C --> D[Select PO, enter tracking number, vessel id, type]
    D --> E[POST /api/v1/purchase-orders/poId/shipment]
    E --> F{"PO belongs to caller, open, no existing shipment?"}
    F -- No, wrong owner --> G[404 - indistinguishable from nonexistent]
    F -- No, already completed/shipped --> H[409 InvalidShipmentStateException]
    F -- Yes --> I[shipments row inserted, status=CREATED, linked to poId]
    I --> J[Result card: shipment id, PO id, status]
    J --> K[PO no longer appears in shippable list on reload]
```

### My Shipments (TC-SE11)

```mermaid
flowchart TD
    A[Signed in as VENDOR_REP] --> B[Account menu -> My Shipments]
    B --> C[GET /api/v1/shipments/mine - shipments for own POs]
    C --> D{Any shipments?}
    D -- No --> E[Empty state]
    D -- Yes --> F[Cards: shipment id, PO id, tracking/vessel/type, status badge, customs status]
```

---

## 3. `frontend-app` (Staff Console) Flows

### Staff OTP login → role-based dashboard (TC-A01, TC-A02)

```mermaid
flowchart TD
    A[Visit /app/login.jsp] --> B[Request + verify OTP]
    B --> C[JWT issued, session stored]
    C --> D[Redirect to /app/index.jsp]
    D --> E{session.role}
    E -- ADMIN --> F["Cards: User Management, Inventory, Shipments + Analytics section"]
    E -- COORDINATOR --> G["Cards: Create PO, Inventory, Shipments + Analytics section"]
    E -- WAREHOUSE_MANAGER --> H["Cards: Record GRN, Inventory, Shipments"]
    E -- CUSTOMS_AGENT --> I["Cards: Manage Shipments, All Shipments"]
    E -- WORKER --> J["No console actions yet"]
```

*Note: "Vendor Performance Report" was removed as a dashboard **card** for ADMIN/COORDINATOR but remains reachable from the nav's Account menu — an intentional asymmetry, not a bug.*

### ADMIN: onboard a staff user (TC-A04)

```mermaid
flowchart TD
    A[ADMIN on app-user-management.jsp] --> B[Click Onboard User]
    B --> C[Modal: email, full name, role dropdown]
    C --> D[POST /api/v1/admin/users]
    D --> E{"@RequiresRole ADMIN check"}
    E -- Fails --> F[403 shown in modal]
    E -- Passes --> G[users row inserted]
    G --> H[WORKER_ONBOARDING email queued]
    H --> I[Modal closes; GET /api/v1/admin/users refreshes table]
    I --> J[New user visible; can log in at /app/login.jsp via OTP]
```

### ADMIN: register a customer or supplier directly (TC-A05, TC-A06)

```mermaid
flowchart TD
    A[ADMIN on app-user-management.jsp] --> B[Click Register Customer or Register Supplier]
    B --> C[Modal: email, name, mobile, address, country]
    C --> D{Which?}
    D -- Customer --> E[POST /api/v1/admin/customers]
    D -- Supplier --> F[POST /api/v1/admin/suppliers]
    E --> G{"@RequiresRole ADMIN check"}
    F --> G
    G -- Fails --> H[403]
    G -- Passes --> I[customers/suppliers row inserted]
    I --> J[CUSTOMER_ONBOARDING / SUPPLIER_ONBOARDING email queued]
    J --> K[Success message; modal closes]
    K --> L[That person can now self-service OTP-login on the customer/seller frontend]
```

### Vendor Performance Report (TC-A08)

```mermaid
flowchart TD
    A[ADMIN or COORDINATOR opens vendor-performance.jsp] --> B[GET /api/v1/admin/vendor-performance]
    B --> C{"@RequiresRole ADMIN/COORDINATOR check"}
    C -- Fails --> D[403]
    C -- Passes --> E[Reads audit_records WHERE type = VENDOR_PERFORMANCE]
    E --> F{Any rows?}
    F -- No --> G["Empty state (expected under IS_PROD=false - not a bug)"]
    F -- Yes --> H[Table: supplier id, recorded at, summary]
```

### Warehouse Inventory (TC-A09)

```mermaid
flowchart TD
    A["ADMIN / COORDINATOR / WAREHOUSE_MANAGER opens inventory.jsp"] --> B[GET /api/v1/inventory/warehouses - populate dropdown]
    B --> C[Auto-load first/selected warehouse: GET /api/v1/inventory/warehouseId]
    C --> D{"@RequiresRole check"}
    D -- Fails --> E[403]
    D -- Passes --> F{Any rows for that warehouse?}
    F -- No --> G[Empty state]
    F -- Yes --> H[Table: product, qty, reorder level, unit price, last updated]
    H --> I{qty < reorderLevel?}
    I -- Yes --> J[Row highlighted amber, qty marked low]
    I -- No --> K[Row rendered normally]
```

### COORDINATOR: create a purchase order, scoped supplier dropdown (TC-A11)

```mermaid
flowchart TD
    A[COORDINATOR sees a product running low] --> B[Opens purchase-orders/create.jsp]
    B --> C[GET /api/v1/products - populate product dropdown]
    C --> D[Select a product]
    D --> E[GET /api/v1/admin/suppliers?productId=selected - supplier dropdown enables]
    E --> F{Any suppliers offer this product?}
    F -- No --> G["Supplier dropdown shows 'No suppliers for this product'"]
    F -- Yes --> H[Select supplier, enter quantity]
    H --> I[POST /api/v1/purchase-orders]
    I --> J{"@RequiresRole COORDINATOR check"}
    J -- Fails --> K[403]
    J -- Passes --> L[purchase_orders row inserted, is_completed=0, total_price = qty x Inventory.unitPrice]
    L --> M[Result card: PO id, product, qty, total]
    M --> N[Supplier ships it next - see Create Shipment flow]
```

### Ship -> Customs -> GRN pipeline (TC-A13, TC-A16, TC-A17)

```mermaid
flowchart TD
    A[VENDOR_REP creates a shipment for the PO - status CREATED] --> B[CUSTOMS_AGENT loads it in manage.jsp]
    B --> C{Current status?}
    C -- CREATED --> D["Status dropdown offers ONLY IN_TRANSIT"]
    D --> E[PUT /status to IN_TRANSIT]
    E --> F[Customs section unlocks: declaration number input + Create Record button]
    F --> G[POST /customs - declarationNumber, status starts PENDING]
    G --> H["Declaration input now shows the value and is DISABLED (locked in)"]
    H --> I[Update customs status dropdown -> CLEARED]
    I --> J[PUT /customs/status]
    J --> K["Customs-status select + Update button now DISABLED (locked in)"]
    K --> L[Notify Carrier button becomes enabled]
    F --> M[PUT /status to DELIVERED]
    M --> N[WAREHOUSE_MANAGER opens record-grn.jsp]
    N --> O[GET /shipments/awaiting-grn - status=DELIVERED, PO not completed]
    O --> P{Shipment appears in dropdown?}
    P -- Yes, but customs not CLEARED yet --> Q[POST /grn -> 409 Customs clearance must be CLEARED]
    P -- Yes, and CLEARED --> R[POST /grn succeeds]
    R --> S[grns row inserted; inventory.qty incremented; purchase_orders.is_completed=1]
    S --> T["shipment.status set to COMPLETED - the ONLY way this status is ever set"]
    T --> U[Shipment drops out of awaiting-grn - duplicate GRN impossible from the UI]
```

### CUSTOMS_AGENT: manage a shipment, dropdown + state-gated controls (TC-A16, TC-A17, TC-A18)

```mermaid
flowchart TD
    A[CUSTOMS_AGENT opens shipments/manage.jsp] --> B[GET /api/v1/shipments - populate Shipment dropdown]
    B --> C[Select a shipment - auto-loads, no Load button]
    C --> D[GET /api/v1/shipments/shipmentId]
    D --> E[Shipment card renders: tracking, vessel, type, warehouse, PO id, customs status, carrier ref]
    E --> F{Current status?}
    F -- CREATED --> G["Status dropdown offers ONLY IN_TRANSIT"]
    F -- other --> H["Status dropdown offers IN_TRANSIT/DELIVERED/DELAYED"]
    E --> I{"Customs controls enabled only while status == IN_TRANSIT"}
    I -- No --> J[Declaration input, Create Record, customs-status select, Update all DISABLED]
    I -- Yes --> K{"declarationNumber already set?"}
    K -- Yes --> L[Declaration input + Create Record DISABLED - locked in]
    K -- No --> M[Declaration input + Create Record enabled]
    I -- Yes --> N{"customsStatus == CLEARED?"}
    N -- Yes --> O[Customs-status select + Update DISABLED - locked in]
    N -- No --> P[Customs-status select + Update enabled]
    E --> Q{"customsStatus == CLEARED?"}
    Q -- Yes --> R[Notify Carrier enabled]
    Q -- No --> S[Notify Carrier disabled]
    G --> T[PUT /status - dropdown label for this shipment refreshes too]
    M --> U[POST /customs]
    P --> V[PUT /customs/status]
    R --> W["POST /notify-carrier (BMT: read tx, simulated external call with no tx open, write tx)"]
    W --> X[Card refreshes with a new CARRIER-uuid reference]
```

### Role-based page access (CC-5, TC-A12/14/19)

```mermaid
flowchart TD
    A[Signed-in staff user navigates directly to a role-gated page URL] --> B{session.role matches the page's required role?}
    B -- Yes --> C[Page loads normally]
    B -- No --> D[Client-side redirect to /app/access-denied.jsp]
    D --> E["Note: this is UX only - the real gate is the endpoint's @RequiresRole, verified separately (TC-A21)"]
```

---

## 4. Cross-Actor Integration Chains (§7 of the test plan)

### TC-X01 — Full procurement chain (create PO → ship → customs → GRN)

See [`E2E_TEST_FLOW.md`](./E2E_TEST_FLOW.md) for the fully verified, field-by-field version of this chain (29 test points). Summarized here:

```mermaid
flowchart TD
    A[VENDOR_REP adds a product offering] --> B[COORDINATOR creates a PO against that supplier/product]
    B --> C[VENDOR_REP creates a shipment for that PO]
    C --> D[CUSTOMS_AGENT: CREATED -> IN_TRANSIT, customs record CLEARED, then -> DELIVERED]
    D --> E[WAREHOUSE_MANAGER records a GRN for that shipment]
    E --> F[grns row inserted; inventory.qty incremented; purchase_orders.is_completed=1; shipment.status=COMPLETED]
    F --> G[VENDOR_REP reloads My Purchase Orders - sees Completed; My Shipments - sees COMPLETED]
    F --> H[ADMIN/COORDINATOR reloads Warehouse Inventory - sees increased qty]
```

```mermaid
sequenceDiagram
    participant Seller as Browser (frontend-seller, VENDOR_REP)
    participant Coord as Browser (frontend-app, COORDINATOR)
    participant Customs as Browser (frontend-app, CUSTOMS_AGENT)
    participant WM as Browser (frontend-app, WAREHOUSE_MANAGER)
    participant GW as api-gateway
    participant DB as MySQL

    Seller->>GW: POST /v1/suppliers/me/products {productId, warehouseId, leadTimeInDays}
    GW->>DB: INSERT supplier_providing_products

    Coord->>GW: GET /v1/admin/suppliers?productId={id}
    GW-->>Coord: [suppliers offering this product]
    Coord->>GW: POST /v1/purchase-orders {supplierId, productId, qty}
    GW->>DB: INSERT purchase_orders (is_completed=0)
    GW-->>Coord: PurchaseOrderSummary (poId)

    Seller->>GW: GET /v1/purchase-orders/shippable
    GW-->>Seller: [PO appears - open, no shipment yet]
    Seller->>GW: POST /v1/purchase-orders/{poId}/shipment {trackingNumber, vesselId, type}
    GW->>DB: INSERT shipments (status=CREATED, purchase_orders_po_id={poId})
    GW-->>Seller: ShipmentSummary (shipmentId)

    Customs->>GW: PUT /v1/shipments/{id}/status {status: IN_TRANSIT, idempotencyKey}
    GW->>DB: UPDATE shipments SET status = 'IN_TRANSIT'
    Customs->>GW: POST /v1/shipments/{id}/customs {declarationNumber}
    GW->>DB: INSERT custom_clearence_records (status=PENDING)
    Customs->>GW: PUT /v1/shipments/{id}/customs/status {status: CLEARED}
    GW->>DB: UPDATE custom_clearence_records SET status = 'CLEARED'
    Customs->>GW: PUT /v1/shipments/{id}/status {status: DELIVERED, idempotencyKey}
    GW->>DB: UPDATE shipments SET status = 'DELIVERED'

    WM->>GW: GET /v1/shipments/awaiting-grn
    GW-->>WM: [shipment appears - DELIVERED, PO open]
    WM->>GW: POST /v1/shipments/{id}/grn {qty}
    GW->>DB: Check latest customs record is CLEARED, else 409
    GW->>DB: INSERT grns
    GW->>DB: UPDATE inventory SET qty = qty + ?
    GW->>DB: UPDATE purchase_orders SET is_completed = 1
    GW->>DB: UPDATE shipments SET status = 'COMPLETED'
    GW-->>WM: PurchaseOrderSummary (completed=true)

    Seller->>GW: GET /v1/purchase-orders
    GW-->>Seller: [PO shows completed=true]
    Seller->>GW: GET /v1/shipments/mine
    GW-->>Seller: [shipment shows COMPLETED, customsStatus=CLEARED]
```

### TC-X02 — Full customer order chain

```mermaid
flowchart TD
    A[Customer notes a product's current stock] --> B[Places an order for a few units]
    B --> C[inventory.qty decremented; orders + order_items inserted]
    C --> D[Reload index.jsp - stock reflects the decrease]
    C --> E[Check orders.jsp - new order shows status PLACED]
    C --> F[ADMIN/COORDINATOR checks Warehouse Inventory - same qty decrease visible there too]
```

```mermaid
sequenceDiagram
    participant Cust as Browser (frontend-customer)
    participant Staff as Browser (frontend-app, ADMIN/COORDINATOR)
    participant GW as api-gateway
    participant DB as MySQL

    Cust->>GW: GET /v1/products
    GW->>DB: SELECT products + best inventory row
    GW-->>Cust: [stock levels shown]

    Cust->>GW: POST /v1/orders {items:[{productId, qty}]}
    GW->>DB: UPDATE inventory SET qty = qty - ?
    GW->>DB: INSERT orders (status=PLACED), INSERT order_items
    GW-->>Cust: OrderSummary

    Cust->>GW: GET /v1/orders
    GW-->>Cust: [new order listed]

    Staff->>GW: GET /v1/inventory/{warehouseId}
    GW->>DB: SELECT inventory WHERE warehouse = ?
    GW-->>Staff: [same decreased qty]
```

### TC-X03 — Shipment lifecycle and state-machine gating

```mermaid
flowchart TD
    A[CUSTOMS_AGENT loads shipment 1 via dropdown - initially IN_TRANSIT, no PO link] --> B[Status dropdown offers IN_TRANSIT/DELIVERED/DELAYED]
    B --> C[Updates status to DELAYED, then back to IN_TRANSIT]
    C --> D[Customs section enabled - creates a customs clearance record]
    D --> E["Declaration input now shows the value and is DISABLED"]
    E --> F[Advances customs status to CLEARED]
    F --> G["Customs-status select + Update DISABLED; Notify Carrier now enabled"]
    G --> H[Notifies the carrier system - gets a CARRIER-uuid ref]
    H --> I[Reload shipment - status, customs status, declaration number, ref all persisted]
    I --> J["Negative check: a freshly-created shipment shows status dropdown offering ONLY IN_TRANSIT, customs section fully disabled"]
    J --> K["Background: a 15-min declarative timer independently polls IN_TRANSIT shipments and may flip status to DELIVERED with no user action"]
```

```mermaid
sequenceDiagram
    participant Agent as Browser (frontend-app, CUSTOMS_AGENT)
    participant GW as api-gateway
    participant LS as logistics-svc
    participant DB as MySQL

    Agent->>GW: GET /v1/shipments
    GW-->>Agent: [shipment dropdown populated]

    Agent->>GW: GET /v1/shipments/1
    GW->>LS: getShipment(1)
    LS->>DB: SELECT shipments WHERE shipment_id = 1
    GW-->>Agent: ShipmentSummary (IN_TRANSIT, customsStatus=null)

    Agent->>GW: PUT /v1/shipments/1/status {status: DELAYED, idempotencyKey}
    GW->>LS: updateStatus(1, DELAYED, key)
    Note over LS: Rejects status=COMPLETED with 409 - not exercised here, only settable via a GRN
    LS->>DB: UPDATE shipments SET status = 'DELAYED'
    GW-->>Agent: ShipmentSummary (DELAYED)

    Agent->>GW: POST /v1/shipments/1/customs {declarationNumber}
    GW->>LS: createCustomsRecord(1, declarationNumber)
    LS->>DB: INSERT custom_clearence_records (status=PENDING)
    GW-->>Agent: 201 Created

    Agent->>GW: PUT /v1/shipments/1/customs/status {status: CLEARED}
    GW->>LS: updateCustomsStatus(1, CLEARED)
    LS->>DB: UPDATE custom_clearence_records SET status = 'CLEARED'
    GW-->>Agent: ShipmentSummary (customsStatus=CLEARED)

    Agent->>GW: POST /v1/shipments/1/notify-carrier
    GW->>LS: notifyCarrierSystem(1)
    Note over LS: BMT bean - read tx, simulated external call<br/>with no tx open, then write tx
    LS->>DB: UPDATE shipments SET ref = 'CARRIER-<uuid>'
    GW-->>Agent: ShipmentSummary (ref populated)
```

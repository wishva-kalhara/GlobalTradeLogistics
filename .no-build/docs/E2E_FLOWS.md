# GlobalTrade Logistics — End-to-End Flow Diagrams

Companion to [`E2E_TEST_PLAN.md`](./E2E_TEST_PLAN.md) — one diagram per flow (or group of closely related flows) from that document, in the same order. Flowcharts show the user-facing decision path; the three cross-actor chains (§7) also get a sequence diagram showing which frontend/actor talks to which endpoint, in order.

---

## 1. `frontend-customer` Flows

### Guest landing → sign-up → profile completion (TC-C02, TC-C03)

```mermaid
flowchart TD
    A[Visit /index.jsp as guest] --> B[Click Create an account]
    B --> C[auth/sign-up.jsp: enter email + country]
    C --> D[POST /api/v1/auth/signup/customer]
    D --> E{Email already registered?}
    E -- Yes --> F[409 error shown, stay on sign-up page]
    E -- No --> G[customers row inserted, full_name/mobile/address left NULL]
    G --> H[JWT issued - auto-login]
    H --> I[Session stored in localStorage]
    I --> J[Redirect to me/update-profile.jsp]
    J --> K[Fill full name, mobile 1/2, address, country]
    K --> L[PUT /api/v1/me/customer]
    L --> M[Profile saved]
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
    I --> J[Redirect to /index.jsp]
    J --> K[Dashboard renders: Browse Products / My Orders / Update Profile]
```

### Browse products (TC-C06)

```mermaid
flowchart TD
    A[Visit /products.jsp - logged in or guest] --> B[GET /api/v1/products]
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
    A[On /products.jsp, adjust quantity via stepper] --> B[Sticky cart bar shows live item count + total]
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

### Returning seller OTP login (TC-SE04)

```mermaid
flowchart TD
    A[Visit /seller/auth/login.jsp] --> B[Request + verify OTP - same shape as customer]
    B --> C[JWT issued, session stored]
    C --> D["Redirect to seller/me/update-profile.jsp (NOT a dashboard - known gap, see plan)"]
```

### Add a product offering (TC-SE06)

```mermaid
flowchart TD
    A[Signed in as VENDOR_REP] --> B[Account menu -> Add Product Offering]
    B --> C[GET /api/v1/products - populate dropdown]
    C --> D[Select product, warehouse id, lead time in days]
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

---

## 3. `frontend-app` (Staff Console) Flows

### Staff OTP login → role-based dashboard (TC-A01, TC-A02)

```mermaid
flowchart TD
    A[Visit /app/login.jsp] --> B[Request + verify OTP]
    B --> C[JWT issued, session stored]
    C --> D[Redirect to /app/index.jsp]
    D --> E{session.role}
    E -- ADMIN --> F["Cards: User Management, Vendor Performance, Inventory"]
    E -- COORDINATOR --> G["Cards: Create PO, Vendor Performance, Inventory"]
    E -- WAREHOUSE_MANAGER --> H["Cards: Record GRN, Inventory"]
    E -- CUSTOMS_AGENT --> I["Card: Manage Shipments"]
    E -- WORKER --> J["No console actions yet"]
```

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
    A["ADMIN / COORDINATOR / WAREHOUSE_MANAGER opens inventory.jsp"] --> B[Enter warehouse id, default 1]
    B --> C[GET /api/v1/inventory/warehouseId]
    C --> D{"@RequiresRole check"}
    D -- Fails --> E[403]
    D -- Passes --> F{Any rows for that warehouse?}
    F -- No --> G[Empty state]
    F -- Yes --> H[Table: product, qty, reorder level, unit price, last updated]
    H --> I{qty < reorderLevel?}
    I -- Yes --> J[Row highlighted amber, qty marked low]
    I -- No --> K[Row rendered normally]
```

### COORDINATOR: create a purchase order (TC-A11)

```mermaid
flowchart TD
    A[COORDINATOR sees a product running low] --> B[Opens purchase-orders/create.jsp]
    B --> C[GET /api/v1/products - populate dropdown]
    C --> D[Enter supplier id, select product, quantity]
    D --> E[POST /api/v1/purchase-orders]
    E --> F{"@RequiresRole COORDINATOR check"}
    F -- Fails --> G[403]
    F -- Passes --> H[purchase_orders row inserted, is_completed=0, total_price = qty x Inventory.unitPrice]
    H --> I[Result card: PO id, product, qty, total]
    I --> J[PO id handed to a warehouse manager for GRN]
```

### WAREHOUSE_MANAGER: record a GRN (TC-A13)

```mermaid
flowchart TD
    A[Goods physically arrive at warehouse] --> B[WAREHOUSE_MANAGER opens purchase-orders/record-grn.jsp]
    B --> C[Enter PO id, quantity received]
    C --> D[POST /api/v1/purchase-orders/poId/grn]
    D --> E{"@RequiresRole WAREHOUSE_MANAGER check"}
    E -- Fails --> F[403]
    E -- Passes --> G[grns row inserted]
    G --> H[inventory.qty incremented for that product]
    H --> I[purchase_orders.is_completed set to 1]
    I --> J[Result card shows PO marked Completed]
```

### CUSTOMS_AGENT: manage a shipment (TC-A16, TC-A17, TC-A18)

```mermaid
flowchart TD
    A[CUSTOMS_AGENT opens shipments/manage.jsp] --> B[Enter shipment id, Load]
    B --> C[GET /api/v1/shipments/shipmentId]
    C --> D{Found?}
    D -- No --> E[404 error shown]
    D -- Yes --> F[Shipment card renders: tracking, vessel, type, warehouse, status, carrier ref]
    F --> G[Pick new status, click Update]
    G --> H[PUT /api/v1/shipments/shipmentId/status with a fresh idempotencyKey]
    H --> I[shipments.status updated; card refreshes]
    F --> J[Enter declaration number, Create Record]
    J --> K[POST /api/v1/shipments/shipmentId/customs]
    K --> L[custom_clearence_records row created]
    F --> M[Click Notify Carrier]
    M --> N["POST /api/v1/shipments/shipmentId/notify-carrier (BMT: read tx, simulated external call with no tx open, write tx)"]
    N --> O[Card refreshes with a new CARRIER-uuid reference]
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

### TC-X01 — Full procurement chain

```mermaid
flowchart TD
    A[VENDOR_REP adds a product offering] --> B[COORDINATOR creates a PO against that supplier/product]
    B --> C[WAREHOUSE_MANAGER records a GRN for that PO]
    C --> D[inventory.qty incremented; purchase_orders.is_completed = 1]
    D --> E[VENDOR_REP reloads My Purchase Orders - sees Completed]
    D --> F[ADMIN/COORDINATOR reloads Warehouse Inventory - sees increased qty]
```

```mermaid
sequenceDiagram
    participant Seller as Browser (frontend-seller)
    participant Staff1 as Browser (frontend-app, COORDINATOR)
    participant Staff2 as Browser (frontend-app, WAREHOUSE_MANAGER)
    participant GW as api-gateway
    participant DB as Postgres

    Seller->>GW: POST /v1/suppliers/me/products {productId, warehouseId, leadTimeInDays}
    GW->>DB: INSERT supplier_providing_products

    Staff1->>GW: POST /v1/purchase-orders {supplierId, productId, qty}
    GW->>DB: INSERT purchase_orders (is_completed=0)
    GW-->>Staff1: PurchaseOrderSummary (poId)

    Staff2->>GW: POST /v1/purchase-orders/{poId}/grn {qty}
    GW->>DB: INSERT grns
    GW->>DB: UPDATE inventory SET qty = qty + ?
    GW->>DB: UPDATE purchase_orders SET is_completed = 1
    GW-->>Staff2: PurchaseOrderSummary (completed=true)

    Seller->>GW: GET /v1/purchase-orders
    GW->>DB: SELECT ... WHERE suppliers_supplier_id = ?
    GW-->>Seller: [PO shows completed=true]
```

### TC-X02 — Full customer order chain

```mermaid
flowchart TD
    A[Customer notes a product's current stock] --> B[Places an order for a few units]
    B --> C[inventory.qty decremented; orders + order_items inserted]
    C --> D[Reload products.jsp - stock reflects the decrease]
    C --> E[Check orders.jsp - new order shows status PLACED]
    C --> F[ADMIN/COORDINATOR checks Warehouse Inventory - same qty decrease visible there too]
```

```mermaid
sequenceDiagram
    participant Cust as Browser (frontend-customer)
    participant Staff as Browser (frontend-app, ADMIN/COORDINATOR)
    participant GW as api-gateway
    participant DB as Postgres

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

### TC-X03 — Shipment lifecycle

```mermaid
flowchart TD
    A[CUSTOMS_AGENT loads shipment 1 - initially IN_TRANSIT] --> B[Updates status to DELAYED]
    B --> C[Creates a customs clearance record]
    C --> D[Notifies the carrier system - gets a CARRIER-uuid ref]
    D --> E[Reload shipment - status and ref both persisted]
    E --> F["Background: a 15-min declarative timer independently polls IN_TRANSIT shipments and may flip status to DELIVERED with no user action"]
```

```mermaid
sequenceDiagram
    participant Agent as Browser (frontend-app, CUSTOMS_AGENT)
    participant GW as api-gateway
    participant LS as logistics-svc
    participant DB as Postgres

    Agent->>GW: GET /v1/shipments/1
    GW->>LS: getShipment(1)
    LS->>DB: SELECT shipments WHERE shipment_id = 1
    GW-->>Agent: ShipmentSummary (IN_TRANSIT)

    Agent->>GW: PUT /v1/shipments/1/status {status: DELAYED, idempotencyKey}
    GW->>LS: updateStatus(1, DELAYED, key)
    LS->>DB: UPDATE shipments SET status = 'DELAYED'
    GW-->>Agent: ShipmentSummary (DELAYED)

    Agent->>GW: POST /v1/shipments/1/customs {declarationNumber}
    GW->>LS: createCustomsRecord(1, declarationNumber)
    LS->>DB: INSERT custom_clearence_records
    GW-->>Agent: 201 Created

    Agent->>GW: POST /v1/shipments/1/notify-carrier
    GW->>LS: notifyCarrierSystem(1)
    Note over LS: BMT bean - read tx, simulated external call<br/>with no tx open, then write tx
    LS->>DB: UPDATE shipments SET ref = 'CARRIER-<uuid>'
    GW-->>Agent: ShipmentSummary (ref populated)
```

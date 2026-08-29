-- GlobalTradeLogistics — manual test / demo dataset
--
-- Replaces the deploy-time seed beans (AdminSeedBean, CountrySeedBean,
-- CatalogSeedBean, ShipmentSeedBean). Load AFTER schema.sql:
--
--   mysql -h localhost -u gtl_app -pgtl_app global_trade_log_corp < .no-build/db/schema.sql
--   mysql -h localhost -u gtl_app -pgtl_app global_trade_log_corp < .no-build/db/test-data.sql
--
-- Safe to re-run: truncates application data first (legacy + Hibernate tables).
-- Does NOT drop tables. Run on a fresh schema.sql load, or anytime you want
-- to reset demo data without rebuilding the database from scratch.
--
-- OTP login still works normally — codes are generated at runtime, not seeded here.
-- Known test accounts (request OTP via the UI, then grep server logs):
--   Staff : admin@globaltradelogistics.local (ADMIN)
--           e2e.coord@example.com (COORDINATOR), e2e.wm@example.com (WAREHOUSE_MANAGER)
--           e2e.customs@example.com (CUSTOMS_AGENT), worker@example.com (WORKER)
--   Seller: e2e.supplier@example.com, pacific@example.com, euro@example.com
--   Buyer : alice@example.com, bob@example.com, carol@example.com

USE global_trade_log_corp;

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Hibernate-owned tables (created by hbm2ddl on deploy if missing — included
-- here so this script works before the first app deploy)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
  user_id   INT          NOT NULL AUTO_INCREMENT,
  email     VARCHAR(255) NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role      VARCHAR(255) NOT NULL,
  is_active BIT(1)       NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS countries (
  code VARCHAR(2)  NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS otp_codes (
  id         INT          NOT NULL AUTO_INCREMENT,
  email      VARCHAR(255) NOT NULL,
  code_hash  VARCHAR(255) NOT NULL,
  purpose    VARCHAR(255) NOT NULL,
  expires_at DATETIME(6)  NOT NULL,
  consumed   BIT(1)       NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Additive columns Hibernate puts on legacy tables (ignore error if already present)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'global_trade_log_corp' AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'status');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE orders ADD COLUMN status VARCHAR(45) NOT NULL DEFAULT ''PLACED''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'global_trade_log_corp' AND TABLE_NAME = 'shipments' AND COLUMN_NAME = 'purchase_orders_po_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE shipments ADD COLUMN purchase_orders_po_id INT NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- Reset (child → parent order)
-- ---------------------------------------------------------------------------

TRUNCATE TABLE custom_clearence_records;
TRUNCATE TABLE grns;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE supplier_providing_products;
TRUNCATE TABLE shipments;
TRUNCATE TABLE purchase_orders;
TRUNCATE TABLE inventory;
TRUNCATE TABLE products;
TRUNCATE TABLE wearhouses;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE customers;
TRUNCATE TABLE audit_records;
TRUNCATE TABLE otp_codes;
TRUNCATE TABLE users;
TRUNCATE TABLE countries;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- Reference: countries (45)
-- ---------------------------------------------------------------------------

INSERT INTO countries (code, name) VALUES
('AU','Australia'),('BD','Bangladesh'),('BE','Belgium'),('BR','Brazil'),
('CA','Canada'),('CH','Switzerland'),('CN','China'),('DE','Germany'),
('DK','Denmark'),('EG','Egypt'),('ES','Spain'),('FI','Finland'),
('FR','France'),('GB','United Kingdom'),('HK','Hong Kong'),('ID','Indonesia'),
('IE','Ireland'),('IN','India'),('IT','Italy'),('JP','Japan'),
('KE','Kenya'),('KR','South Korea'),('LK','Sri Lanka'),('MV','Maldives'),
('MX','Mexico'),('MY','Malaysia'),('NG','Nigeria'),('NL','Netherlands'),
('NO','Norway'),('NZ','New Zealand'),('PH','Philippines'),('PK','Pakistan'),
('PL','Poland'),('PT','Portugal'),('QA','Qatar'),('RU','Russia'),
('SA','Saudi Arabia'),('SE','Sweden'),('SG','Singapore'),('TH','Thailand'),
('TR','Turkey'),('TW','Taiwan'),('AE','United Arab Emirates'),('US','United States'),
('VN','Vietnam'),('ZA','South Africa');

-- ---------------------------------------------------------------------------
-- Staff users (OTP login — no password column)
-- ---------------------------------------------------------------------------

INSERT INTO users (email, full_name, role, is_active) VALUES
('admin@globaltradelogistics.local', 'System Administrator', 'ADMIN', 1),
('e2e.coord@example.com',            'E2E Coordinator',      'COORDINATOR', 1),
('e2e.wm@example.com',               'E2E Warehouse Manager','WAREHOUSE_MANAGER', 1),
('e2e.customs@example.com',        'E2E Customs Agent',    'CUSTOMS_AGENT', 1),
('worker@example.com',               'Demo Worker',          'WORKER', 1),
('coord2@example.com',               'Second Coordinator',   'COORDINATOR', 1);

-- ---------------------------------------------------------------------------
-- Customers
-- ---------------------------------------------------------------------------

INSERT INTO customers (user_id, email, is_active, full_name, mobile_1, mobile_2, address, country) VALUES
(1, 'alice@example.com', 'true', 'Alice Customer',  '555-0101', NULL,     '10 Market St',   'United States'),
(2, 'bob@example.com',   'true', 'Bob Buyer',       '555-0202', '555-0203','22 High Road',   'United Kingdom'),
(3, 'carol@example.com', 'true', 'Carol Chen',      '555-0303', NULL,     '88 Orchard Rd',  'Singapore'),
(4, 'dave@example.com',  'true', NULL,              NULL,       NULL,     NULL,             'United States');

-- ---------------------------------------------------------------------------
-- Suppliers (id 4 = incomplete profile — excluded from coordinator dropdown)
-- ---------------------------------------------------------------------------

INSERT INTO suppliers (supplier_id, email, is_active, full_name, mobile_1, mobile_2, address, country) VALUES
(1, 'e2e.supplier@example.com', 'true', 'E2E Supplier Co',      '555-1000', NULL, '1 E2E Way',        'United States'),
(2, 'pacific@example.com',      'true', 'Pacific Parts Ltd',    '555-2000', NULL, '12 Harbour Rd',    'Singapore'),
(3, 'euro@example.com',         'true', 'Euro Industrial GmbH', '555-3000', NULL, 'Industriestr 5',   'Germany'),
(4, 'newvendor@example.com',    'true', NULL,                   NULL,       NULL, NULL,               'United States');

-- ---------------------------------------------------------------------------
-- Warehouses (wearhous_id is NOT auto-increment in legacy schema)
-- ---------------------------------------------------------------------------

INSERT INTO wearhouses (wearhous_id, country) VALUES
(1, 'US'),
(2, 'SG'),
(3, 'DE');

-- ---------------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------------

INSERT INTO products (product_id, name, description, product_image) VALUES
(1,  'Steel Pipe (3m)',        'Galvanized steel pipe, 3m',      'steel-pipe.png'),
(2,  'Industrial Bearing',     'High-load ball bearing',         'bearing.png'),
(3,  'Hydraulic Hose (5m)',    'Reinforced hose, 5m',            'hydraulic-hose.png'),
(4,  'Circuit Breaker 32A',    '32A single-pole breaker',        'circuit-breaker.png'),
(5,  'Pallet Wrap Roll',       'Stretch wrap 500mm',             'pallet-wrap.png'),
(6,  'Copper Wire Spool',      '2.5mm copper wire 100m',         'copper-wire.png'),
(7,  'Safety Valve 2in',       'Brass safety valve 2 inch',      'safety-valve.png'),
(8,  'LED Panel 60W',          '600x600 LED panel light',        'led-panel.png'),
(9,  'Rubber Gasket Set',      'Assorted gasket kit',            'gasket-set.png'),
(10, 'Forklift Battery 48V',   'Industrial 48V traction battery', 'forklift-batt.png');

-- ---------------------------------------------------------------------------
-- Inventory (inventory_id is NOT auto-increment)
-- Product 8 @ warehouse 2 is deliberately below reorder_level (reorder timer)
-- ---------------------------------------------------------------------------

INSERT INTO inventory (inventory_id, wearhouses_wearhous_id, products_product_id, qty, reorder_level, last_updated_at, unit_price) VALUES
( 1, 1,  1, 480, 20, '2026-01-15 08:00:00', 12.50),
( 2, 1,  2, 290, 20, '2026-01-15 08:00:00',  8.25),
( 3, 1,  3, 140, 20, '2026-01-15 08:00:00', 34.90),
( 4, 1,  4, 195, 20, '2026-01-15 08:00:00', 19.75),
( 5, 1,  5, 760, 20, '2026-01-15 08:00:00',  6.40),
( 6, 1,  6, 120, 15, '2026-01-20 09:00:00', 45.00),
( 7, 1,  7,  85, 10, '2026-01-20 09:00:00', 125.00),
( 8, 1,  8,  60, 10, '2026-01-20 09:00:00',  89.99),
( 9, 1,  9, 400, 25, '2026-01-20 09:00:00',  14.50),
(10, 1, 10,  25,  5, '2026-01-20 09:00:00', 890.00),
(11, 2,  4, 310, 20, '2026-01-18 10:00:00', 18.50),
(12, 2,  5, 520, 20, '2026-01-18 10:00:00',  6.10),
(13, 2,  8,   8, 20, '2026-01-25 11:00:00', 85.00),
(14, 3,  7, 150, 15, '2026-01-22 12:00:00', 118.00),
(15, 3,  9, 220, 20, '2026-01-22 12:00:00',  13.80);

-- ---------------------------------------------------------------------------
-- Supplier catalog (who supplies what, from which warehouse, lead time days)
-- ---------------------------------------------------------------------------

INSERT INTO supplier_providing_products (products_product_id, suppliers_supplier_id, wearhouses_wearhous_id, lead_time_in_days) VALUES
(1, 1, 1, 7),  (2, 1, 1, 5),  (3, 1, 1, 10),
(4, 2, 2, 14), (5, 2, 2, 7),  (6, 2, 2, 21),
(7, 3, 3, 12), (8, 3, 3, 18), (9, 3, 3, 9);

-- ---------------------------------------------------------------------------
-- Customer orders
-- ---------------------------------------------------------------------------

INSERT INTO orders (order_id, ordered_at, total_price, customers_customer_id, status) VALUES
(1, '2026-02-01 14:30:00',  37.50, 1, 'PLACED'),
(2, '2026-02-03 09:15:00',  34.90, 2, 'PLACED'),
(3, '2026-01-28 16:00:00', 156.65, 3, 'DELIVERED'),
(4, '2026-01-10 11:00:00',  25.00, 1, 'CANCELLED');

INSERT INTO order_items (order_item_id, qty, unit_price, products_product_id, orders_order_id) VALUES
(1, 3, 12.50, 1, 1),
(2, 1, 34.90, 3, 2),
(3, 2, 12.50, 1, 3),
(4, 1, 19.75, 4, 3),
(5, 1, 89.99, 8, 3),
(6, 2, 12.50, 1, 4);

-- ---------------------------------------------------------------------------
-- Purchase orders
--  PO 1 : open — E2E flow default (7 × steel pipe)
--  PO 2 : open — second open PO for same supplier
--  PO 3 : open — Pacific Parts
--  PO 4 : completed (GRN recorded)
--  PO 5 : open but overdue (created 45 days ago, 7-day lead time)
--  PO 6 : open — Euro Industrial
-- ---------------------------------------------------------------------------

INSERT INTO purchase_orders (po_id, suppliers_supplier_id, created_at, total_price, is_completed, products_product_id, requesting_qty) VALUES
(1, 1, '2026-02-10 10:00:00',   87.50, 0, 1,  7),
(2, 1, '2026-02-08 09:00:00',  412.50, 0, 2, 50),
(3, 2, '2026-02-05 14:00:00', 1975.00, 0, 4, 100),
(4, 3, '2026-01-05 08:00:00', 3750.00, 1, 7, 30),
(5, 2, '2025-12-01 08:00:00', 1280.00, 0, 5, 200),
(6, 3, '2026-02-12 11:00:00',  414.00, 0, 9, 30);

-- ---------------------------------------------------------------------------
-- Shipments
--  1 : legacy-style demo (no PO) — IN_TRANSIT for carrier-status timer
--  2 : PO 2 — CREATED (supplier can see, not yet in transit)
--  3 : PO 3 — IN_TRANSIT, customs PENDING
--  4 : PO 5 — DELIVERED, customs CLEARED — awaiting GRN (warehouse manager)
--  5 : PO 4 — COMPLETED (GRN done)
--  6 : PO 6 — CREATED
-- ---------------------------------------------------------------------------

INSERT INTO shipments (shipment_id, tracking_number, vessal_id, type, wearhouses_wearhous_id, status, shipment_type, ref, purchase_orders_po_id) VALUES
(1, 'TRK-0001',      'VESSEL-ALPHA',  'SEA', 1, 'IN_TRANSIT', NULL, NULL,              NULL),
(2, 'TRK-PO2-001',   'VESSEL-BETA',   'SEA', 1, 'CREATED',    NULL, NULL,               2),
(3, 'TRK-PO3-001',   'VESSEL-GAMMA',  'SEA', 2, 'IN_TRANSIT', NULL, NULL,               3),
(4, 'TRK-PO5-OVERDUE','VESSEL-DELTA', 'SEA', 2, 'DELIVERED',  NULL, 'CARRIER-legacy',   5),
(5, 'TRK-PO4-DONE',  'VESSEL-EPS',    'SEA', 3, 'COMPLETED',  NULL, 'CARRIER-PO4-REF', 4),
(6, 'TRK-PO6-001',   'VESSEL-ZETA',   'AIR', 3, 'CREATED',    NULL, NULL,               6);

-- ---------------------------------------------------------------------------
-- Customs clearance
-- ---------------------------------------------------------------------------

INSERT INTO custom_clearence_records (record_id, declaration_number, supplier_shipments_shipment_id, status) VALUES
(1, 'DEC-PO3-2026-001', 3, 'PENDING'),
(2, 'DEC-PO5-2025-099', 4, 'CLEARED'),
(3, 'DEC-PO4-2026-002', 5, 'CLEARED');

-- ---------------------------------------------------------------------------
-- GRN (PO 4 completed — stock was incremented in app; qty reflects intent)
-- ---------------------------------------------------------------------------

INSERT INTO grns (grn_id, suppliers_supplier_id, created_at, purchase_orders_po_id, products_product_id, qty) VALUES
(1, 3, '2026-01-20 15:00:00', 4, 7, 30);

-- ---------------------------------------------------------------------------
-- Audit trail samples (vendor performance + operational events)
-- ---------------------------------------------------------------------------

INSERT INTO audit_records (id, created_at, resource, action, details, type, reference) VALUES
(1, '2026-02-03 03:00:00', 'PROCUREMENT', 'recomputeForSupplier', '8/10 on time (80.0%)',  'VENDOR_PERFORMANCE', '1'),
(2, '2026-02-03 03:00:01', 'PROCUREMENT', 'recomputeForSupplier', '6/8 on time (75.0%)',   'VENDOR_PERFORMANCE', '2'),
(3, '2026-02-03 03:00:02', 'PROCUREMENT', 'recomputeForSupplier', '5/5 on time (100.0%)',  'VENDOR_PERFORMANCE', '3'),
(4, '2026-02-10 10:05:00', 'PROCUREMENT', 'createPo',             'PO 1 steel pipe x7',    'PROCUREMENT',        '1'),
(5, '2026-01-20 15:01:00', 'PROCUREMENT', 'recordGrnForShipment', 'GRN qty 30 PO 4',     'PROCUREMENT',        '4'),
(6, '2026-02-05 14:30:00', 'LOGISTICS',   'updateStatus',         'IN_TRANSIT',          'LOGISTICS',          '3');

-- ---------------------------------------------------------------------------
-- Done
-- ---------------------------------------------------------------------------

SELECT 'test-data.sql loaded' AS status,
       (SELECT COUNT(*) FROM users) AS users,
       (SELECT COUNT(*) FROM customers) AS customers,
       (SELECT COUNT(*) FROM suppliers) AS suppliers,
       (SELECT COUNT(*) FROM products) AS products,
       (SELECT COUNT(*) FROM purchase_orders) AS purchase_orders,
       (SELECT COUNT(*) FROM shipments) AS shipments;

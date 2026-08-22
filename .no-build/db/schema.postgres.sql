-- PostgreSQL translation of schema.sql (MySQL Workbench source of truth).
-- Regenerate by hand if schema.sql changes.

CREATE TABLE IF NOT EXISTS regions (
  region_key VARCHAR(45) NOT NULL,
  region_name VARCHAR(45) NOT NULL,
  PRIMARY KEY (region_key)
);

CREATE TABLE IF NOT EXISTS customers (
  user_id SERIAL NOT NULL,
  email VARCHAR(45) NOT NULL,
  is_active VARCHAR(45) NOT NULL DEFAULT 'true',
  full_name VARCHAR(45) NOT NULL,
  mobile_1 VARCHAR(45) NULL,
  mobile_2 VARCHAR(45) NULL,
  address VARCHAR(45) NULL,
  country VARCHAR(45) NULL,
  regions_region_key VARCHAR(45) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_customers_regions1
    FOREIGN KEY (regions_region_key)
    REFERENCES regions (region_key)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_customers_regions1_idx ON customers (regions_region_key);

CREATE TABLE IF NOT EXISTS products (
  product_id SERIAL NOT NULL,
  name VARCHAR(45) NOT NULL,
  description VARCHAR(45) NOT NULL,
  product_image VARCHAR(45) NOT NULL,
  PRIMARY KEY (product_id)
);

CREATE TABLE IF NOT EXISTS orders (
  order_id SERIAL NOT NULL,
  ordered_at TIMESTAMP NOT NULL,
  total_price DOUBLE PRECISION NOT NULL,
  customers_customer_id INT NOT NULL,
  PRIMARY KEY (order_id),
  CONSTRAINT fk_orders_users1
    FOREIGN KEY (customers_customer_id)
    REFERENCES customers (user_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_orders_users1_idx ON orders (customers_customer_id);

CREATE TABLE IF NOT EXISTS order_items (
  order_item_id SERIAL NOT NULL,
  qty INT NOT NULL,
  unit_price DOUBLE PRECISION NOT NULL,
  products_product_id INT NOT NULL,
  orders_order_id INT NOT NULL,
  PRIMARY KEY (order_item_id),
  CONSTRAINT fk_order_items_products1
    FOREIGN KEY (products_product_id)
    REFERENCES products (product_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_order_items_orders1
    FOREIGN KEY (orders_order_id)
    REFERENCES orders (order_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_order_items_products1_idx ON order_items (products_product_id);
CREATE INDEX IF NOT EXISTS fk_order_items_orders1_idx ON order_items (orders_order_id);

CREATE TABLE IF NOT EXISTS suppliers (
  supplier_id SERIAL NOT NULL,
  email VARCHAR(45) NOT NULL,
  is_active VARCHAR(45) NOT NULL DEFAULT 'true',
  full_name VARCHAR(45) NOT NULL,
  mobile_1 VARCHAR(45) NOT NULL,
  mobile_2 VARCHAR(45) NULL,
  address VARCHAR(45) NOT NULL,
  country VARCHAR(45) NOT NULL,
  PRIMARY KEY (supplier_id)
);

CREATE TABLE IF NOT EXISTS purchase_orders (
  po_id SERIAL NOT NULL,
  suppliers_supplier_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  total_price DOUBLE PRECISION NOT NULL,
  is_completed SMALLINT NOT NULL,
  products_product_id INT NOT NULL,
  requesting_qty INT NOT NULL,
  PRIMARY KEY (po_id),
  CONSTRAINT fk_table1_suppliers1
    FOREIGN KEY (suppliers_supplier_id)
    REFERENCES suppliers (supplier_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_purchase_orders_products1
    FOREIGN KEY (products_product_id)
    REFERENCES products (product_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_table1_suppliers1_idx ON purchase_orders (suppliers_supplier_id);
CREATE INDEX IF NOT EXISTS fk_purchase_orders_products1_idx ON purchase_orders (products_product_id);

CREATE TABLE IF NOT EXISTS grns (
  grn_id SERIAL NOT NULL,
  suppliers_supplier_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  purchase_orders_po_id INT NOT NULL,
  products_product_id INT NOT NULL,
  qty INT NOT NULL,
  PRIMARY KEY (grn_id),
  CONSTRAINT fk_grns_suppliers1
    FOREIGN KEY (suppliers_supplier_id)
    REFERENCES suppliers (supplier_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_grns_purchase_orders1
    FOREIGN KEY (purchase_orders_po_id)
    REFERENCES purchase_orders (po_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_grns_products1
    FOREIGN KEY (products_product_id)
    REFERENCES products (product_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_grns_suppliers1_idx ON grns (suppliers_supplier_id);
CREATE INDEX IF NOT EXISTS fk_grns_purchase_orders1_idx ON grns (purchase_orders_po_id);
CREATE INDEX IF NOT EXISTS fk_grns_products1_idx ON grns (products_product_id);

CREATE TABLE IF NOT EXISTS wearhouses (
  wearhous_id INT NOT NULL,
  country VARCHAR(45) NOT NULL,
  regions_region_key VARCHAR(45) NOT NULL,
  PRIMARY KEY (wearhous_id),
  CONSTRAINT fk_wearhouses_regions1
    FOREIGN KEY (regions_region_key)
    REFERENCES regions (region_key)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_wearhouses_regions1_idx ON wearhouses (regions_region_key);

CREATE TABLE IF NOT EXISTS inventory (
  inventory_id INT NOT NULL,
  wearhouses_wearhous_id INT NOT NULL,
  products_product_id INT NOT NULL,
  qty INT NOT NULL,
  reorder_level INT NOT NULL,
  last_updated_at TIMESTAMP NOT NULL DEFAULT now(),
  unit_price DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (inventory_id),
  CONSTRAINT fk_inventory_wearhouses1
    FOREIGN KEY (wearhouses_wearhous_id)
    REFERENCES wearhouses (wearhous_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_inventory_products1
    FOREIGN KEY (products_product_id)
    REFERENCES products (product_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_inventory_wearhouses1_idx ON inventory (wearhouses_wearhous_id);
CREATE INDEX IF NOT EXISTS fk_inventory_products1_idx ON inventory (products_product_id);

CREATE TABLE IF NOT EXISTS audit_records (
  id SERIAL NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  resource VARCHAR(45) NOT NULL,
  action VARCHAR(45) NOT NULL,
  details VARCHAR(45) NULL,
  type VARCHAR(45) NOT NULL,
  reference VARCHAR(45) NOT NULL,
  PRIMARY KEY (id, reference, type)
);

CREATE TABLE IF NOT EXISTS logs (
  created_at INT NOT NULL,
  idempotency_key VARCHAR(45) NOT NULL,
  log_level VARCHAR(45) NOT NULL,
  messages VARCHAR(45) NOT NULL,
  class_name VARCHAR(45) NOT NULL,
  method_name VARCHAR(45) NOT NULL,
  file_name VARCHAR(45) NOT NULL,
  line_nuber VARCHAR(45) NOT NULL,
  thread_name VARCHAR(45) NOT NULL,
  PRIMARY KEY (created_at, idempotency_key)
);

CREATE TABLE IF NOT EXISTS shipments (
  shipment_id SERIAL NOT NULL,
  tracking_number VARCHAR(45) NOT NULL,
  vessal_id VARCHAR(45) NOT NULL,
  type VARCHAR(45) NOT NULL,
  wearhouses_wearhous_id INT NOT NULL,
  status VARCHAR(45) NOT NULL,
  shipment_type VARCHAR(45) NULL,
  ref VARCHAR(45) NULL,
  PRIMARY KEY (shipment_id),
  CONSTRAINT fk_supplier_shipments_wearhouses1
    FOREIGN KEY (wearhouses_wearhous_id)
    REFERENCES wearhouses (wearhous_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_supplier_shipments_wearhouses1_idx ON shipments (wearhouses_wearhous_id);

CREATE TABLE IF NOT EXISTS custom_clearence_records (
  record_id SERIAL NOT NULL,
  declaration_number VARCHAR(45) NULL,
  supplier_shipments_shipment_id INT NOT NULL,
  status VARCHAR(45) NULL,
  PRIMARY KEY (record_id),
  CONSTRAINT fk_custom_clearence_records_supplier_shipments1
    FOREIGN KEY (supplier_shipments_shipment_id)
    REFERENCES shipments (shipment_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_custom_clearence_records_supplier_shipments1_idx ON custom_clearence_records (supplier_shipments_shipment_id);

CREATE TABLE IF NOT EXISTS supplier_providing_products (
  products_product_id INT NOT NULL,
  suppliers_supplier_id INT NOT NULL,
  wearhouses_wearhous_id INT NOT NULL,
  lead_time_in_days INT NOT NULL,
  PRIMARY KEY (products_product_id, suppliers_supplier_id, wearhouses_wearhous_id),
  CONSTRAINT fk_products_has_suppliers_products1
    FOREIGN KEY (products_product_id)
    REFERENCES products (product_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_products_has_suppliers_suppliers1
    FOREIGN KEY (suppliers_supplier_id)
    REFERENCES suppliers (supplier_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_supplier_providing_products_wearhouses1
    FOREIGN KEY (wearhouses_wearhous_id)
    REFERENCES wearhouses (wearhous_id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);
CREATE INDEX IF NOT EXISTS fk_products_has_suppliers_suppliers1_idx ON supplier_providing_products (suppliers_supplier_id);
CREATE INDEX IF NOT EXISTS fk_products_has_suppliers_products1_idx ON supplier_providing_products (products_product_id);
CREATE INDEX IF NOT EXISTS fk_supplier_providing_products_wearhouses1_idx ON supplier_providing_products (wearhouses_wearhous_id);

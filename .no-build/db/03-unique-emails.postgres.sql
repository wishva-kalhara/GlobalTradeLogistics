-- Additive: self-service signup (customer/supplier) needs to reject
-- duplicate emails. This adds unique indexes without altering the
-- original table definitions in schema.postgres.sql.

CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email_unique ON customers (email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_suppliers_email_unique ON suppliers (email);

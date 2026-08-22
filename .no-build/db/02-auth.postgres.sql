-- Additive auth tables for passwordless (OTP-based) authentication.
-- Loaded after 01-schema.postgres.sql (docker-entrypoint-initdb.d runs
-- files in filename order) — does not modify any of the 15 existing tables.

CREATE TABLE IF NOT EXISTS users (
  user_id SERIAL NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  role VARCHAR(45) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS otp_codes (
  id SERIAL NOT NULL,
  email VARCHAR(255) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  purpose VARCHAR(45) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_otp_codes_email ON otp_codes (email);

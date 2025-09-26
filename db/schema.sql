-- PHASE B — CORES ONLY (NO FKs here). Replace TODOs with actual DDL later.
-- Goal: Define 5 core tables with PK + business fields + audit trio.
-- DoD: schema loads cleanly; audit trio on every core; ZERO FOREIGN KEY clauses in cores.
CREATE DATABASE IF NOT EXISTS ccinfom_dev;
USE ccinfom_dev;

DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS customers;

-- [PRODUCTS]
-- TODO: CREATE TABLE products (
--   PK: product_id INT AUTO_INCREMENT PRIMARY KEY
--   Business fields: sku (UNIQUE), name, category, unit_price, unit_of_measure
--   Inventory fields: on_hand_qty DECIMAL, reserved_qty DECIMAL
--   Status: active_flag BOOLEAN
--   Audit trio: created_at, updated_at (with ON UPDATE), updated_by
-- );

CREATE TABLE customers (
  customer_id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  customer_name             VARCHAR(200) NOT NULL,
  contact_person            VARCHAR(200),
  phone                     VARCHAR(40),
  email                     VARCHAR(120),
  default_delivery_address  VARCHAR(300) NOT NULL, 
  created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by                VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT ck_customers_email CHECK (email IS NULL OR email LIKE '%@%')
);

CREATE TABLE employees (
  employee_id   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  last_name     VARCHAR(100) NOT NULL,
  first_name    VARCHAR(100) NOT NULL,
  employee_role          ENUM('picker','packer','dispatcher') NOT NULL,
  phone         VARCHAR(20),
  email         VARCHAR(150) UNIQUE,
  employee_status        ENUM('active','inactive') DEFAULT 'active',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by    VARCHAR(64) NOT NULL DEFAULT 'system'
);

-- [VEHICLES]
-- TODO: CREATE TABLE vehicles (
--   PK: vehicle_id
--   Business fields: plate_number (UNIQUE), vehicle_type, capacity DECIMAL, status (available/maintenance)
--   Audit trio: created_at, updated_at, updated_by
-- );

CREATE TABLE branches (
  branch_id       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  branch_name     VARCHAR(200) NOT NULL,
  address         VARCHAR(300) NOT NULL,           
  city            VARCHAR(120) NOT NULL,
  contact_person  VARCHAR(200),
  phone           VARCHAR(40),
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system'
);


-- NOTES:
-- • Keep foreign keys OUT of core tables. Relationships live in transaction tables (Phase C/E).
-- • Use snake_case; PK names = <table>_id; include DEFAULTs for numeric/text as appropriate.
-- • After implementing, run on a clean DB before pushing (see docs/runbook.md).

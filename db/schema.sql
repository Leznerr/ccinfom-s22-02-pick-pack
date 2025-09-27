-- PHASE B — CORES ONLY (NO FKs here). Replace TODOs with actual DDL later.
-- Goal: Define 5 core tables with PK + business fields + audit trio.
-- DoD: schema loads cleanly; audit trio on every core; ZERO FOREIGN KEY clauses in cores.

CREATE DATABASE IF NOT EXISTS ccinfom_dev;
USE ccinfom_dev;


DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS vehicles;

CREATE TABLE products (
  product_id       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  sku              VARCHAR(64) NOT NULL UNIQUE,
  product_name     VARCHAR(200) NOT NULL,
  category         VARCHAR(100) NOT NULL,
  unit_price       DECIMAL(12,2) NOT NULL CHECK (unit_price >= 0),
  unit_of_measure  VARCHAR(50) NOT NULL,
  on_hand_qty      DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (on_hand_qty >= 0),
  reserved_qty     DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
  active_flag      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NOT NULL DEFAULT 'system'
);

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
  employee_id   			BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  last_name     			VARCHAR(100) NOT NULL,
  first_name    			VARCHAR(100) NOT NULL,
  employee_role          	ENUM('picker','packer','dispatcher') NOT NULL,
  phone         			VARCHAR(20),
  email         			VARCHAR(150) UNIQUE,
  employee_status        	ENUM('active','inactive') NOT NULL DEFAULT 'active',
  created_at    			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by    			VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE TABLE vehicles (
  vehicle_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  plate_number      VARCHAR(10) NOT NULL UNIQUE,
  vehicle_type      ENUM('van','truck','motorcycle') NOT NULL,
  capacity          DECIMAL(12,2) NOT NULL CHECK (capacity >= 0),
  vehicle_status            ENUM('available','maintenance','inactive') NOT NULL DEFAULT 'available',
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by        VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE TABLE branches (
  branch_id       			BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  branch_name     			VARCHAR(200) NOT NULL,
  address         			VARCHAR(300) NOT NULL,           
  city            			VARCHAR(120) NOT NULL,
  contact_person  			VARCHAR(200),
  phone           			VARCHAR(40),
  created_at      			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      			TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      			VARCHAR(64) NOT NULL DEFAULT 'system'
);


-- NOTES:
-- • Keep foreign keys OUT of core tables. Relationships live in transaction tables (Phase C/E).
-- • Use snake_case; PK names = <table>_id; include DEFAULTs for numeric/text as appropriate.
-- • After implementing, run on a clean DB before pushing (see docs/runbook.md).
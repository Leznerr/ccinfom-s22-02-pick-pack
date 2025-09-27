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


/* ==========================================
   PHASE C — T1/T2 DDL TODO (production-ready)
   Scope: add transactions w/ FKs, CHECKs, UNIQUEs, indexes + inventory/status logic.
   ------------------------------------------
   BUILD
   [ ] Create T1 tables:
       - pick_ticket_hdr(pk BIGINT AI, customer_id FK→customers, branch_id FK→branches,
         ticket_status ENUM('Open','Picking','Packed','Dispatched','Delivered','Short-Closed') DEFAULT 'Open',
         remarks, audit trio)
       - pick_ticket_line(pk BIGINT AI, pick_ticket_id FK→pick_ticket_hdr ON DELETE CASCADE,
         product_id FK→products, requested_qty DECIMAL(12,2) CHECK(requested_qty > 0),
         uom VARCHAR(50), UNIQUE(pick_ticket_id,product_id), audit trio)
       - Indexes: idx_pt_hdr_customer_id, idx_pt_hdr_branch_id, idx_pt_hdr_status, idx_pt_line_product_id
   [ ] Create T2 tables:
       - picking_hdr(pk BIGINT AI, pick_ticket_id FK→pick_ticket_hdr, picker_employee_id FK→employees,
         picking_status ENUM('Picking','Picked','Cancelled') DEFAULT 'Picking',
         started_at, completed_at NULL, audit trio, UNIQUE(pick_ticket_id))
       - picking_line(pk BIGINT AI, picking_id FK→picking_hdr ON DELETE CASCADE,
         product_id FK→products, picked_qty DECIMAL(12,2) CHECK(picked_qty > 0),
         uom VARCHAR(50), UNIQUE(picking_id,product_id), audit trio)
       - Indexes: idx_pick_hdr_ticket_id, idx_pick_hdr_picker_id, idx_pick_hdr_status, idx_pick_line_product_id
   [ ] DO NOT add FKs to core tables (cores stay FK-free).

   INVENTORY & STATUS
   [ ] Implement inventory effects via TRIGGERS on picking_line:
       - AFTER INSERT: products.reserved_qty += NEW.picked_qty
       - AFTER UPDATE: products.reserved_qty += (NEW.picked_qty - OLD.picked_qty)
       - AFTER DELETE: products.reserved_qty -= OLD.picked_qty
       - Guard: available = on_hand_qty - reserved_qty; IF available < delta THEN SIGNAL '45000' 'Insufficient available'
   [ ] Status automation: AFTER INSERT on picking_hdr → set pick_ticket_hdr.ticket_status='Picking'

   (Alternative: single proc sp_allocate_and_pick(...) with SELECT ... FOR UPDATE; choose ONE approach and keep consistent.)

   DEFINITION OF DONE (DDL)
   [ ] All 4 tx tables created with PK/FK/CHECK/UNIQUE + indexes
   [ ] Triggers/proc compile with 0 warnings; status automation works
   [ ] Names/ENUMs match spec; no FKs added to cores; file runs clean on fresh DB
========================================== */

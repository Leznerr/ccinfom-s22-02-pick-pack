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
       - pick_ticket_hdr (
           pick_ticket_id BIGINT AI PK,
           customer_id BIGINT NOT NULL FK→customers(customer_id),
           branch_id   BIGINT NOT NULL FK→branches(branch_id),
           ticket_status ENUM('Open','Picking','Packed','Dispatched','Delivered','Short-Closed') NOT NULL DEFAULT 'Open',
           remarks VARCHAR(300) NULL,
           created_at, updated_at, updated_by  -- audit trio
         )
       - pick_ticket_line (
           ticket_line_id BIGINT AI PK,
           pick_ticket_id BIGINT NOT NULL FK→pick_ticket_hdr(pick_ticket_id) ON DELETE CASCADE,
           product_id BIGINT NOT NULL FK→products(product_id),
           requested_qty DECIMAL(12,2) NOT NULL CHECK(requested_qty > 0),
           uom VARCHAR(50) NOT NULL,
           UNIQUE (pick_ticket_id, product_id),
           created_at, updated_at, updated_by
           -- (Optional) line_status ENUM('Valid','Invalid','Duplicate','Cancelled') DEFAULT 'Valid'
         )
       - Indexes: idx_pt_hdr_customer_id, idx_pt_hdr_branch_id, idx_pt_hdr_status,
                  idx_pt_line_product_id

   [ ] Create T2 tables:
       - picking_hdr (
           picking_id BIGINT AI PK,
           pick_ticket_id BIGINT NOT NULL FK→pick_ticket_hdr(pick_ticket_id),
           picker_employee_id BIGINT NOT NULL FK→employees(employee_id),
           picking_status ENUM('Picking','Done','Cancelled') NOT NULL DEFAULT 'Picking',
           started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
           completed_at TIMESTAMP NULL,
           UNIQUE(pick_ticket_id),     -- one picking batch per ticket
           created_at, updated_at, updated_by
         )
       - picking_line (
           picking_line_id BIGINT AI PK,
           picking_id BIGINT NOT NULL FK→picking_hdr(picking_id) ON DELETE CASCADE,
           ticket_line_id BIGINT NOT NULL FK→pick_ticket_line(ticket_line_id),
           product_id BIGINT NOT NULL FK→products(product_id),
           picked_qty DECIMAL(12,2) NOT NULL CHECK(picked_qty > 0),
           uom VARCHAR(50) NOT NULL,
           -- tie to the ticket line: prevents duplicates at line level
           UNIQUE (picking_id, ticket_line_id),
           -- helpful meta for Phase E/Reports
           short_reason VARCHAR(200) NULL,
           scan_ref     VARCHAR(80)  NULL,
           created_at, updated_at, updated_by
         )
       - Indexes: idx_pick_hdr_ticket_id, idx_pick_hdr_picker_id, idx_pick_hdr_status,
                  idx_pick_line_product_id, idx_pick_line_ticket_line_id

   [ ] DO NOT add FKs to core tables (cores stay FK-free).

   INVENTORY & STATUS
   [ ] BEFORE INSERT/UPDATE picking_line guard (enforce business rules):
       - Ensure ticket_line_id belongs to the same pick_ticket_id as picking_hdr.
       - Ensure product_id matches the ticket line’s product_id.
       - Ensure SUM(picked_qty by ticket_line_id, including NEW) ≤ requested_qty.
       - Ensure product is active (products.active_flag = TRUE).
       - Compute delta = NEW.picked_qty - COALESCE(OLD.picked_qty,0);
         available = products.on_hand_qty - products.reserved_qty;
         IF available < delta THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient available';
   [ ] AFTER INSERT/UPDATE/DELETE picking_line inventory effects:
       - AI: products.reserved_qty += NEW.picked_qty
       - AU: products.reserved_qty += (NEW.picked_qty - OLD.picked_qty)
       - AD: products.reserved_qty -= OLD.picked_qty
   [ ] AFTER INSERT picking_hdr → set pick_ticket_hdr.ticket_status='Picking'

   (Alternative: one proc sp_allocate_and_pick(...) using SELECT ... FOR UPDATE; pick either triggers OR proc and keep consistent.)

   DEFINITION OF DONE (DDL)
   [ ] All 4 tx tables created with PK/FK/CHECK/UNIQUE + indexes
   [ ] Guards + inventory/status triggers (or proc) compile with 0 warnings
   [ ] ENUMs/names match spec; cores remain FK-free; file runs clean on fresh DB
========================================== */

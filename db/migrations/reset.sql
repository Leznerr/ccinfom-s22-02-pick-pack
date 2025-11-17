-- ==========================================================
-- CCINFOM DEV — Phase B (Cores) + Phase C (T1 + T2)
-- Database: ccinfom_dev
-- ==========================================================

-- Ensure database exists and select it
CREATE DATABASE IF NOT EXISTS ccinfom_dev;
USE ccinfom_dev;

SET @old_fk = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- Phase E (T3-T5)
DROP TABLE IF EXISTS close_variance;
DROP TABLE IF EXISTS close_hdr;
DROP TABLE IF EXISTS dispatch_line;
DROP TABLE IF EXISTS dispatch_hdr;
DROP TABLE IF EXISTS pack_box_line;
DROP TABLE IF EXISTS pack_box_hdr;

-- T2 (Picking): drop triggers then tables
DROP TRIGGER IF EXISTS trg_pick_hdr_ai_set_status;
DROP TRIGGER IF EXISTS trg_pick_line_bi_guard;
DROP TRIGGER IF EXISTS trg_pick_line_bu_guard;
DROP TRIGGER IF EXISTS trg_pick_line_ai_reserve;
DROP TRIGGER IF EXISTS trg_pick_line_au_reserve;
DROP TRIGGER IF EXISTS trg_pick_line_ad_reserve;

DROP TABLE IF EXISTS picking_line;
DROP TABLE IF EXISTS picking_hdr;

-- T1 (Pick Ticket)
DROP TABLE IF EXISTS pick_ticket_line;
DROP TABLE IF EXISTS pick_ticket_hdr;

-- Cores
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS vehicles;

SET FOREIGN_KEY_CHECKS = @old_fk;

-- ==========================================================
-- Phase B — CORES (no foreign keys in cores)
-- ==========================================================

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
  customer_status           ENUM('active','inactive') NOT NULL DEFAULT 'active',
  created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by                VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT ck_customers_email CHECK (email IS NULL OR email LIKE '%@%')
);

CREATE TABLE employees (
  employee_id       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  last_name         VARCHAR(100) NOT NULL,
  first_name        VARCHAR(100) NOT NULL,
  employee_role     ENUM('picker','packer','dispatcher','driver') NOT NULL,
  phone             VARCHAR(20),
  email             VARCHAR(150) UNIQUE,
  employee_status   ENUM('active','inactive') NOT NULL DEFAULT 'active',
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by        VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE TABLE vehicles (
  vehicle_id        BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  plate_number      VARCHAR(10) NOT NULL UNIQUE,
  vehicle_type      ENUM('van','truck','motorcycle') NOT NULL,
  capacity          DECIMAL(12,2) NOT NULL CHECK (capacity >= 0),
  sla_hours         TINYINT UNSIGNED NOT NULL DEFAULT 24,
  vehicle_status    ENUM('available','maintenance','inactive') NOT NULL DEFAULT 'available',
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by        VARCHAR(64) NOT NULL DEFAULT 'system'
);

CREATE TABLE branches (
  branch_id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  branch_name       VARCHAR(200) NOT NULL,
  address           VARCHAR(300) NOT NULL,
  city              VARCHAR(120) NOT NULL,
  contact_person    VARCHAR(200),
  phone             VARCHAR(40),
  branch_status     ENUM('active','inactive') NOT NULL DEFAULT 'active',
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by        VARCHAR(64) NOT NULL DEFAULT 'system'
);

-- ==========================================================
-- Phase C — T1 (Pick Ticket)
-- ==========================================================

CREATE TABLE pick_ticket_hdr (
  pick_ticket_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  customer_id     BIGINT UNSIGNED NOT NULL,
  branch_id       BIGINT UNSIGNED NOT NULL,
  ticket_status   ENUM('Open','Picking','Packed','Dispatched','Delivered','Closed')
                  NOT NULL DEFAULT 'Open',
  remarks         VARCHAR(300) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT fk_pt_hdr_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id),
  CONSTRAINT fk_pt_hdr_branch FOREIGN KEY (branch_id)
    REFERENCES branches(branch_id)
);

CREATE INDEX idx_pt_hdr_customer_id ON pick_ticket_hdr (customer_id);
CREATE INDEX idx_pt_hdr_branch_id   ON pick_ticket_hdr (branch_id);
CREATE INDEX idx_pt_hdr_status      ON pick_ticket_hdr (ticket_status);

CREATE TABLE pick_ticket_line (
  ticket_line_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  product_id      BIGINT UNSIGNED NOT NULL,
  requested_qty   DECIMAL(12,2) NOT NULL CHECK (requested_qty > 0),
  uom             VARCHAR(50) NOT NULL,
  line_status     ENUM('Valid','Invalid','Duplicate','Cancelled') DEFAULT 'Valid',
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT fk_pt_line_hdr FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_pt_line_product FOREIGN KEY (product_id)
    REFERENCES products(product_id),
  CONSTRAINT uq_pt_line UNIQUE (pick_ticket_id, product_id)
);

CREATE INDEX idx_pt_line_product_id ON pick_ticket_line (product_id);
CREATE INDEX idx_pt_line_ticket_id  ON pick_ticket_line (pick_ticket_id);

-- ==========================================================
-- Phase C — T2 (Picking) + triggers
-- ==========================================================

CREATE TABLE picking_hdr (
  picking_id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  pick_ticket_id      BIGINT UNSIGNED NOT NULL,
  picker_employee_id  BIGINT UNSIGNED NOT NULL,
  picking_status      ENUM('Picking','Done','Cancelled') NOT NULL DEFAULT 'Picking',
  started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at        TIMESTAMP NULL,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by          VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT ck_picking_times
    CHECK (completed_at IS NULL OR completed_at >= started_at),
  CONSTRAINT fk_pick_hdr_ticket
    FOREIGN KEY (pick_ticket_id)     REFERENCES pick_ticket_hdr(pick_ticket_id),
  CONSTRAINT fk_pick_hdr_employee
    FOREIGN KEY (picker_employee_id) REFERENCES employees(employee_id),
  CONSTRAINT uq_pick_hdr_ticket UNIQUE (pick_ticket_id)  -- one picking batch per ticket
);

CREATE INDEX idx_pick_hdr_ticket_id ON picking_hdr (pick_ticket_id);
CREATE INDEX idx_pick_hdr_picker_id ON picking_hdr (picker_employee_id);
CREATE INDEX idx_pick_hdr_status    ON picking_hdr (picking_status);

CREATE TABLE picking_line (
  picking_line_id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  picking_id      BIGINT UNSIGNED NOT NULL,
  ticket_line_id  BIGINT UNSIGNED NOT NULL,
  product_id      BIGINT UNSIGNED NOT NULL,
  picked_qty      DECIMAL(12,2) NOT NULL CHECK (picked_qty > 0),
  uom             VARCHAR(50) NOT NULL,
  short_reason    VARCHAR(200) NULL,   -- shortages/notes (reporting)
  scan_ref        VARCHAR(80)  NULL,   -- barcode/scan reference
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  CONSTRAINT fk_pick_line_hdr
    FOREIGN KEY (picking_id)     REFERENCES picking_hdr(picking_id) ON DELETE CASCADE,
  CONSTRAINT fk_pick_line_ticket_line
    FOREIGN KEY (ticket_line_id) REFERENCES pick_ticket_line(ticket_line_id),
  CONSTRAINT fk_pick_line_product
    FOREIGN KEY (product_id)     REFERENCES products(product_id),
  CONSTRAINT uq_pick_line UNIQUE (picking_id, ticket_line_id)
);

CREATE INDEX idx_pick_line_picking_id     ON picking_line (picking_id);
CREATE INDEX idx_pick_line_product_id     ON picking_line (product_id);
CREATE INDEX idx_pick_line_ticket_line_id ON picking_line (ticket_line_id);

-- ==========================================
-- Triggers: business guards + inventory effects + status flip
-- ==========================================
DELIMITER $$

-- After creating a picking batch, flip ticket to 'Picking' (if it was 'Open')
CREATE TRIGGER trg_pick_hdr_ai_set_status
AFTER INSERT ON picking_hdr
FOR EACH ROW
BEGIN
  UPDATE pick_ticket_hdr
     SET ticket_status = 'Picking',
         updated_at = CURRENT_TIMESTAMP,
         updated_by = NEW.updated_by
   WHERE pick_ticket_id = NEW.pick_ticket_id
     AND ticket_status = 'Open';
END$$

-- BEFORE INSERT guard on picking_line
CREATE TRIGGER trg_pick_line_bi_guard
BEFORE INSERT ON picking_line
FOR EACH ROW
BEGIN
  DECLARE v_hdr_ticket     BIGINT UNSIGNED;
  DECLARE v_ticket_id      BIGINT UNSIGNED;
  DECLARE v_ticket_product BIGINT UNSIGNED;
  DECLARE v_requested      DECIMAL(12,2);
  DECLARE v_active         BOOLEAN;
  DECLARE v_on_hand        DECIMAL(12,2);
  DECLARE v_reserved       DECIMAL(12,2);
  DECLARE v_sum            DECIMAL(12,2);

  -- Picking header's ticket
  SELECT ph.pick_ticket_id INTO v_hdr_ticket
    FROM picking_hdr ph
   WHERE ph.picking_id = NEW.picking_id;

  IF v_hdr_ticket IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid picking_id';
  END IF;

  -- Ticket line details
  SELECT tl.pick_ticket_id, tl.product_id, tl.requested_qty
    INTO v_ticket_id, v_ticket_product, v_requested
    FROM pick_ticket_line tl
   WHERE tl.ticket_line_id = NEW.ticket_line_id;

  IF v_ticket_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid ticket_line_id';
  END IF;

  -- Same ticket as header
  IF v_ticket_id <> v_hdr_ticket THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ticket line not in this pick ticket';
  END IF;

  -- Product must match
  IF NEW.product_id <> v_ticket_product THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product mismatch vs ticket line';
  END IF;

  -- Product active + availability
  SELECT p.active_flag, p.on_hand_qty, p.reserved_qty
    INTO v_active, v_on_hand, v_reserved
    FROM products p
   WHERE p.product_id = NEW.product_id;

  IF v_active = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product is inactive';
  END IF;

  -- No over-pick across lines of this ticket_line
  SELECT COALESCE(SUM(pl.picked_qty),0)
    INTO v_sum
    FROM picking_line pl
   WHERE pl.ticket_line_id = NEW.ticket_line_id;

  IF (v_sum + NEW.picked_qty) > v_requested THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Picked qty exceeds requested';
  END IF;

  -- Availability: (on_hand - reserved) must cover NEW
  IF (v_on_hand - v_reserved) < NEW.picked_qty THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient available stock';
  END IF;
END$$

-- BEFORE UPDATE guard on picking_line
CREATE TRIGGER trg_pick_line_bu_guard
BEFORE UPDATE ON picking_line
FOR EACH ROW
BEGIN
  DECLARE v_hdr_ticket     BIGINT UNSIGNED;
  DECLARE v_ticket_id      BIGINT UNSIGNED;
  DECLARE v_ticket_product BIGINT UNSIGNED;
  DECLARE v_requested      DECIMAL(12,2);
  DECLARE v_active         BOOLEAN;
  DECLARE v_on_hand        DECIMAL(12,2);
  DECLARE v_reserved       DECIMAL(12,2);
  DECLARE v_sum            DECIMAL(12,2);
  DECLARE v_delta          DECIMAL(12,2);

  -- Prevent changing product
  IF NEW.product_id <> OLD.product_id THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Changing product is not allowed';
  END IF;

  -- Re-validate relationships
  SELECT ph.pick_ticket_id INTO v_hdr_ticket
    FROM picking_hdr ph
   WHERE ph.picking_id = NEW.picking_id;

  SELECT tl.pick_ticket_id, tl.product_id, tl.requested_qty
    INTO v_ticket_id, v_ticket_product, v_requested
    FROM pick_ticket_line tl
   WHERE tl.ticket_line_id = NEW.ticket_line_id;

  IF v_ticket_id IS NULL OR v_hdr_ticket IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid picking/ticket_line reference';
  END IF;

  IF v_ticket_id <> v_hdr_ticket THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ticket line not in this pick ticket';
  END IF;

  IF NEW.product_id <> v_ticket_product THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product mismatch vs ticket line';
  END IF;

  -- Product active
  SELECT p.active_flag, p.on_hand_qty, p.reserved_qty
    INTO v_active, v_on_hand, v_reserved
    FROM products p
   WHERE p.product_id = NEW.product_id;

  IF v_active = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product is inactive';
  END IF;

  -- No over-pick excluding this row
  SELECT COALESCE(SUM(pl.picked_qty),0)
    INTO v_sum
    FROM picking_line pl
   WHERE pl.ticket_line_id = NEW.ticket_line_id
     AND pl.picking_line_id <> OLD.picking_line_id;

  IF (v_sum + NEW.picked_qty) > v_requested THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Picked qty exceeds requested';
  END IF;

  -- Availability for positive delta
  SET v_delta = NEW.picked_qty - OLD.picked_qty;
  IF v_delta > 0 THEN
    IF (v_on_hand - v_reserved) < v_delta THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient available stock';
    END IF;
  END IF;
END$$

-- Phase E: inventory adjustments handled via services

-- Reset delimiter back to default before Phase E DDL
DELIMITER ;

-- ==========================================================
-- Phase E — Pack/Dispatch/Close (T3/T4/T5)
-- Inlined to avoid SOURCE path issues (Windows + spaces/apostrophes).
-- ==========================================================
-- Pack (T3)
USE ccinfom_dev;

CREATE TABLE IF NOT EXISTS pack_box_hdr (
  box_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  picking_id      BIGINT UNSIGNED NOT NULL,
  sealed_flag     BOOLEAN NOT NULL DEFAULT FALSE,
  seal_method     VARCHAR(50) NULL,
  sealed_at       TIMESTAMP NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_pack_box_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_pack_box_picking
    FOREIGN KEY (picking_id)
    REFERENCES picking_hdr(picking_id)
    ON DELETE CASCADE
);

CREATE INDEX idx_pack_box_ticket_id ON pack_box_hdr(pick_ticket_id);
CREATE INDEX idx_pack_box_picking_id ON pack_box_hdr(picking_id);
CREATE INDEX idx_pack_box_sealed_flag ON pack_box_hdr(sealed_flag);

CREATE TABLE IF NOT EXISTS pack_box_line (
  box_line_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  box_id          BIGINT UNSIGNED NOT NULL,
  picking_line_id BIGINT UNSIGNED NOT NULL,
  packed_qty      DECIMAL(12,2) NOT NULL CHECK (packed_qty >= 0),
  uom             VARCHAR(50) NOT NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_pack_line_hdr
    FOREIGN KEY (box_id)
    REFERENCES pack_box_hdr(box_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_pack_line_picking
    FOREIGN KEY (picking_line_id)
    REFERENCES picking_line(picking_line_id),

  CONSTRAINT uq_pack_line_picking UNIQUE (picking_line_id)
);

CREATE INDEX idx_pack_line_box_id ON pack_box_line(box_id);
CREATE INDEX idx_pack_line_picking_id ON pack_box_line(picking_line_id);

-- Dispatch (T4)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS close_variance;
DROP TABLE IF EXISTS close_hdr;
DROP TABLE IF EXISTS dispatch_line;
DROP TABLE IF EXISTS dispatch_hdr;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE dispatch_hdr (
  dispatch_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  vehicle_id      BIGINT UNSIGNED NOT NULL,
  driver_id       BIGINT UNSIGNED NOT NULL,
  manifest_no     VARCHAR(100) NOT NULL UNIQUE,
  depart_ts       TIMESTAMP NULL,
  arrive_ts       TIMESTAMP NULL,
  pod_ref         VARCHAR(100) NULL,
  pod_ts          TIMESTAMP NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_dispatch_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_dispatch_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES vehicles(vehicle_id),

  CONSTRAINT fk_dispatch_driver
    FOREIGN KEY (driver_id)
    REFERENCES employees(employee_id)
);

CREATE INDEX idx_dispatch_vehicle_id ON dispatch_hdr(vehicle_id);
CREATE INDEX idx_dispatch_ticket_id ON dispatch_hdr(pick_ticket_id);
CREATE INDEX idx_dispatch_driver_id ON dispatch_hdr(driver_id);

CREATE TABLE dispatch_line (
  dispatch_line_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  dispatch_id      BIGINT UNSIGNED NOT NULL,
  box_id           BIGINT UNSIGNED NOT NULL,
  source_ref       VARCHAR(100) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_dispatch_line_hdr
    FOREIGN KEY (dispatch_id)
    REFERENCES dispatch_hdr(dispatch_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_dispatch_line_box
    FOREIGN KEY (box_id)
    REFERENCES pack_box_hdr(box_id)
    ON DELETE CASCADE,

  CONSTRAINT uq_dispatch_line_box UNIQUE (box_id)
);

CREATE INDEX idx_dispatch_line_hdr ON dispatch_line(dispatch_id);
CREATE INDEX idx_dispatch_line_box ON dispatch_line(box_id);

-- Close (T5)
DROP TABLE IF EXISTS close_variance;
DROP TABLE IF EXISTS close_hdr;

CREATE TABLE close_hdr (
  close_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  dispatch_id     BIGINT UNSIGNED NOT NULL,
  final_status    ENUM('Delivered','Short-Closed') NOT NULL,
  pod_ref         VARCHAR(100) NULL,
  pod_ts          TIMESTAMP NULL,
  notes           VARCHAR(300) NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_close_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_close_dispatch
    FOREIGN KEY (dispatch_id)
    REFERENCES dispatch_hdr(dispatch_id)
    ON DELETE CASCADE
);

CREATE INDEX idx_close_hdr_ticket_id ON close_hdr(pick_ticket_id);
CREATE INDEX idx_close_hdr_dispatch_id ON close_hdr(dispatch_id);
CREATE INDEX idx_close_hdr_status ON close_hdr(final_status);

CREATE TABLE close_variance (
  variance_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  close_id        BIGINT UNSIGNED NOT NULL,
  ticket_line_id  BIGINT UNSIGNED NOT NULL,
  requested_qty   DECIMAL(12,2) NOT NULL,
  delivered_qty   DECIMAL(12,2) NOT NULL,
  short_qty       DECIMAL(12,2) NOT NULL,
  reason          VARCHAR(200) NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_close_variance_hdr
    FOREIGN KEY (close_id)
    REFERENCES close_hdr(close_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_close_variance_ticket_line
    FOREIGN KEY (ticket_line_id)
    REFERENCES pick_ticket_line(ticket_line_id)
    ON DELETE CASCADE,

  CONSTRAINT ck_close_variance_requested_nonneg CHECK (requested_qty >= 0),
  CONSTRAINT ck_close_variance_delivered_nonneg CHECK (delivered_qty >= 0),
  CONSTRAINT ck_close_variance_short_nonneg CHECK (short_qty >= 0)
);

CREATE INDEX idx_close_variance_hdr ON close_variance(close_id);
CREATE INDEX idx_close_variance_ticket_line ON close_variance(ticket_line_id);

DELIMITER ;
USE ccinfom_dev;
-- Phase F report views
SOURCE db/views/v_r1_daily_outcomes.sql;
SOURCE db/views/v_r2_weekly_picker_productivity.sql;
SOURCE db/views/v_r4_on_time_delivery.sql;

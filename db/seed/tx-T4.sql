USE ccinfom_dev;

-- ----------------------------------------------------------
-- 0) Clean up previous T4 seed data completely (safe)
-- ----------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
DELETE FROM dispatch_line;
DELETE FROM dispatch_hdr;
SET SQL_SAFE_UPDATES = 1;

ALTER TABLE dispatch_hdr AUTO_INCREMENT = 1;
ALTER TABLE dispatch_line AUTO_INCREMENT = 1;

-- ----------------------------------------------------------
-- 1) Identify pick ticket to dispatch (dynamic)
-- ----------------------------------------------------------
-- Pick the first available pick_ticket_id from T3
SELECT pick_ticket_id
  INTO @ticket_a
FROM pack_box_hdr
WHERE sealed_flag = 1
LIMIT 1;

-- Sanity check
SELECT @ticket_a AS pick_ticket_id;

-- =========================================================
-- [E-SEED-T4-001] Happy Path – Dispatch Manifest (Sealed Boxes)
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(@ticket_a, 1, 3, CONCAT('MANIFEST-', LPAD(1,4,'0')), NOW(), 'seed', 'seed');

SET @dispatch_id := LAST_INSERT_ID();

-- Insert only sealed boxes that haven't been dispatched yet
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'seed', 'seed'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  );

COMMIT;

-- =========================================================
-- [E-SEED-T4-002A] Exception – Vehicle Under Maintenance
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(@ticket_a, 13, 6, CONCAT('MANIFEST-MAINT-', LPAD(1,4,'0')), NOW(), 'seed', 'seed');

-- Rollback for maintenance exception
ROLLBACK;

-- =========================================================
-- [E-SEED-T4-002B] Exception – Unsealed Box
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(@ticket_a, 2, 6, CONCAT('MANIFEST-UNSEALED-', LPAD(1,4,'0')), NOW(), 'seed', 'seed');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load one unsealed box
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'seed', 'seed'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  )
LIMIT 1;

ROLLBACK;

-- =========================================================
-- [E-SEED-T4-002C] Exception – Vehicle Capacity Breach
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(@ticket_a, 1, 9, CONCAT('MANIFEST-CAPACITY-', LPAD(1,4,'0')), NOW(), 'seed', 'seed');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to insert all sealed boxes (safe, no duplicates)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'seed', 'seed'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  );

ROLLBACK;

-- =========================================================
-- [E-SEED-T4-002D] Exception – Duplicate Load (Same Box)
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(@ticket_a, 1, 11, CONCAT('MANIFEST-DUPLOAD-', LPAD(1,4,'0')), NOW(), 'seed', 'seed');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to insert one sealed box (if available)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'seed', 'seed'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  )
LIMIT 1;

-- Attempt duplicate load (will fail in reality, rolled back)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'seed', 'seed'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  )
LIMIT 1;

ROLLBACK;

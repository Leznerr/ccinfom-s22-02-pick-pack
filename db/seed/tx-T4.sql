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

-- =========================================================
-- [E-SEED-T4-001] Happy Path – Dispatch Manifest (Sealed Boxes)
-- =========================================================
START TRANSACTION;

-- Insert dispatch header for pick_ticket_id 7
INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 1, 3, 'MANIFEST-0004', NOW(), 'seed', 'seed', 'seed-T4-001');

SET @dispatch_id := LAST_INSERT_ID();

-- Insert only sealed boxes for this pick ticket (safe)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by, source_ref)
SELECT @dispatch_id, box_id, 'seed', 'seed', CONCAT('seed-T4-line-', box_id)
FROM pack_box_hdr p
WHERE pick_ticket_id = 7
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
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 13, 6, 'MANIFEST-MAINT', NOW(), 'seed', 'seed', 'seed-T4-002A');

-- Vehicle maintenance exception: rollback
ROLLBACK;

-- =========================================================
-- [E-SEED-T4-002B] Exception – Unsealed Box
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 2, 6, 'MANIFEST-UNSEALED', NOW(), 'seed', 'seed', 'seed-T4-002B');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load only unsealed boxes (safe)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by, source_ref)
SELECT @dispatch_id, box_id, 'seed', 'seed', CONCAT('seed-T4-unsealed-', box_id)
FROM pack_box_hdr p
WHERE sealed_flag = 0
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
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 1, 9, 'MANIFEST-CAPACITY', NOW(), 'seed', 'seed', 'seed-T4-002C');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load sealed boxes safely (no duplicates)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by, source_ref)
SELECT @dispatch_id, box_id, 'seed', 'seed', CONCAT('seed-T4-cap-', box_id)
FROM pack_box_hdr p
WHERE pick_ticket_id = 7
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
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 1, 11, 'MANIFEST-DUPLOAD', NOW(), 'seed', 'seed', 'seed-T4-002D');

SET @dispatch_id := LAST_INSERT_ID();

-- First insert safely
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by, source_ref)
SELECT @dispatch_id, box_id, 'seed', 'seed', CONCAT('seed-T4-dup-', box_id)
FROM pack_box_hdr p
WHERE box_id = 5
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  );

-- Attempt duplicate load (should fail) safely
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by, source_ref)
SELECT @dispatch_id, box_id, 'seed', 'seed', CONCAT('seed-T4-dup-', box_id, 'b')
FROM pack_box_hdr p
WHERE box_id = 5
  AND NOT EXISTS (
    SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  );

ROLLBACK;

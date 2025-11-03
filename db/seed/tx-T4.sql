USE ccinfom_dev;

-- ----------------------------------------------------------
-- 0) Clean up previous T4 seed data (safe and scoped)
-- ----------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
DELETE FROM dispatch_line WHERE source_ref LIKE 'seed-T4%';
DELETE FROM dispatch_hdr WHERE source_ref LIKE 'seed-T4%';
SET SQL_SAFE_UPDATES = 1;

-- =========================================================
-- [E-SEED-T4-001] Happy Path – Dispatch Manifest (Sealed Boxes)
-- =========================================================
START TRANSACTION;

-- INSERT INTO dispatch_hdr
-- (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
-- VALUES
-- (7, 1, 3, 'MANIFEST-0003', NOW(), 'seed', 'seed');

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by, source_ref)
VALUES
(7, 1, 3, 'MANIFEST-0001', NOW(), 'seed', 'seed', 'seed-T4-001');


INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES
(LAST_INSERT_ID(), 1, 'seed', 'seed'),
(LAST_INSERT_ID(), 2, 'seed', 'seed');

COMMIT;

-- =========================================================
-- [E-SEED-T4-002A] Exception – Vehicle Under Maintenance
-- =========================================================
START TRANSACTION;

INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(2, 13, 6, 'MANIFEST-MAINT', NOW(), 'seed', 'seed');

-- Should be rolled back due to VEHICLE_UNDER_MAINTENANCE
ROLLBACK;


-- =========================================================
-- [E-SEED-T4-002B] Exception – Unsealed Box
-- =========================================================
START TRANSACTION;

-- Create header with available vehicle and driver
INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(2, 2, 6, 'MANIFEST-UNSEALED', NOW(), 'seed', 'seed');

-- Attempt to load unsealed box (sealed_flag = 0, e.g., box_id 3)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES
(LAST_INSERT_ID(), 3, 'seed', 'seed');

-- Should be rolled back due to DISPATCH_BOX_NOT_SEALED
ROLLBACK;


-- =========================================================
-- [E-SEED-T4-002C] Exception – Vehicle Capacity Breach
-- =========================================================
START TRANSACTION;

-- Vehicle ID 2 has capacity 5.00; attempt to load > capacity (6 boxes)
INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(3, 2, 9, 'MANIFEST-CAPACITY', NOW(), 'seed', 'seed');

-- Simulate overload (6 boxes)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES
(LAST_INSERT_ID(), 4, 'seed', 'seed'),
(LAST_INSERT_ID(), 5, 'seed', 'seed'),
(LAST_INSERT_ID(), 6, 'seed', 'seed'),
(LAST_INSERT_ID(), 7, 'seed', 'seed'),
(LAST_INSERT_ID(), 8, 'seed', 'seed'),
(LAST_INSERT_ID(), 9, 'seed', 'seed');

ROLLBACK;


-- =========================================================
-- [E-SEED-T4-002D] Exception – Duplicate Load (Same Box)
-- =========================================================
START TRANSACTION;

-- First create valid dispatch header
INSERT INTO dispatch_hdr
(pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES
(3, 4, 11, 'MANIFEST-DUPLOAD', NOW(), 'seed', 'seed');

-- Attempt to load same box twice
SET @dispatch_id := LAST_INSERT_ID();

INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES
(@dispatch_id, 10, 'seed', 'seed');

-- Duplicate load -> violates UNIQUE(box_id)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES
(@dispatch_id, 10, 'seed', 'seed');

ROLLBACK;

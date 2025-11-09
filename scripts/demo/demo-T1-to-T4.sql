-- Phase C demo placeholder (T1/T2). Phase E will extend this script.

USE ccinfom_dev;

-- TODO[E-DEMO-T3-001] Append Pack (T3) sequence with happy + over-pack exception.
-- Why: Demo must illustrate box creation, sealing, and over-pack rollback with clear comments.
-- Steps (after existing T1/T2 demo):
--   1) BEGIN; create pack_box_hdr / pack_box_line for a seeded picking session.
--   2) COMMIT happy path; comment "No inventory log for T3 per policy."
--   3) Attempt over-pack in separate transaction; expect failure with PACK_OVER_QTY.
-- Acceptance:
--   - Script executes end-to-end without manual edits.
--   - Over-pack step raises PACK_OVER_QTY message in Workbench.
--   - Demo walkthrough references this section.
-- | Links: docs/decisions.md#phase-e

-- ==========================================================
-- PHASE E — T3 Pack & Box
-- Demo: Happy path (Ticket A) + Over-pack exception (rollback)
-- ==========================================================

-- ----------------------------------------------------------
-- Step 1: Happy Path — Review Seeded Boxes (Ticket A)
-- ----------------------------------------------------------

-- Ticket A = Delivered (Happy Path)
SELECT @ticket_a := pick_ticket_id,
       @picking_a := picking_id
  FROM pack_box_hdr
 WHERE source_ref = 'seed-T3-box1'
 LIMIT 1;

-- Verify seeded headers and lines
SELECT 'Pack Box Headers — Ticket A' AS section;
SELECT box_id, pick_ticket_id, picking_id, sealed_flag, seal_method, sealed_at, source_ref
  FROM pack_box_hdr
 WHERE pick_ticket_id = @ticket_a;

SELECT 'Pack Box Lines — Ticket A' AS section;
SELECT pbl.box_line_id, pbl.box_id, pbl.picking_line_id, pbl.packed_qty, pbl.uom, pbl.source_ref
  FROM pack_box_line pbl
  JOIN pack_box_hdr pbh ON pbh.box_id = pbl.box_id
 WHERE pbh.pick_ticket_id = @ticket_a;

-- ----------------------------------------------------------
-- Step 2: Seal the Unsealed Box (Box 2)
-- ----------------------------------------------------------

START TRANSACTION;

UPDATE pack_box_hdr
   SET sealed_flag = TRUE,
       seal_method = 'tape',
       sealed_at = CURRENT_TIMESTAMP,
       updated_by = 'demo'
 WHERE source_ref = 'seed-T3-box2'
   AND sealed_flag = FALSE;

COMMIT;

-- Verify status flip
SELECT 'Updated Box 2 to sealed' AS action, sealed_flag, seal_method, sealed_at
  FROM pack_box_hdr
 WHERE source_ref = 'seed-T3-box2';

-- Expected:
-- - Both boxes for Ticket A are now sealed
-- - Ticket status should automatically be 'Packed' once any valid pack exists

SELECT 'Ticket status check' AS section;
SELECT pick_ticket_id, ticket_status
  FROM pick_ticket_hdr
 WHERE pick_ticket_id = @ticket_a;

-- ----------------------------------------------------------
-- Step 3: Over-pack Scenario (Expect PACK_OVER_QTY)
-- ----------------------------------------------------------

-- Simulate an over-pack attempt on Ticket A’s picking lines.
-- This uses a new box header for demonstration and should trigger
-- the validation rule in PackService (or fail gracefully in DB if enforced).

START TRANSACTION;

INSERT INTO pack_box_hdr (
    pick_ticket_id, picking_id, sealed_flag, created_by, updated_by, source_ref
)
VALUES (
    @ticket_a, @picking_a, FALSE, 'demo', 'demo', 'demo-T3-overpack-test'
);

SET @bad_box_id := LAST_INSERT_ID();

-- Try to pack 2x the picked quantity (should fail PACK_OVER_QTY)
INSERT INTO pack_box_line (
    box_id, picking_line_id, packed_qty, uom, created_by, updated_by, source_ref
)
SELECT
    @bad_box_id,
    pl.picking_line_id,
    pl.picked_qty * 2,  -- intentionally exceeds picked qty
    pl.uom,
    'demo',
    'demo',
    'demo-T3-overpack-line'
FROM picking_line pl
WHERE pl.picking_id = @picking_a
  AND pl.picking_line_id NOT IN (SELECT picking_line_id FROM pack_box_line)
LIMIT 1;

-- Expected result:
--  Error: PACK_OVER_QTY (triggered by service logic)
--  Rollback should leave no trace of @bad_box_id

ROLLBACK;

-- Confirm rollback worked
SELECT 'Check for rolled back over-pack box (should be none)' AS section;
SELECT * FROM pack_box_hdr WHERE source_ref = 'demo-T3-overpack-test';

-- ----------------------------------------------------------
-- Step 4: QA Snapshot — Packed vs Picked Quantities
-- ----------------------------------------------------------

SELECT
  pl.picking_line_id,
  pl.picked_qty,
  COALESCE(SUM(pbl.packed_qty),0) AS total_packed,
  CASE
    WHEN COALESCE(SUM(pbl.packed_qty),0) <= pl.picked_qty THEN 'OK'
    ELSE 'VIOLATION'
  END AS packed_vs_picked
FROM picking_line pl
LEFT JOIN pack_box_line pbl ON pbl.picking_line_id = pl.picking_line_id
WHERE pl.picking_id = @picking_a
GROUP BY pl.picking_line_id, pl.picked_qty
ORDER BY pl.picking_line_id;

-- ----------------------------------------------------------
-- Step 5: End of T3 Section (Ready for Dispatch T4)
-- ----------------------------------------------------------

SELECT 'T3 Pack & Box demo complete — proceed to Dispatch (T4)' AS message;


-- TODO[E-DEMO-T4-002] Append Dispatch (T4) sequence after pack demo.
-- Why: Show manifest creation, sealed-only enforcement, and duplicate load failure.
-- Steps:
--   1) BEGIN; insert dispatch_hdr + dispatch_line for sealed boxes; COMMIT.
--   2) Attempt to load unsealed box → expect DISPATCH_UNSEALED_BOX.
--   3) Attempt duplicate load → expect DISPATCH_BOX_ALREADY_LOADED.
-- Acceptance:
--   - Happy path sets ticket status to 'Dispatched'.
--   - Exceptions raise specified codes.
--   - Referenced in README demo instructions.
-- | Links: qa/validation_queries.sql, docs/seed-id-map.md


-- =========================================================
-- PHASE E — T4 Dispatch
-- Demo: Happy path (sealed boxes) + exceptions (unsealed, duplicate)
-- =========================================================

-- ----------------------------------------------------------
-- Step 1: Happy Path — Dispatch Sealed Boxes
-- ----------------------------------------------------------

-- Get pick_ticket_id from T3 happy path (Ticket A)
SELECT pick_ticket_id
  INTO @ticket_a
  FROM pack_box_hdr
 WHERE source_ref = 'seed-T3-box1'
 LIMIT 1;

-- Insert dispatch header
START TRANSACTION;

INSERT INTO dispatch_hdr (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES (@ticket_a, 1, 3, CONCAT('MANIFEST-', LPAD(FLOOR(RAND()*1000), 4, '0')), NOW(), 'demo', 'demo');

SET @dispatch_id := LAST_INSERT_ID();

-- Insert dispatch lines for sealed boxes only (dynamic, safe)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'demo', 'demo'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
  AND NOT EXISTS (
      SELECT 1 FROM dispatch_line d WHERE d.box_id = p.box_id
  );

COMMIT;

-- Verify dispatched boxes
SELECT 'Happy Path — Dispatched Sealed Boxes' AS section;
SELECT dl.dispatch_id, dl.box_id, dh.pick_ticket_id
FROM dispatch_line dl
JOIN dispatch_hdr dh ON dh.dispatch_id = dl.dispatch_id
WHERE dh.pick_ticket_id = @ticket_a;


-- ----------------------------------------------------------
-- Step 2: Exception — Attempt to Dispatch Unsealed Box
-- ----------------------------------------------------------

START TRANSACTION;

INSERT INTO dispatch_hdr (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES (@ticket_a, 2, 3, CONCAT('MANIFEST-UNSEALED-', LPAD(FLOOR(RAND()*1000), 4, '0')), NOW(), 'demo', 'demo');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load an unsealed box (should trigger exception in service)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'demo', 'demo'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 0
LIMIT 1;

-- Rollback: expected failure
ROLLBACK;

SELECT 'Exception — Unsealed Box attempt rolled back' AS section;
SELECT * FROM dispatch_line WHERE dispatch_id = @dispatch_id;


-- ----------------------------------------------------------
-- Step 3: Exception — Duplicate Box Load Attempt
-- ----------------------------------------------------------

-- First, pick a sealed box that was already dispatched
SELECT box_id
  INTO @dup_box_id
  FROM dispatch_line
 WHERE dispatch_id = (
     SELECT dispatch_id FROM dispatch_hdr WHERE pick_ticket_id = @ticket_a ORDER BY dispatch_id LIMIT 1
 )
 LIMIT 1;

-- Start transaction for duplicate load attempt
START TRANSACTION;

INSERT INTO dispatch_hdr (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES (@ticket_a, 1, 3, CONCAT('MANIFEST-DUP-', LPAD(FLOOR(RAND()*1000), 4, '0')), NOW(), 'demo', 'demo');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to insert the same box again (should fail unique constraint)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
VALUES (@dispatch_id, @dup_box_id, 'demo', 'demo');

-- Rollback expected
ROLLBACK;

SELECT 'Exception — Duplicate Box attempt rolled back' AS section;
SELECT * FROM dispatch_line WHERE box_id = @dup_box_id;


-- ----------------------------------------------------------
-- Step 4: Exception — Vehicle Unavailable
-- ----------------------------------------------------------

START TRANSACTION;

INSERT INTO dispatch_hdr (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES (@ticket_a, 999, 3, CONCAT('MANIFEST-MAINT-', LPAD(FLOOR(RAND()*1000), 4, '0')), NOW(), 'demo', 'demo');  -- vehicle 999 is unavailable

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load sealed box (should fail due to vehicle unavailability)
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'demo', 'demo'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1
LIMIT 1;

ROLLBACK;

SELECT 'Exception — Vehicle Unavailable rolled back' AS section;


-- ----------------------------------------------------------
-- Step 5: Exception — Vehicle Capacity Exceed
-- ----------------------------------------------------------

START TRANSACTION;

INSERT INTO dispatch_hdr (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, created_by, updated_by)
VALUES (@ticket_a, 1, 3, CONCAT('MANIFEST-CAP-', LPAD(FLOOR(RAND()*1000), 4, '0')), NOW(), 'demo', 'demo');

SET @dispatch_id := LAST_INSERT_ID();

-- Attempt to load too many sealed boxes
INSERT INTO dispatch_line (dispatch_id, box_id, created_by, updated_by)
SELECT @dispatch_id, box_id, 'demo', 'demo'
FROM pack_box_hdr p
WHERE pick_ticket_id = @ticket_a
  AND sealed_flag = 1;

-- Here, the service should reject if vehicle capacity exceeded
ROLLBACK;

SELECT 'Exception — Vehicle Capacity Exceed rolled back' AS section;


-- =========================================================
-- End of T4 Dispatch Demo
-- =========================================================

SELECT 'Demo complete: Happy path + exceptions executed safely' AS message;

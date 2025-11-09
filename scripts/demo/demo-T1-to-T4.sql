-- Phase C demo placeholder (T1/T2). Phase E will extend this script.

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

USE ccinfom_dev;

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


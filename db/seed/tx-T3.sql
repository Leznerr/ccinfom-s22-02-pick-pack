-- Phase E Seeds: T3 Pack & Box

-- TODO[E-SEED-T3-001] Insert happy path pack records referencing seeded picking_line IDs.
-- Steps:
--   1) Ensure prerequisite T1/T2 seeds created pick_ticket_id/picking_line_id (see docs/seed-id-map.md).
--   2) Insert pack_box_hdr rows (unsealed and sealed examples).
--   3) Insert pack_box_line rows with packed_qty <= picked_qty.
-- Acceptance: Script executes on clean DB; QA packed_vs_picked query shows zero violations.
-- | Links: docs/seed-id-map.md

-- TODO[E-SEED-T3-002] Insert exception scenarios (over-pack, inactive product, seal missing).
-- Steps:
--   1) Attempt over-pack and wrap in transaction with ROLLBACK + comment referencing PACK_OVER_QTY.
--   2) Include sample unsealed box entry for dispatch demo (seal flag 0).
-- Acceptance: Demo script references these cases; QA can reproduce failures manually.
-- 

USE ccinfom_dev;

-- ----------------------------------------------------------
-- 0) Clean up previous seed data (safe and scoped)
-- ----------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
DELETE FROM pack_box_line WHERE source_ref LIKE 'seed-T3%';
DELETE FROM pack_box_hdr WHERE source_ref LIKE 'seed-T3%';
SET SQL_SAFE_UPDATES = 1;


START TRANSACTION;

-- ----------------------------------------------------------
-- 1) Identify picking session (Ticket A = fully picked)
-- ----------------------------------------------------------
SELECT MIN(picking_id)
  INTO @picking_a
  FROM picking_hdr
 WHERE picking_status = 'Done';

SELECT pick_ticket_id
  INTO @ticket_a
  FROM picking_hdr
 WHERE picking_id = @picking_a;

-- Sanity check
SELECT @ticket_a AS pick_ticket_id, @picking_a AS picking_id;


-- ----------------------------------------------------------
-- 2) Create Pack Box Headers
-- ----------------------------------------------------------

-- Box 1 — Sealed (Happy path)
INSERT INTO pack_box_hdr (
    pick_ticket_id,
    picking_id,
    sealed_flag,
    seal_method,
    sealed_at,
    created_by,
    updated_by,
    source_ref
)
VALUES (
    @ticket_a,
    @picking_a,
    TRUE,
    'tape',
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T3-box1'
);

SET @box_a1 := LAST_INSERT_ID();

-- Box 2 — Unsealed (for later Dispatch test)
INSERT INTO pack_box_hdr (
    pick_ticket_id,
    picking_id,
    sealed_flag,
    created_by,
    updated_by,
    source_ref
)
VALUES (
    @ticket_a,
    @picking_a,
    FALSE,
    'seed',
    'seed',
    'seed-T3-box2'
);

SET @box_a2 := LAST_INSERT_ID();


-- ----------------------------------------------------------
-- 3) Insert Pack Box Lines (Happy Path)
-- ----------------------------------------------------------

-- Get at least two picking lines from Ticket A
DROP TEMPORARY TABLE IF EXISTS tmp_pick_lines;
CREATE TEMPORARY TABLE tmp_pick_lines AS
SELECT picking_line_id, picking_id, product_id, picked_qty, uom
  FROM picking_line
 WHERE picking_id = @picking_a
 LIMIT 2;

-- Box 1 line — full packed (<= picked)
INSERT INTO pack_box_line (
    box_id,
    picking_line_id,
    packed_qty,
    uom,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @box_a1,
    pl.picking_line_id,
    pl.picked_qty,
    pl.uom,
    'seed',
    'seed',
    'seed-T3-line1'
FROM tmp_pick_lines pl
LIMIT 1;

-- Box 2 line — half packed (<= picked)
INSERT INTO pack_box_line (
    box_id,
    picking_line_id,
    packed_qty,
    uom,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @box_a2,
    pl.picking_line_id,
    ROUND(pl.picked_qty * 0.5, 2),
    pl.uom,
    'seed',
    'seed',
    'seed-T3-line2'
FROM tmp_pick_lines pl
LIMIT 1 OFFSET 1;


-- ----------------------------------------------------------
-- 4) Exception: Over-Pack Scenario (Expect to Fail)
-- ----------------------------------------------------------

BEGIN;
INSERT INTO pack_box_hdr (
    pick_ticket_id,
    picking_id,
    sealed_flag,
    created_by,
    updated_by,
    source_ref
)
VALUES (
    @ticket_a,
    @picking_a,
    TRUE,
    'seed',
    'seed',
    'seed-T3-overpack-hdr'
);

SET @bad_box := LAST_INSERT_ID();

-- Try to pack more than picked (should fail business logic)
INSERT INTO pack_box_line (
    box_id,
    picking_line_id,
    packed_qty,
    uom,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @bad_box,
    pl.picking_line_id,
    pl.picked_qty + 5,
    pl.uom,
    'seed',
    'seed',
    'seed-T3-overpack-line'
FROM picking_line pl
WHERE pl.picking_id = @picking_a
  AND pl.picking_line_id NOT IN (
      SELECT picking_line_id FROM pack_box_line
  )
LIMIT 1;


-- Expect PACK_OVER_QTY in service test (DB will allow insert for now)
ROLLBACK;


-- ----------------------------------------------------------
-- 4b) Exception: Inactive Product Scenario (Expect to Fail)
-- ----------------------------------------------------------

BEGIN;
INSERT INTO pack_box_hdr (
    pick_ticket_id,
    picking_id,
    sealed_flag,
    created_by,
    updated_by,
    source_ref
)
VALUES (
    @ticket_a,
    @picking_a,
    TRUE,
    'seed',
    'seed',
    'seed-T3-inactive-hdr'
);

SET @inactive_box := LAST_INSERT_ID();

-- Try to pack inactive product (simulated; rolled back)
INSERT INTO pack_box_line (
    box_id,
    picking_line_id,
    packed_qty,
    uom,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @inactive_box,
    pl.picking_line_id,
    pl.picked_qty,
    pl.uom,
    'seed',
    'seed',
    'seed-T3-inactive-line'
FROM picking_line pl
JOIN products p ON pl.product_id = p.product_id
WHERE p.active_flag = 0
LIMIT 1;

-- Expect PRODUCT_INACTIVE in service test (DB will allow insert for now)
ROLLBACK;


-- ----------------------------------------------------------
-- 5) Cleanup and verify
-- ----------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_pick_lines;

COMMIT;


-- ----------------------------------------------------------
-- 6) Validation Check (for reference)
-- ----------------------------------------------------------
SELECT 'pack_box_hdr' AS table_name, COUNT(*) AS row_count FROM pack_box_hdr;
SELECT 'pack_box_line' AS table_name, COUNT(*) AS row_count FROM pack_box_line;

SELECT * FROM pack_box_hdr;
SELECT * FROM pack_box_line;

-- Expected:
--   2 rows in pack_box_hdr (1 sealed, 1 unsealed)
--   2 rows in pack_box_line (1 per box)
--   Over-pack and inactive-product rolled back (no insert persisted)
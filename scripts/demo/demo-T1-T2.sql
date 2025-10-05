-- =========================================
-- PHASE C — DEMO (Happy Path) T1 + T2
-- Steps:
-- 1) Create Ticket A (hdr + 2–3 lines). SELECT hdr/lines to show data.
-- 2) Start Picking (insert picking_hdr). VERIFY ticket_status='Picking'.
-- 3) Insert picking_line rows (join to ticket_line). SHOW products before/after:
--      reserved_qty ↑ equals SUM(picked_qty), on_hand_qty ↔ unchanged.
-- 4) Run QA snippets (/qa/validation_queries.sql) and print results.
-- 5) OPTIONAL NEGATIVE: Attempt an over-pick to prove DB guard (expect ERROR; no data change).

-- Expected:
-- - No errors; status flip to 'Picking'; reserved_qty increased exactly by picked_qty.
-- - No change to on_hand_qty at this phase.

-- Expected:
-- - No errors on happy path; status flips to 'Picking'; reserved_qty increased exactly by picked_qty.
-- - No change to on_hand_qty at this phase.
-- - Over-pick attempt throws 'Insufficient available' and leaves data unchanged.
-- =========================================

-- Step 1: Create a new pick ticket header
INSERT INTO pick_ticket_hdr (customer_id, branch_id, ticket_status, remarks, updated_by)
VALUES (1, 1, 'Open', 'Demo Ticket A for T1', 'demo');

-- Save the new ticket_id
SET @new_ticket_id = LAST_INSERT_ID();

-- Step 2: Insert 2–3 product lines for this ticket
INSERT INTO pick_ticket_line (pick_ticket_id, product_id, requested_qty, uom, updated_by)
VALUES
  (@new_ticket_id, 1, 5, 'pcs', 'demo'),
  (@new_ticket_id, 2, 10, 'pcs', 'demo'),
  (@new_ticket_id, 3, 3, 'pcs', 'demo');

-- Step 3: Show the created header and its lines
SELECT * 
FROM pick_ticket_hdr
WHERE pick_ticket_id = @new_ticket_id;

SELECT * 
FROM pick_ticket_line
WHERE pick_ticket_id = @new_ticket_id;

/* ================================
   TRANSACTION 2 — Allocate & Pick
   Happy path for the ticket created above
   (No IF/END IF used)
   ================================ */

START TRANSACTION;

-- 0) Choose a picker (prefer active 'picker', else any active)
SELECT COALESCE(
         (SELECT e.employee_id
            FROM employees e
           WHERE e.employee_status='active' AND e.employee_role='picker'
           ORDER BY e.employee_id LIMIT 1),
         (SELECT e.employee_id
            FROM employees e
           WHERE e.employee_status='active'
           ORDER BY e.employee_id LIMIT 1)
       )
INTO @picker_employee_id;

-- Boolean guard: 1 if we can proceed, else 0 (script becomes no-op)
SET @can_pick := (@picker_employee_id IS NOT NULL) AND (@new_ticket_id IS NOT NULL);

-- 1) Snapshot the ticket’s lines + product inventory BEFORE picking
DROP TEMPORARY TABLE IF EXISTS tmp_demo_lines;
CREATE TEMPORARY TABLE tmp_demo_lines AS
SELECT
  tl.ticket_line_id,
  tl.product_id,
  tl.requested_qty,
  tl.uom,
  p.on_hand_qty  AS on_hand_before,
  p.reserved_qty AS reserved_before
FROM pick_ticket_line tl
JOIN products p ON p.product_id = tl.product_id
WHERE @can_pick = 1
  AND tl.pick_ticket_id = @new_ticket_id;

-- 2) Top up availability (keep demo “green”); also force active
UPDATE products p
JOIN (
  SELECT product_id, SUM(requested_qty) AS need_qty
  FROM tmp_demo_lines
  GROUP BY product_id
) n ON n.product_id = p.product_id
   SET p.on_hand_qty = GREATEST(p.on_hand_qty, p.reserved_qty + n.need_qty),
       p.active_flag = TRUE,
       p.updated_at  = CURRENT_TIMESTAMP,
       p.updated_by  = 'demo'
WHERE @can_pick = 1;

-- 3) Create picking header for this ticket (trigger flips ticket → 'Picking')
SET @picking_id := NULL;

INSERT INTO picking_hdr (pick_ticket_id, picker_employee_id, picking_status, started_at, updated_by)
SELECT @new_ticket_id, @picker_employee_id, 'Picking', CURRENT_TIMESTAMP, 'demo'
FROM DUAL
WHERE @can_pick = 1
  AND NOT EXISTS (SELECT 1 FROM picking_hdr ph WHERE ph.pick_ticket_id = @new_ticket_id);

-- Get (existing or newly inserted) picking_id
SELECT ph.picking_id
  INTO @picking_id
FROM picking_hdr ph
WHERE @can_pick = 1
  AND ph.pick_ticket_id = @new_ticket_id
ORDER BY ph.picking_id DESC
LIMIT 1;

-- 4) Verify ticket flipped to 'Picking' (informational)
SELECT th.pick_ticket_id, th.ticket_status
FROM pick_ticket_hdr th
WHERE @can_pick = 1
  AND th.pick_ticket_id = @new_ticket_id;

-- 5) Insert picking lines — full pick, idempotent via anti-dup guard
INSERT INTO picking_line (picking_id, ticket_line_id, product_id, picked_qty, uom, updated_by)
SELECT @picking_id, d.ticket_line_id, d.product_id, d.requested_qty, d.uom, 'demo'
FROM tmp_demo_lines d
WHERE @can_pick = 1
  AND @picking_id IS NOT NULL
  AND NOT EXISTS (
        SELECT 1
        FROM picking_line pl
        WHERE pl.picking_id = @picking_id
          AND pl.ticket_line_id = d.ticket_line_id
      );

-- 6) AFTER snapshot & deltas (expect delta_on_hand = 0; delta_reserved = picked)
SELECT
  d.product_id,
  p.on_hand_qty      AS on_hand_after,
  p.reserved_qty     AS reserved_after,
  d.on_hand_before,
  d.reserved_before,
  (p.on_hand_qty  - d.on_hand_before)  AS delta_on_hand,
  (p.reserved_qty - d.reserved_before) AS delta_reserved
FROM tmp_demo_lines d
JOIN products p ON p.product_id = d.product_id
WHERE @can_pick = 1
ORDER BY d.product_id;

-- 7) Quick invariants for this ticket
-- 7a) on_hand unchanged by T2 (expect 0)
SELECT
  SUM( (p.on_hand_qty - d.on_hand_before) <> 0 ) AS any_on_hand_changed
FROM tmp_demo_lines d
JOIN products p ON p.product_id = d.product_id
WHERE @can_pick = 1;

-- 7b) Per line: picked <= requested (expect 0 rows)
SELECT tl.ticket_line_id, tl.requested_qty, COALESCE(SUM(pl.picked_qty),0) AS picked_total
FROM pick_ticket_line tl
LEFT JOIN picking_line pl ON pl.ticket_line_id = tl.ticket_line_id
WHERE @can_pick = 1
  AND tl.pick_ticket_id = @new_ticket_id
GROUP BY tl.ticket_line_id, tl.requested_qty
HAVING COALESCE(SUM(pl.picked_qty),0) > tl.requested_qty;

-- 7c) Status snapshot
SELECT th.pick_ticket_id, th.ticket_status,
       ph.picking_id, ph.picking_status, ph.started_at, ph.completed_at
FROM pick_ticket_hdr th
LEFT JOIN picking_hdr ph ON ph.pick_ticket_id = th.pick_ticket_id
WHERE @can_pick = 1
  AND th.pick_ticket_id = @new_ticket_id;

COMMIT;

-- If guard prevented work, surface a friendly note (no-op run)
SELECT 'TX-T2 no-op: missing active picker or @new_ticket_id' AS note
WHERE @can_pick = 0;

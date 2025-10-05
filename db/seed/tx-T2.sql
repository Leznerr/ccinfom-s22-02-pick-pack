/* ==========================================
   PHASE C — SEEDS FOR T2 (Allocate & Pick)
   Goal: 1 full pick for Ticket A + 1 partial pick for Ticket B.
   Properties: Idempotent (safe to re-run), no failing inserts.
========================================== */
USE ccinfom_dev;

START TRANSACTION;

-- ----------------------------------------------------------
-- 0) Choose a picker (prefer active 'picker', else any active)
--    If none exists, the guarded WHERE clauses below will no-op.
-- ----------------------------------------------------------
SELECT COALESCE(
         (SELECT e.employee_id
            FROM employees e
           WHERE e.employee_status = 'active'
             AND e.employee_role   = 'picker'
           ORDER BY e.employee_id
           LIMIT 1),
         (SELECT e.employee_id
            FROM employees e
           WHERE e.employee_status = 'active'
           ORDER BY e.employee_id
           LIMIT 1)
       )
INTO @picker_employee_id;

-- ----------------------------------------------------------
-- 1) Choose two different pick tickets that have lines
--    (A = earliest with lines; B = next with lines)
-- ----------------------------------------------------------
SELECT h.pick_ticket_id
  INTO @ticket_a
  FROM pick_ticket_hdr h
 WHERE EXISTS (SELECT 1
                 FROM pick_ticket_line l
                WHERE l.pick_ticket_id = h.pick_ticket_id)
 ORDER BY h.pick_ticket_id
 LIMIT 1;

SELECT h.pick_ticket_id
  INTO @ticket_b
  FROM pick_ticket_hdr h
 WHERE h.pick_ticket_id <> @ticket_a
   AND EXISTS (SELECT 1
                 FROM pick_ticket_line l
                WHERE l.pick_ticket_id = h.pick_ticket_id)
 ORDER BY h.pick_ticket_id
 LIMIT 1;

-- ----------------------------------------------------------
-- 2) Materialize ticket lines (zero rows if ticket is NULL)
-- ----------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_t1_a;
DROP TEMPORARY TABLE IF EXISTS tmp_t1_b;

CREATE TEMPORARY TABLE tmp_t1_a AS
SELECT t1.ticket_line_id, t1.pick_ticket_id, t1.product_id, t1.requested_qty, t1.uom
  FROM pick_ticket_line t1
 WHERE @ticket_a IS NOT NULL
   AND t1.pick_ticket_id = @ticket_a;

CREATE TEMPORARY TABLE tmp_t1_b AS
SELECT t1.ticket_line_id, t1.pick_ticket_id, t1.product_id, t1.requested_qty, t1.uom
  FROM pick_ticket_line t1
 WHERE @ticket_b IS NOT NULL
   AND t1.pick_ticket_id = @ticket_b;

-- ----------------------------------------------------------
-- 3) Compute availability needed per product:
--    - Full for A
--    - ~50% for B (min 0.01 to avoid zeros)
-- ----------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_needed;
CREATE TEMPORARY TABLE tmp_needed AS
SELECT product_id, SUM(need_qty) AS need_qty
FROM (
  SELECT product_id, requested_qty AS need_qty FROM tmp_t1_a
  UNION ALL
  SELECT product_id, GREATEST(0.01, ROUND(requested_qty * 0.50, 2)) AS need_qty FROM tmp_t1_b
) t
GROUP BY product_id;

-- ----------------------------------------------------------
-- 4) Ensure products can cover the planned picks
--    available = on_hand_qty - reserved_qty
--    Only do this when a picker exists (otherwise no-op)
-- ----------------------------------------------------------
SET @can_pick := (@picker_employee_id IS NOT NULL);

UPDATE products p
JOIN tmp_needed n
  ON n.product_id = p.product_id
   SET p.on_hand_qty = GREATEST(p.on_hand_qty, p.reserved_qty + n.need_qty),
       p.active_flag = TRUE,
       p.updated_at  = CURRENT_TIMESTAMP,
       p.updated_by  = 'seed'
 WHERE @can_pick = 1;

-- ----------------------------------------------------------
-- 5) Create picking headers only if absent (idempotent),
--    then fetch @picking_a / @picking_b
--    Note: AFTER INSERT trigger flips ticket to 'Picking'.
-- ----------------------------------------------------------

-- Ticket A
SET @picking_a := NULL;
INSERT INTO picking_hdr (pick_ticket_id, picker_employee_id, picking_status, started_at, updated_by)
SELECT @ticket_a, @picker_employee_id, 'Picking', CURRENT_TIMESTAMP, 'seed'
FROM DUAL
WHERE @ticket_a IS NOT NULL
  AND @picker_employee_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM picking_hdr ph WHERE ph.pick_ticket_id = @ticket_a);

SELECT ph.picking_id
  INTO @picking_a
  FROM picking_hdr ph
 WHERE ph.pick_ticket_id = @ticket_a
 ORDER BY ph.picking_id DESC
 LIMIT 1;

-- Ticket B
SET @picking_b := NULL;
INSERT INTO picking_hdr (pick_ticket_id, picker_employee_id, picking_status, started_at, updated_by)
SELECT @ticket_b, @picker_employee_id, 'Picking', CURRENT_TIMESTAMP, 'seed'
FROM DUAL
WHERE @ticket_b IS NOT NULL
  AND @picker_employee_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM picking_hdr ph WHERE ph.pick_ticket_id = @ticket_b);

SELECT ph.picking_id
  INTO @picking_b
  FROM picking_hdr ph
 WHERE ph.pick_ticket_id = @ticket_b
 ORDER BY ph.picking_id DESC
 LIMIT 1;

-- ----------------------------------------------------------
-- 6) Insert picking lines
--    - A = full requested
--    - B = ~50% requested
--    Anti-dup guard uses (picking_id, ticket_line_id)
-- ----------------------------------------------------------

-- Lines for A (full)
INSERT INTO picking_line (picking_id, ticket_line_id, product_id, picked_qty, uom, updated_by)
SELECT @picking_a, a.ticket_line_id, a.product_id, a.requested_qty, a.uom, 'seed'
  FROM tmp_t1_a a
 WHERE @picking_a IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM picking_line pl
          WHERE pl.picking_id     = @picking_a
            AND pl.ticket_line_id = a.ticket_line_id
       );

-- Lines for B (~50%)
INSERT INTO picking_line (picking_id, ticket_line_id, product_id, picked_qty, uom, updated_by)
SELECT @picking_b, b.ticket_line_id, b.product_id,
       GREATEST(0.01, ROUND(b.requested_qty * 0.50, 2)), b.uom, 'seed'
  FROM tmp_t1_b b
 WHERE @picking_b IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM picking_line pl
          WHERE pl.picking_id     = @picking_b
            AND pl.ticket_line_id = b.ticket_line_id
       );

-- ----------------------------------------------------------
-- 7) If Ticket A fully satisfied, mark its picking as 'Done'
--    Leave B as 'Picking' (by design for demo)
-- ----------------------------------------------------------
UPDATE picking_hdr ph
JOIN (
  SELECT a.pick_ticket_id,
         SUM(a.requested_qty) AS req,
         SUM(pl.picked_qty)   AS got
    FROM tmp_t1_a a
    JOIN picking_line pl
      ON pl.ticket_line_id = a.ticket_line_id
   WHERE pl.picking_id = @picking_a
   GROUP BY a.pick_ticket_id
) x
  ON x.pick_ticket_id = ph.pick_ticket_id
   SET ph.picking_status = CASE WHEN x.req = x.got THEN 'Done' ELSE ph.picking_status END,
       ph.completed_at   = CASE WHEN x.req = x.got THEN CURRENT_TIMESTAMP ELSE ph.completed_at END,
       ph.updated_at     = CURRENT_TIMESTAMP,
       ph.updated_by     = 'seed'
 WHERE @picking_a IS NOT NULL
   AND ph.picking_id = @picking_a;

COMMIT;

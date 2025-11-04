USE ccinfom_dev;

SET SQL_SAFE_UPDATES = 0;
DELETE FROM close_variance WHERE source_ref LIKE 'seed-T5%';
DELETE FROM close_hdr      WHERE source_ref LIKE 'seed-T5%';
SET SQL_SAFE_UPDATES = 1;

START TRANSACTION;

SELECT dispatch_id,
       pick_ticket_id
  INTO @dispatch_delivered,
       @ticket_delivered
  FROM dispatch_hdr
 WHERE source_ref = 'seed-T4-hdr-001'
 LIMIT 1;

SELECT dispatch_id,
       pick_ticket_id
  INTO @dispatch_short,
       @ticket_short
  FROM dispatch_hdr
 WHERE source_ref = 'seed-T4-hdr-002'
 LIMIT 1;

-- ----------------------------------------------------------
-- Delivered flow (Ticket A)
-- ----------------------------------------------------------
SET @close_delivered_id := NULL;

INSERT INTO close_hdr (
    pick_ticket_id,
    dispatch_id,
    final_status,
    pod_ref,
    pod_ts,
    notes,
    source_ref,
    created_by,
    updated_by
)
SELECT
    @ticket_delivered,
    @dispatch_delivered,
    'Delivered',
    CONCAT('POD-', @dispatch_delivered),
    CURRENT_TIMESTAMP,
    'All items delivered successfully.',
    'seed-T5-hdr-delivered',
    'seed',
    'seed'
WHERE @ticket_delivered   IS NOT NULL
  AND @dispatch_delivered IS NOT NULL;

SET @close_delivered_id := IF(ROW_COUNT() > 0, LAST_INSERT_ID(), NULL);

INSERT INTO close_variance (
    close_id,
    ticket_line_id,
    requested_qty,
    delivered_qty,
    short_qty,
    reason,
    source_ref,
    created_by,
    updated_by
)
SELECT
    @close_delivered_id,
    ptl.ticket_line_id,
    ptl.requested_qty,
    ptl.requested_qty,
    0,
    NULL,
    CONCAT('seed-T5-var-delivered-', ROW_NUMBER() OVER (ORDER BY ptl.ticket_line_id)),
    'seed',
    'seed'
FROM pick_ticket_line ptl
WHERE @close_delivered_id IS NOT NULL
  AND ptl.pick_ticket_id = @ticket_delivered;

-- ----------------------------------------------------------
-- Short-close flow (Ticket B)
-- ----------------------------------------------------------
SET @close_short_id := NULL;

INSERT INTO close_hdr (
    pick_ticket_id,
    dispatch_id,
    final_status,
    pod_ref,
    pod_ts,
    notes,
    source_ref,
    created_by,
    updated_by
)
SELECT
    @ticket_short,
    @dispatch_short,
    'Short-Closed',
    CONCAT('POD-', @dispatch_short),
    CURRENT_TIMESTAMP,
    'Partial delivery due to damaged items.',
    'seed-T5-hdr-short',
    'seed',
    'seed'
WHERE @ticket_short   IS NOT NULL
  AND @dispatch_short IS NOT NULL;

SET @close_short_id := IF(ROW_COUNT() > 0, LAST_INSERT_ID(), NULL);

INSERT INTO close_variance (
    close_id,
    ticket_line_id,
    requested_qty,
    delivered_qty,
    short_qty,
    reason,
    source_ref,
    created_by,
    updated_by
)
SELECT
    @close_short_id,
    ptl.ticket_line_id,
    ptl.requested_qty,
    ROUND(GREATEST(0, COALESCE(pl.picked_qty, 0)), 2) AS delivered_qty,
    ROUND(GREATEST(0, ptl.requested_qty - COALESCE(pl.picked_qty, 0)), 2) AS short_qty,
    CASE
      WHEN ROUND(GREATEST(0, ptl.requested_qty - COALESCE(pl.picked_qty, 0)), 2) > 0 THEN 'Damaged in transit'
      ELSE NULL
    END AS reason,
    CONCAT('seed-T5-var-short-', ROW_NUMBER() OVER (ORDER BY ptl.ticket_line_id)),
    'seed',
    'seed'
FROM pick_ticket_line ptl
LEFT JOIN picking_line pl
  ON pl.ticket_line_id = ptl.ticket_line_id
WHERE @close_short_id IS NOT NULL
  AND ptl.pick_ticket_id = @ticket_short;

COMMIT;

SELECT 'close_hdr' AS table_name, COUNT(*) AS row_count FROM close_hdr WHERE source_ref LIKE 'seed-T5%';
SELECT 'close_variance' AS table_name, COUNT(*) AS row_count FROM close_variance WHERE source_ref LIKE 'seed-T5%';

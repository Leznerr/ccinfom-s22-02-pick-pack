USE ccinfom_dev;

SET SQL_SAFE_UPDATES = 0;
DELETE FROM close_variance WHERE source_ref LIKE 'seed-T5%';
DELETE FROM close_hdr      WHERE source_ref LIKE 'seed-T5%';
SET SQL_SAFE_UPDATES = 1;

START TRANSACTION;

SELECT dispatch_id, pick_ticket_id
  INTO @dispatch_delivered, @ticket_delivered
  FROM dispatch_hdr
 WHERE source_ref = 'seed-T4-hdr-001'
 LIMIT 1;

SELECT ticket_line_id, product_id, requested_qty
  INTO @ticket_line_a, @product_a, @requested_qty_a
  FROM pick_ticket_line
 WHERE pick_ticket_id = @ticket_delivered
 ORDER BY ticket_line_id
 LIMIT 1;

SELECT ticket_line_id, product_id, requested_qty
  INTO @ticket_line_b, @product_b, @requested_qty_b
  FROM pick_ticket_line
 WHERE pick_ticket_id = @ticket_delivered
 ORDER BY ticket_line_id DESC
 LIMIT 1;

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

SET @close_delivered_id := LAST_INSERT_ID();

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
    @ticket_line_a,
    @requested_qty_a,
    @requested_qty_a,
    0,
    NULL,
    'seed-T5-var-delivered-1',
    'seed',
    'seed'
WHERE @close_delivered_id IS NOT NULL
  AND @ticket_line_a      IS NOT NULL;

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
    'Short-Closed',
    CONCAT('POD-', @dispatch_delivered, '-SHORT'),
    CURRENT_TIMESTAMP,
    'Partial delivery due to damaged items.',
    'seed-T5-hdr-short',
    'seed',
    'seed'
WHERE @ticket_delivered   IS NOT NULL
  AND @dispatch_delivered IS NOT NULL;

SET @close_short_id := LAST_INSERT_ID();

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
    @ticket_line_b,
    @requested_qty_b,
    GREATEST(0.01, ROUND(@requested_qty_b * 0.40, 2)),
    GREATEST(0, ROUND(@requested_qty_b - GREATEST(0.01, ROUND(@requested_qty_b * 0.40, 2)), 2)),
    'Damaged in transit',
    'seed-T5-var-short-1',
    'seed',
    'seed'
WHERE @close_short_id IS NOT NULL
  AND @ticket_line_b     IS NOT NULL;

COMMIT;

SELECT 'close_hdr' AS table_name, COUNT(*) AS row_count FROM close_hdr WHERE source_ref LIKE 'seed-T5%';
SELECT 'close_variance' AS table_name, COUNT(*) AS row_count FROM close_variance WHERE source_ref LIKE 'seed-T5%';

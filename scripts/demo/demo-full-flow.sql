-- =================================================================
-- Phase E — Full Lifecycle Demo (T1 → T5)
-- =================================================================
-- Runs an end-to-end lifecycle for two tickets:
-- 1. Ticket A: Full delivery (happy path)
-- 2. Ticket B: Short-delivery (variance scenario)
--
-- Pre-requisites:
-- - Clean DB seeded with `make seed` or equivalent.
-- - `docs/seed-id-map.md` must be up-to-date.
-- =================================================================

USE ccinfom_dev;

-- =================================================================
-- Flow 1: Ticket A — Delivered (Happy Path)
-- =================================================================

-- -----------------------------------------------------------------
-- Step 1.1: Select Canonical IDs for Ticket A
-- -----------------------------------------------------------------
SET @ticket_a_id = 1;
SET @dispatch_a_id = 5;

SELECT 'Flow 1: Ticket A (Delivered)' AS section,
       CONCAT('Ticket ID: ', @ticket_a_id) AS ticket,
       CONCAT('Dispatch ID: ', @dispatch_a_id) AS dispatch;

-- -----------------------------------------------------------------
-- Step 1.2: Review Pre-Close State
-- -----------------------------------------------------------------
SELECT 'Ticket A: Pre-Close Status' AS section;
SELECT pick_ticket_id, ticket_status FROM pick_ticket_hdr WHERE pick_ticket_id = @ticket_a_id;

SELECT 'Ticket A: Sealed Boxes & Dispatch Manifest' AS section;
SELECT pbh.box_id, pbh.sealed_flag, dl.dispatch_id, dh.manifest_no
FROM pack_box_hdr pbh
JOIN dispatch_line dl ON pbh.box_id = dl.box_id
JOIN dispatch_hdr dh ON dl.dispatch_id = dh.dispatch_id
WHERE pbh.pick_ticket_id = @ticket_a_id;

SELECT 'Ticket A: Product Inventory (Before Close)' AS section;
SELECT p.product_id, p.sku, p.on_hand_qty, p.reserved_qty
FROM products p
JOIN pick_ticket_line ptl ON p.product_id = ptl.product_id
WHERE ptl.pick_ticket_id = @ticket_a_id;

-- -----------------------------------------------------------------
-- Step 1.3: Close Ticket A as 'Delivered'
-- -----------------------------------------------------------------
START TRANSACTION;

-- Insert the close header
INSERT INTO close_hdr (pick_ticket_id, dispatch_id, final_status, pod_ref, pod_ts, notes, created_by, updated_by, source_ref)
VALUES (@ticket_a_id, @dispatch_a_id, 'Delivered', 'pod-signed-jd-20251110.pdf', NOW(), 'All items delivered successfully.', 'demo', 'demo', 'demo-close-A');

SET @close_a_id = LAST_INSERT_ID();

-- Insert variance lines (no shortages)
INSERT INTO close_variance (close_id, ticket_line_id, requested_qty, delivered_qty, short_qty, reason, created_by, updated_by, source_ref)
SELECT
    @close_a_id,
    ptl.ticket_line_id,
    ptl.requested_qty,
    ptl.requested_qty, -- delivered_qty = requested_qty
    0,                 -- short_qty = 0
    'Delivered in full',
    'demo',
    'demo',
    CONCAT('demo-var-A-', ptl.ticket_line_id)
FROM pick_ticket_line ptl
WHERE ptl.pick_ticket_id = @ticket_a_id;

-- Update inventory: on_hand and reserved both decrease
UPDATE products p
JOIN pick_ticket_line ptl ON p.product_id = ptl.product_id
SET
    p.on_hand_qty = p.on_hand_qty - ptl.requested_qty,
    p.reserved_qty = p.reserved_qty - ptl.requested_qty,
    p.updated_by = 'demo-close-A'
WHERE ptl.pick_ticket_id = @ticket_a_id;

-- Log the inventory transaction
INSERT INTO inventory_txn_log (product_id, ticket_id, source_txn_type, source_txn_id, delta_reserved, delta_on_hand, note, created_by, source_ref)
SELECT
    ptl.product_id,
    @ticket_a_id,
    'CLOSE',
    @close_a_id,
    -ptl.requested_qty, -- Delta is negative
    -ptl.requested_qty, -- Delta is negative
    'Full delivery close',
    'demo',
    CONCAT('demo-log-A-', ptl.product_id)
FROM pick_ticket_line ptl
WHERE ptl.pick_ticket_id = @ticket_a_id;

-- Flip ticket status to 'Delivered'
UPDATE pick_ticket_hdr
SET ticket_status = 'Delivered', updated_by = 'demo-close-A'
WHERE pick_ticket_id = @ticket_a_id;

COMMIT;

-- -----------------------------------------------------------------
-- Step 1.4: Verify Post-Close State
-- -----------------------------------------------------------------
SELECT 'Ticket A: Post-Close Status' AS section;
SELECT pick_ticket_id, ticket_status FROM pick_ticket_hdr WHERE pick_ticket_id = @ticket_a_id;

SELECT 'Ticket A: Close Records' AS section;
SELECT * FROM close_hdr WHERE pick_ticket_id = @ticket_a_id;
SELECT * FROM close_variance WHERE close_id = @close_a_id;

SELECT 'Ticket A: Product Inventory (After Close)' AS section;
SELECT p.product_id, p.sku, p.on_hand_qty, p.reserved_qty
FROM products p
JOIN pick_ticket_line ptl ON p.product_id = ptl.product_id
WHERE ptl.pick_ticket_id = @ticket_a_id;

SELECT 'Ticket A: Inventory Log Entries' AS section;
SELECT log_id, product_id, source_txn_type, delta_reserved, delta_on_hand, note
FROM inventory_txn_log
WHERE ticket_id = @ticket_a_id AND source_txn_type = 'CLOSE';


-- =================================================================
-- Flow 2: Ticket B — Short-Closed (Variance Scenario)
-- =================================================================

-- -----------------------------------------------------------------
-- Step 2.1: Select Canonical IDs for Ticket B
-- -----------------------------------------------------------------
SET @ticket_b_id = 2;
SET @dispatch_b_id = 6;

SELECT 'Flow 2: Ticket B (Short-Closed)' AS section,
       CONCAT('Ticket ID: ', @ticket_b_id) AS ticket,
       CONCAT('Dispatch ID: ', @dispatch_b_id) AS dispatch;

-- -----------------------------------------------------------------
-- Step 2.2: Review Pre-Close State
-- -----------------------------------------------------------------
SELECT 'Ticket B: Pre-Close Status' AS section;
SELECT pick_ticket_id, ticket_status FROM pick_ticket_hdr WHERE pick_ticket_id = @ticket_b_id;

SELECT 'Ticket B: Product Inventory (Before Close)' AS section;
SELECT p.product_id, p.sku, p.on_hand_qty, p.reserved_qty
FROM products p
JOIN pick_ticket_line ptl ON p.product_id = ptl.product_id
WHERE ptl.pick_ticket_id = @ticket_b_id;

-- -----------------------------------------------------------------
-- Step 2.3: Close Ticket B as 'Short-Closed'
-- -----------------------------------------------------------------
START TRANSACTION;

-- Insert the close header
INSERT INTO close_hdr (pick_ticket_id, dispatch_id, final_status, pod_ref, pod_ts, notes, created_by, updated_by, source_ref)
VALUES (@ticket_b_id, @dispatch_b_id, 'Short-Closed', 'pod-signed-incomplete-20251110.pdf', NOW(), 'One item short, warehouse damage.', 'demo', 'demo', 'demo-close-B');

SET @close_b_id = LAST_INSERT_ID();

-- Insert variance lines (with shortages)
-- Line 1: Full delivery
-- Line 2: Short delivery
-- Line 3: Zero delivery
INSERT INTO close_variance (close_id, ticket_line_id, requested_qty, delivered_qty, short_qty, reason, created_by, updated_by, source_ref)
VALUES
    (@close_b_id, 3, 10.00, 10.00, 0.00, 'Delivered in full', 'demo', 'demo', 'demo-var-B-3'),
    (@close_b_id, 4, 5.00, 3.00, 2.00, 'Damaged in warehouse', 'demo', 'demo', 'demo-var-B-4'),
    (@close_b_id, 5, 8.00, 0.00, 8.00, 'Out of stock', 'demo', 'demo', 'demo-var-B-5');

-- Update inventory based on variances
-- For delivered items, decrease on_hand and reserved
-- For shorted items, only decrease reserved
UPDATE products p
JOIN close_variance cv ON p.product_id = (SELECT product_id FROM pick_ticket_line WHERE ticket_line_id = cv.ticket_line_id)
SET
    p.on_hand_qty = p.on_hand_qty - cv.delivered_qty,
    p.reserved_qty = p.reserved_qty - (cv.delivered_qty + cv.short_qty),
    p.updated_by = 'demo-close-B'
WHERE cv.close_id = @close_b_id;

-- Log the inventory transactions
INSERT INTO inventory_txn_log (product_id, ticket_id, source_txn_type, source_txn_id, delta_reserved, delta_on_hand, note, created_by, source_ref)
SELECT
    ptl.product_id,
    @ticket_b_id,
    'CLOSE',
    @close_b_id,
    -(cv.delivered_qty + cv.short_qty),
    -cv.delivered_qty,
    'Short delivery close',
    'demo',
    CONCAT('demo-log-B-', ptl.product_id)
FROM close_variance cv
JOIN pick_ticket_line ptl ON cv.ticket_line_id = ptl.ticket_line_id
WHERE cv.close_id = @close_b_id;

-- Flip ticket status to 'Closed'
UPDATE pick_ticket_hdr
SET ticket_status = 'Closed', updated_by = 'demo-close-B'
WHERE pick_ticket_id = @ticket_b_id;

COMMIT;

-- -----------------------------------------------------------------
-- Step 2.4: Verify Post-Close State
-- -----------------------------------------------------------------
SELECT 'Ticket B: Post-Close Status' AS section;
SELECT pick_ticket_id, ticket_status FROM pick_ticket_hdr WHERE pick_ticket_id = @ticket_b_id;

SELECT 'Ticket B: Close Records' AS section;
SELECT * FROM close_hdr WHERE pick_ticket_id = @ticket_b_id;
SELECT * FROM close_variance WHERE close_id = @close_b_id;

SELECT 'Ticket B: Product Inventory (After Close)' AS section;
SELECT p.product_id, p.sku, p.on_hand_qty, p.reserved_qty
FROM products p
JOIN pick_ticket_line ptl ON p.product_id = ptl.product_id
WHERE ptl.pick_ticket_id = @ticket_b_id;

SELECT 'Ticket B: Inventory Log Entries' AS section;
SELECT log_id, product_id, source_txn_type, delta_reserved, delta_on_hand, note
FROM inventory_txn_log
WHERE ticket_id = @ticket_b_id AND source_txn_type = 'CLOSE';

-- =================================================================
-- End of Demo
-- =================================================================
SELECT 'Full lifecycle demo complete.' AS message;
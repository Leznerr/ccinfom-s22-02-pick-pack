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
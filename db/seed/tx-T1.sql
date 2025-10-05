/* ==========================================
   PHASE C — SEEDS FOR T1 (Create Pick Ticket)
   Goal: 3 tickets (2–3 lines each) using existing core data. No failing inserts.
   Guidance:
   [ ] Insert into pick_ticket_hdr (customer_id, branch_id, ticket_status='Open', remarks, updated_by='seed')
   [ ] Insert matching pick_ticket_line rows (product_id, requested_qty>0, uom) — active products only
   [ ] Do not duplicate product within the same ticket (UNIQUE guard)
   DEFINITION OF DONE (SEEDS T1)
   [ ] Script runs clean on fresh DB after schema & core seeds
   [ ] 3 headers created; 6–9 lines total; all audit fields populated
========================================== */
USE ccinfom_dev;

START TRANSACTION;

-- Ticket 1: Customer 1 at Branch 1
INSERT INTO pick_ticket_hdr (customer_id, branch_id, ticket_status, remarks, updated_by)
VALUES (1, 1, 'Open', 'Initial order for gadgets', 'seed');

INSERT INTO pick_ticket_line (pick_ticket_id, product_id, requested_qty, uom, updated_by)
VALUES
  (LAST_INSERT_ID(), 1, 5, 'pcs', 'seed'),
  (LAST_INSERT_ID(), 2, 10, 'pcs', 'seed');

-- Ticket 2: Customer 2 at Branch 2
INSERT INTO pick_ticket_hdr (customer_id, branch_id, ticket_status, remarks, updated_by)
VALUES (2, 2, 'Open', 'Tools restock order', 'seed');

INSERT INTO pick_ticket_line (pick_ticket_id, product_id, requested_qty, uom, updated_by)
VALUES
  (LAST_INSERT_ID(), 3, 8, 'pcs', 'seed'),
  (LAST_INSERT_ID(), 4, 4, 'pcs', 'seed'),
  (LAST_INSERT_ID(), 5, 2, 'pcs', 'seed');

-- Ticket 3: Customer 3 at Branch 3
INSERT INTO pick_ticket_hdr (customer_id, branch_id, ticket_status, remarks, updated_by)
VALUES (3, 3, 'Open', 'Accessory shipment', 'seed');

INSERT INTO pick_ticket_line (pick_ticket_id, product_id, requested_qty, uom, updated_by)
VALUES
  (LAST_INSERT_ID(), 6, 12, 'pcs', 'seed'),
  (LAST_INSERT_ID(), 7, 6, 'pcs', 'seed');

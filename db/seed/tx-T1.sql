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


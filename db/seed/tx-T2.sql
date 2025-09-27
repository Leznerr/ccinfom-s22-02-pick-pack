/* ==========================================
   PHASE C — SEEDS FOR T2 (Allocate & Pick)
   Goal: 1 full pick for Ticket A + 1 partial pick for Ticket B. No failing inserts.
   Guidance:
   [ ] Insert picking_hdr (pick_ticket_id, picker_employee_id, picking_status='Picking', updated_by='seed')
   [ ] Insert picking_line (picking_id, ticket_line_id, product_id, picked_qty>0, uom)
   [ ] Ensure picked_qty ≤ available (on_hand - reserved) to keep seed green
   [ ] After insert, verify products.reserved_qty increased; on_hand unchanged
   DEFINITION OF DONE (SEEDS T2)
   [ ] Script runs clean; ticket status for seeded ticket becomes 'Picking'
   [ ] products.reserved_qty reflects SUM(picked_qty) deltas
========================================== */


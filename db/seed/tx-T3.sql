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


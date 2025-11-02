-- Phase E Seeds: T4 Dispatch

-- TODO[E-SEED-T4-001] Insert happy dispatch manifest loading sealed boxes.
-- Steps:
--   1) Reference pack_box_hdr entries with sealed_flag = 1.
--   2) Insert dispatch_hdr (available vehicle/driver, manifest_no unique).
--   3) Insert dispatch_line rows (one per box).
-- Acceptance: Script loads cleanly; ticket status updated to Dispatched via service.

-- TODO[E-SEED-T4-002] Seed exception scenarios (vehicle maintenance, unsealed box, capacity breach, duplicate load).
-- Steps:
--   1) Wrap failing INSERTs in transactions with ROLLBACK and comments referencing exception codes.
--   2) Provide data for QA queries (e.g., maintenance vehicle row).
-- Acceptance: Demo script uses these seeds to showcase failures; QA duplicate/vehicle checks pass after rollback.

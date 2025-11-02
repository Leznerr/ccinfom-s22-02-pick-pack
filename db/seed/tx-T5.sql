-- Phase E Seeds: T5 Close Ticket

-- TODO[E-SEED-T5-001] Insert fully delivered close scenario.
-- Steps:
--   1) Reference dispatch_hdr/dispatch_line records.
--   2) Insert close_hdr (final_status='Delivered') and close_variance rows with short_qty=0.
-- Acceptance: CloseService sets ticket to Delivered and logs inventory deltas (reserved -, on_hand -).

-- TODO[E-SEED-T5-002] Insert short-close scenario with variance reasons.
-- Steps:
--   1) Provide close_hdr final_status='Short-Closed'.
--   2) close_variance rows with delivered < requested, short_qty recorded.
--   3) Ensure pod_ref reuses dispatch seed value.
-- Acceptance: QA reconciliation query passes; demo-full-flow.sql highlights short-close handling.

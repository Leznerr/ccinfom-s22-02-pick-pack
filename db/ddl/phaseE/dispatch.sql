-- Phase E DDL scaffold: Dispatch (T4)

-- TODO[E-DDL-T4-004] Create dispatch_hdr table.
-- Why: Manifest metadata needed for dispatch workflow.
-- Columns (suggested): dispatch_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, vehicle_id FK, driver_id FK (employees), manifest_no VARCHAR UNIQUE, depart_ts, arrive_ts, pod_ref, pod_ts, created_at/by, updated_at/by.
-- Notes: Align pod_ref/pod_ts types with Close module; ensure indices on vehicle_id, pick_ticket_id.
-- Acceptance: tx-T4.sql inserts succeed; QA vehicle capacity/status query runs against this table.

-- TODO[E-DDL-T4-005] Create dispatch_line table.
-- Why: Links sealed boxes to manifest with uniqueness constraint.
-- Columns: dispatch_line_id BIGINT UNSIGNED PK AUTO_INCREMENT, dispatch_id FKâ†’dispatch_hdr, box_id FKâ†’pack_box_hdr, created_at/by.
-- Constraints: UNIQUE(box_id) to prevent duplicate loads.
-- Acceptance: Duplicate load demo raises DISPATCH_BOX_ALREADY_LOADED; QA duplicate check returns zero.

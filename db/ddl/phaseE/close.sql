-- Phase E DDL scaffold: Close Ticket (T5)

-- TODO[E-DDL-T5-006] Create close_hdr table.
-- Why: Stores final delivery outcome with linkage to dispatch/ticket.
-- Columns (suggested): close_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, dispatch_id FK, final_status ENUM('Delivered','Short-Closed'), pod_ref, pod_ts, notes, created_at/by, updated_at/by.
-- Notes: Align pod fields with dispatch_hdr; ensure final_status values match service transitions.
-- Acceptance: tx-T5.sql loads; CloseService updates ticket status; QA inventory reconciliation passes.
-- Owner: Renzel

-- TODO[E-DDL-T5-007] Create close_variance table.
-- Why: Tracks delivered vs short quantities per ticket line.
-- Columns: variance_id BIGINT UNSIGNED PK AUTO_INCREMENT, close_id FK→close_hdr, ticket_line_id FK→pick_ticket_line, requested_qty DECIMAL(12,2) NOT NULL, delivered_qty DECIMAL(12,2) NOT NULL, short_qty DECIMAL(12,2) NOT NULL, reason VARCHAR.
-- Constraints: Column-level CHECKs for non-negatives; reconciliation enforced in service/tests.
-- Acceptance: ServiceTestRunner close tests green; QA reconciliation query returns zero.
-- Owner: Renzel

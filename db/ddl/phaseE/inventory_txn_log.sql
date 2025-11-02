-- Phase E DDL scaffold: inventory_txn_log (cross-cutting)

-- TODO[E-DDL-XCUT-008] Define inventory_txn_log table for audited deltas (T2/T5 only).
-- Columns (suggested): log_id BIGINT UNSIGNED PK AUTO_INCREMENT, product_id FK→products, warehouse_id BIGINT NULL (future use), ticket_id BIGINT NULL, source_txn_type ENUM('RESERVE','CLOSE') NOT NULL, source_txn_id BIGINT NOT NULL, delta_reserved DECIMAL(12,2) NOT NULL, delta_on_hand DECIMAL(12,2) NOT NULL, note VARCHAR(300), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, created_by VARCHAR(64) NOT NULL.
-- Constraints: CHECK(delta_reserved IS NOT NULL AND delta_on_hand IS NOT NULL); consider index on (product_id, source_txn_type).
-- Acceptance: RESERVE/CLOSE services write entries with non-zero deltas; QA inventory rollup uses this table; no entries created for PACK.
-- Owner: Joshua

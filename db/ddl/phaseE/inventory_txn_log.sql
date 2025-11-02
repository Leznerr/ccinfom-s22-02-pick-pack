-- Phase E DDL scaffold: inventory_txn_log (cross-cutting)

-- TODO[E-DDL-XCUT-008] Define inventory_txn_log table for audited deltas (T2/T5 only).
-- Columns (suggested): log_id BIGINT UNSIGNED PK AUTO_INCREMENT, product_id FK to products, warehouse_id BIGINT NULL (future use), ticket_id BIGINT NULL, source_txn_type ENUM('RESERVE','CLOSE') NOT NULL, source_txn_id BIGINT NOT NULL, delta_reserved DECIMAL(12,2) NOT NULL, delta_on_hand DECIMAL(12,2) NOT NULL, note VARCHAR(300), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, created_by VARCHAR(64) NOT NULL.
-- Constraints: CHECK(delta_reserved IS NOT NULL AND delta_on_hand IS NOT NULL); consider index on (product_id, source_txn_type).
-- Acceptance: RESERVE/CLOSE services write entries with non-zero deltas; QA inventory rollup uses this table; no entries created for PACK.

USE ccinfom_dev;

CREATE TABLE inventory_txn_log (
  log_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT UNSIGNED NOT NULL,
  warehouse_id    BIGINT UNSIGNED NULL,
  ticket_id       BIGINT UNSIGNED NULL,
  source_txn_type ENUM('RESERVE','CLOSE') NOT NULL,
  source_txn_id   BIGINT UNSIGNED NOT NULL,
  delta_reserved  DECIMAL(12,2) NOT NULL,
  delta_on_hand   DECIMAL(12,2) NOT NULL,
  note            VARCHAR(300) NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT ck_inventory_txn_nonzero
    CHECK (delta_reserved <> 0 OR delta_on_hand <> 0),

  CONSTRAINT fk_inventory_txn_product
    FOREIGN KEY (product_id)
    REFERENCES products(product_id),

  CONSTRAINT fk_inventory_txn_ticket
    FOREIGN KEY (ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE SET NULL
);

CREATE INDEX idx_inventory_txn_product_source
  ON inventory_txn_log (product_id, source_txn_type, source_txn_id);

CREATE INDEX idx_inventory_txn_ticket
  ON inventory_txn_log (ticket_id);

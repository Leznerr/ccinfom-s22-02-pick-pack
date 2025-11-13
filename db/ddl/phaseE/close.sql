-- Phase E DDL implementation: Close Ticket (T5)

-- TODO[E-DDL-T5-006] Create close_hdr table.
-- Why: Stores final delivery outcome with linkage to dispatch/ticket.
-- Columns (suggested): close_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, dispatch_id FK, final_status ENUM('Delivered','Short-Closed'), pod_ref, pod_ts, notes, created_at/by, updated_at/by.
-- Notes: Align pod fields with dispatch_hdr; ensure final_status values match service transitions.
-- Acceptance: tx-T5.sql loads; CloseService updates ticket status; QA inventory reconciliation passes.

-- TODO[E-DDL-T5-007] Create close_variance table.
-- Why: Tracks delivered vs short quantities per ticket line.
-- Columns: variance_id BIGINT UNSIGNED PK AUTO_INCREMENT, close_id FK close_hdr, ticket_line_id FK pick_ticket_line, requested_qty DECIMAL(12,2) NOT NULL, delivered_qty DECIMAL(12,2) NOT NULL, short_qty DECIMAL(12,2) NOT NULL, reason VARCHAR.
-- Constraints: Column-level CHECKs for non-negatives; reconciliation enforced in service/tests.
-- Acceptance: ServiceTestRunner close tests green; QA reconciliation query returns zero.

USE ccinfom_dev;
DROP TABLE IF EXISTS close_variance;
DROP TABLE IF EXISTS close_hdr;

CREATE TABLE close_hdr (
  close_id        BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  dispatch_id     BIGINT UNSIGNED NOT NULL,
  final_status    ENUM('Delivered','Short-Closed') NOT NULL,
  pod_ref         VARCHAR(100) NULL,
  pod_ts          TIMESTAMP NULL,
  notes           VARCHAR(300) NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_close_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_close_dispatch
    FOREIGN KEY (dispatch_id)
    REFERENCES dispatch_hdr(dispatch_id)
    ON DELETE CASCADE
);

CREATE INDEX idx_close_hdr_ticket_id ON close_hdr(pick_ticket_id);
CREATE INDEX idx_close_hdr_dispatch_id ON close_hdr(dispatch_id);
CREATE INDEX idx_close_hdr_status ON close_hdr(final_status);

CREATE TABLE close_variance (
  variance_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  close_id        BIGINT UNSIGNED NOT NULL,
  ticket_line_id  BIGINT UNSIGNED NOT NULL,
  requested_qty   DECIMAL(12,2) NOT NULL,
  delivered_qty   DECIMAL(12,2) NOT NULL,
  short_qty       DECIMAL(12,2) NOT NULL,
  reason          VARCHAR(200) NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_close_variance_hdr
    FOREIGN KEY (close_id)
    REFERENCES close_hdr(close_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_close_variance_ticket_line
    FOREIGN KEY (ticket_line_id)
    REFERENCES pick_ticket_line(ticket_line_id)
    ON DELETE CASCADE,

  CONSTRAINT ck_close_variance_requested_nonneg CHECK (requested_qty >= 0),
  CONSTRAINT ck_close_variance_delivered_nonneg CHECK (delivered_qty >= 0),
  CONSTRAINT ck_close_variance_short_nonneg CHECK (short_qty >= 0)
);

CREATE INDEX idx_close_variance_hdr ON close_variance(close_id);
CREATE INDEX idx_close_variance_ticket_line ON close_variance(ticket_line_id);

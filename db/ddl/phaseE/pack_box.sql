-- Phase E DDL scaffold: Pack (T3)
USE ccinfom_dev;

-- TODO[E-DDL-T3-002] Implement pack_box_hdr table definition.
-- Why: Required to track boxes created during packing; must include audit trio and sealed_flag.
-- Columns (suggested): box_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, picking_id FK, sealed_flag BOOLEAN, created_at/by, updated_at/by.
-- Notes: Enforce FK to picking_hdr (ticket lineage) and default sealed_flag = 0.
-- Acceptance: table loads on clean DB; tx-T3.sql inserts succeed; PackService integration tests pass.
-- | Links: docs/seed-id-map.md

CREATE TABLE pack_box_hdr (
  box_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  picking_id      BIGINT UNSIGNED NOT NULL,
  sealed_flag     BOOLEAN NOT NULL DEFAULT FALSE,
  seal_method     VARCHAR(50) NULL,
  sealed_at       TIMESTAMP NULL,
  handling_notes  VARCHAR(255) NULL,
  box_status      VARCHAR(20) NOT NULL DEFAULT 'Open',
  source_ref	  VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by	  VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_pack_box_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_pack_box_picking
    FOREIGN KEY (picking_id)
    REFERENCES picking_hdr(picking_id)
    ON DELETE RESTRICT
);

-- Helpful indexes
CREATE INDEX idx_pack_box_ticket_id ON pack_box_hdr(pick_ticket_id);
CREATE INDEX idx_pack_box_picking_id ON pack_box_hdr(picking_id);
CREATE INDEX idx_pack_box_sealed_flag ON pack_box_hdr(sealed_flag);

-- TODO[E-DDL-T3-003] Implement pack_box_line table definition.
-- Why: Associates picking_line to boxes with single-lineage rule.
-- Columns: box_line_id BIGINT UNSIGNED PK AUTO_INCREMENT, box_id FK→pack_box_hdr, picking_line_id FK→picking_line, packed_qty DECIMAL(12,2) NOT NULL CHECK (packed_qty >= 0), updated_by.
-- Constraints: UNIQUE(picking_line_id) to prevent double-boxing.
-- Acceptance: FK/UNIQUE enforced; over-pack test triggers via service; QA packed_vs_picked query reads this table.
-- 

CREATE TABLE pack_box_line (
  box_line_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  box_id          BIGINT UNSIGNED NOT NULL,
  picking_line_id BIGINT UNSIGNED NOT NULL,
  packed_qty      DECIMAL(12,2) NOT NULL CHECK (packed_qty >= 0),
  uom             VARCHAR(50) NOT NULL,
  source_ref	  VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by	  VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_pack_line_hdr
    FOREIGN KEY (box_id)
    REFERENCES pack_box_hdr(box_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_pack_line_picking
    FOREIGN KEY (picking_line_id)
    REFERENCES picking_line(picking_line_id),

  CONSTRAINT uq_pack_line_picking UNIQUE (picking_line_id)
);

-- Helpful indexes
CREATE INDEX idx_pack_line_box_id ON pack_box_line(box_id);
CREATE INDEX idx_pack_line_picking_id ON pack_box_line(picking_line_id);

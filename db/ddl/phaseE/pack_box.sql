-- Phase E DDL scaffold: Pack (T3)

-- TODO[E-DDL-T3-002] Implement pack_box_hdr table definition.
-- Why: Required to track boxes created during packing; must include audit trio and sealed_flag.
-- Columns (suggested): box_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, picking_id FK, sealed_flag BOOLEAN, created_at/by, updated_at/by.
-- Notes: Enforce FK to picking_hdr (ticket lineage) and default sealed_flag = 0.
-- Acceptance: table loads on clean DB; tx-T3.sql inserts succeed; PackService integration tests pass.
-- Owner: Mark | Links: docs/seed-id-map.md

-- TODO[E-DDL-T3-003] Implement pack_box_line table definition.
-- Why: Associates picking_line to boxes with single-lineage rule.
-- Columns: box_line_id BIGINT UNSIGNED PK AUTO_INCREMENT, box_id FK→pack_box_hdr, picking_line_id FK→picking_line, packed_qty DECIMAL(12,2) NOT NULL CHECK (packed_qty >= 0), updated_by.
-- Constraints: UNIQUE(picking_line_id) to prevent double-boxing.
-- Acceptance: FK/UNIQUE enforced; over-pack test triggers via service; QA packed_vs_picked query reads this table.
-- Owner: Mark

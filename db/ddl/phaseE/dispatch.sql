-- Phase E DDL scaffold: Dispatch (T4)

-- TODO[E-DDL-T4-004] Create dispatch_hdr table.
-- Why: Manifest metadata needed for dispatch workflow.
-- Columns (suggested): dispatch_id BIGINT UNSIGNED PK AUTO_INCREMENT, pick_ticket_id FK, vehicle_id FK, driver_id FK (employees), manifest_no VARCHAR UNIQUE, depart_ts, arrive_ts, pod_ref, pod_ts, created_at/by, updated_at/by.
-- Notes: Align pod_ref/pod_ts types with Close module; ensure indices on vehicle_id, pick_ticket_id.
-- Acceptance: tx-T4.sql inserts succeed; QA vehicle capacity/status query runs against this table.
-- | Links: docs/seed-id-map.md

-- TODO[E-DDL-T4-005] Create dispatch_line table.
-- Why: Links sealed boxes to manifest with uniqueness constraint.
-- Columns: dispatch_line_id BIGINT UNSIGNED PK AUTO_INCREMENT, dispatch_id FK→dispatch_hdr, box_id FK→pack_box_hdr, created_at/by.
-- Constraints: UNIQUE(box_id) to prevent duplicate loads.
-- Acceptance: Duplicate load demo raises DISPATCH_BOX_ALREADY_LOADED; QA duplicate check returns zero.
-- 
-- Phase E DDL implementation: Dispatch (T4)
USE ccinfom_dev;

CREATE TABLE dispatch_hdr (
  dispatch_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pick_ticket_id  BIGINT UNSIGNED NOT NULL,
  vehicle_id      BIGINT UNSIGNED NOT NULL,
  driver_id       BIGINT UNSIGNED NOT NULL, -- references the employee who is driving
  manifest_no     VARCHAR(100) NOT NULL UNIQUE, -- official dispatch num (unique)
  depart_ts       TIMESTAMP NULL,
  arrive_ts       TIMESTAMP NULL,
  pod_ref         VARCHAR(100) NULL,
  pod_ts          TIMESTAMP NULL,
  source_ref      VARCHAR(100) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by      VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_dispatch_ticket
    FOREIGN KEY (pick_ticket_id)
    REFERENCES pick_ticket_hdr(pick_ticket_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_dispatch_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES vehicles(vehicle_id),

  CONSTRAINT fk_dispatch_driver
    FOREIGN KEY (driver_id)
    REFERENCES employees(employee_id)
);

-- Helpful indexes
CREATE INDEX idx_dispatch_vehicle_id ON dispatch_hdr(vehicle_id);
CREATE INDEX idx_dispatch_ticket_id ON dispatch_hdr(pick_ticket_id);
CREATE INDEX idx_dispatch_driver_id ON dispatch_hdr(driver_id);

CREATE TABLE dispatch_line (
  dispatch_line_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, -- unique id for each box line
  dispatch_id      BIGINT UNSIGNED NOT NULL, -- points to the header
  box_id           BIGINT UNSIGNED NOT NULL,
  source_ref       VARCHAR(100) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_dispatch_line_hdr
    FOREIGN KEY (dispatch_id)
    REFERENCES dispatch_hdr(dispatch_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_dispatch_line_box
    FOREIGN KEY (box_id)
    REFERENCES pack_box_hdr(box_id)
    ON DELETE CASCADE,

  CONSTRAINT uq_dispatch_line_box UNIQUE (box_id) -- prevents the same box from being added to multiple dispatches
);

-- Helpful indexes
CREATE INDEX idx_dispatch_line_hdr ON dispatch_line(dispatch_id);
CREATE INDEX idx_dispatch_line_box ON dispatch_line(box_id);

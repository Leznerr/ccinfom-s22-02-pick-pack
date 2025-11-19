USE ccinfom_dev;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS close_variance;
DROP TABLE IF EXISTS close_hdr;
DROP TABLE IF EXISTS dispatch_line;
DROP TABLE IF EXISTS dispatch_hdr;

SET FOREIGN_KEY_CHECKS = 1;
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
    ON DELETE RESTRICT,

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
  dispatch_id      BIGINT UNSIGNED NOT NULL, -- points to the header6666
  box_id           BIGINT UNSIGNED NOT NULL,
  source_ref       VARCHAR(100) NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by       VARCHAR(64) NOT NULL DEFAULT 'system',
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by       VARCHAR(64) NOT NULL DEFAULT 'system',

  CONSTRAINT fk_dispatch_line_hdr
    FOREIGN KEY (dispatch_id)
    REFERENCES dispatch_hdr(dispatch_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_dispatch_line_box
    FOREIGN KEY (box_id)
    REFERENCES pack_box_hdr(box_id)
    ON DELETE RESTRICT,

  CONSTRAINT uq_dispatch_line_box UNIQUE (box_id) -- prevents the same box from being added to multiple dispatches
);

-- Helpful indexes
CREATE INDEX idx_dispatch_line_hdr ON dispatch_line(dispatch_id);
CREATE INDEX idx_dispatch_line_box ON dispatch_line(box_id);

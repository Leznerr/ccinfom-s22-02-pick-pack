USE ccinfom_dev;

-- ----------------------------------------------------------
-- 0) Clean up previous T4 seed data (scoped by source_ref)
-- ----------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
DELETE FROM dispatch_line
 WHERE source_ref LIKE 'seed-T4%'
    OR box_id IN (
         SELECT box_id
           FROM pack_box_hdr
          WHERE source_ref IN ('seed-T3-box1','seed-T3-box2')
             OR source_ref LIKE 'seed-T4-capacity-box-%'
       );
DELETE FROM dispatch_hdr
 WHERE source_ref LIKE 'seed-T4%'
    OR pick_ticket_id IN (
         SELECT pick_ticket_id
           FROM pack_box_hdr
          WHERE source_ref IN ('seed-T3-box1','seed-T3-box2')
       );
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------------------------------------
-- 1) Gather IDs produced by earlier seeds
-- ----------------------------------------------------------
SELECT box_id, pick_ticket_id, picking_id
  INTO @sealed_box_id, @sealed_ticket_id, @sealed_picking_id
  FROM pack_box_hdr
 WHERE source_ref = 'seed-T3-box1'
 LIMIT 1;

SELECT box_id
  INTO @unsealed_box_id
  FROM pack_box_hdr
 WHERE source_ref = 'seed-T3-box2'
 LIMIT 1;

SELECT vehicle_id
  INTO @vehicle_primary_id
  FROM vehicles
 WHERE vehicle_status = 'available'
 ORDER BY vehicle_id
 LIMIT 1;

SELECT vehicle_id
  INTO @vehicle_secondary_id
  FROM vehicles
 WHERE vehicle_status = 'available'
   AND vehicle_id <> @vehicle_primary_id
 ORDER BY vehicle_id
 LIMIT 1;

SELECT employee_id
  INTO @driver_primary_id
  FROM employees
 WHERE employee_role = 'dispatcher'
 ORDER BY employee_id
 LIMIT 1;

SELECT employee_id
  INTO @driver_secondary_id
  FROM employees
 WHERE employee_role = 'dispatcher'
   AND employee_id <> @driver_primary_id
 ORDER BY employee_id
 LIMIT 1;

-- ----------------------------------------------------------
-- 2) Exception scaffolds (each rolled back)
-- ----------------------------------------------------------

-- 2a. Vehicle under maintenance
START TRANSACTION;

UPDATE vehicles
   SET vehicle_status = 'maintenance'
 WHERE vehicle_id = @vehicle_secondary_id;

INSERT INTO dispatch_hdr (
    pick_ticket_id,
    vehicle_id,
    driver_id,
    manifest_no,
    depart_ts,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @sealed_ticket_id,
    @vehicle_secondary_id,
    COALESCE(@driver_secondary_id, @driver_primary_id),
    'MANIFEST-MAINT',
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T4-maint'
WHERE @sealed_ticket_id IS NOT NULL
  AND @vehicle_secondary_id IS NOT NULL;

ROLLBACK;

-- 2b. Unsealed box attempt
START TRANSACTION;

INSERT INTO dispatch_hdr (
    pick_ticket_id,
    vehicle_id,
    driver_id,
    manifest_no,
    depart_ts,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @sealed_ticket_id,
    @vehicle_primary_id,
    COALESCE(@driver_secondary_id, @driver_primary_id),
    'MANIFEST-UNSEALED',
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T4-unsealed'
WHERE @sealed_ticket_id IS NOT NULL
  AND @vehicle_primary_id IS NOT NULL;

SET @dispatch_unsealed_id := LAST_INSERT_ID();

INSERT INTO dispatch_line (
    dispatch_id,
    box_id,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @dispatch_unsealed_id,
    @unsealed_box_id,
    'seed',
    'seed',
    'seed-T4-unsealed-line'
WHERE @dispatch_unsealed_id IS NOT NULL
  AND @unsealed_box_id IS NOT NULL;

ROLLBACK;

-- 2c. Capacity breach (temporary boxes inside transaction)
START TRANSACTION;

INSERT INTO dispatch_hdr (
    pick_ticket_id,
    vehicle_id,
    driver_id,
    manifest_no,
    depart_ts,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @sealed_ticket_id,
    @vehicle_primary_id,
    COALESCE(@driver_primary_id, @driver_secondary_id),
    'MANIFEST-CAPACITY',
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T4-capacity'
WHERE @sealed_ticket_id IS NOT NULL
  AND @vehicle_primary_id IS NOT NULL;

SET @dispatch_capacity_id := LAST_INSERT_ID();

INSERT INTO pack_box_hdr (
    pick_ticket_id,
    picking_id,
    sealed_flag,
    seal_method,
    sealed_at,
    created_by,
    updated_by,
    source_ref
)
SELECT @sealed_ticket_id, @sealed_picking_id, TRUE, 'strap', CURRENT_TIMESTAMP, 'seed', 'seed',
       CONCAT('seed-T4-capacity-box-', seq.seq)
  FROM (SELECT 1 AS seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) seq
WHERE @sealed_ticket_id IS NOT NULL
  AND @sealed_picking_id IS NOT NULL;

INSERT INTO dispatch_line (
    dispatch_id,
    box_id,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @dispatch_capacity_id,
    box_id,
    'seed',
    'seed',
    CONCAT('seed-T4-capacity-line-', ROW_NUMBER() OVER (ORDER BY box_id))
  FROM pack_box_hdr
 WHERE source_ref LIKE 'seed-T4-capacity-box-%';

ROLLBACK;

-- 2d. Duplicate load staging (service will attempt duplicate)
START TRANSACTION;

INSERT INTO dispatch_hdr (
    pick_ticket_id,
    vehicle_id,
    driver_id,
    manifest_no,
    depart_ts,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @sealed_ticket_id,
    @vehicle_secondary_id,
    COALESCE(@driver_primary_id, @driver_secondary_id),
    'MANIFEST-DUPLOAD',
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T4-duplicate'
WHERE @sealed_ticket_id IS NOT NULL
  AND @vehicle_secondary_id IS NOT NULL;

-- Only stage the header; the service layer will attempt to add the same box
-- and should hit DISPATCH_BOX_ALREADY_LOADED.
ROLLBACK;

-- ----------------------------------------------------------
-- 3) Happy path — sealed boxes loaded onto a manifest
-- ----------------------------------------------------------
START TRANSACTION;

INSERT INTO dispatch_hdr (
    pick_ticket_id,
    vehicle_id,
    driver_id,
    manifest_no,
    depart_ts,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @sealed_ticket_id,
    @vehicle_primary_id,
    COALESCE(@driver_primary_id, @driver_secondary_id),
    CONCAT('MANIFEST-', LPAD(@sealed_ticket_id, 4, '0')),
    CURRENT_TIMESTAMP,
    'seed',
    'seed',
    'seed-T4-hdr-001'
WHERE @sealed_ticket_id IS NOT NULL
  AND @vehicle_primary_id IS NOT NULL
  AND ( @driver_primary_id IS NOT NULL OR @driver_secondary_id IS NOT NULL );

SET @dispatch_happy_id := LAST_INSERT_ID();

INSERT INTO dispatch_line (
    dispatch_id,
    box_id,
    created_by,
    updated_by,
    source_ref
)
SELECT
    @dispatch_happy_id,
    @sealed_box_id,
    'seed',
    'seed',
    'seed-T4-line-001'
WHERE @dispatch_happy_id IS NOT NULL
  AND @sealed_box_id IS NOT NULL;

COMMIT;

-- ----------------------------------------------------------
-- 4) Reference counts for verification
-- ----------------------------------------------------------
SELECT 'dispatch_hdr' AS table_name, COUNT(*) AS row_count
  FROM dispatch_hdr
 WHERE source_ref LIKE 'seed-T4%';

SELECT 'dispatch_line' AS table_name, COUNT(*) AS row_count
  FROM dispatch_line
 WHERE source_ref LIKE 'seed-T4%';

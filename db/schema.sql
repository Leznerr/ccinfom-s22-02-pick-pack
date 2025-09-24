-- PHASE B — CORES ONLY (NO FKs here). Replace TODOs with actual DDL later.
-- Goal: Define 5 core tables with PK + business fields + audit trio.
-- DoD: schema loads cleanly; audit trio on every core; ZERO FOREIGN KEY clauses in cores.

-- [PRODUCTS]
-- TODO: CREATE TABLE products (
--   PK: product_id INT AUTO_INCREMENT PRIMARY KEY
--   Business fields: sku (UNIQUE), name, category, unit_price, unit_of_measure
--   Inventory fields: on_hand_qty DECIMAL, reserved_qty DECIMAL
--   Status: active_flag BOOLEAN
--   Audit trio: created_at, updated_at (with ON UPDATE), updated_by
-- );

-- [CUSTOMERS]
-- TODO: CREATE TABLE customers (
--   PK: customer_id
--   Business fields: customer_name, contact_person, phone, email, default_delivery_address (TEXT)
--   Audit trio: created_at, updated_at, updated_by
-- );

-- [EMPLOYEES]
-- TODO: CREATE TABLE employees (
--   PK: employee_id
--   Business fields: last_name, first_name, role (picker/packer/dispatcher), phone, email, status
--   Audit trio: created_at, updated_at, updated_by
-- );

-- [VEHICLES]
-- TODO: CREATE TABLE vehicles (
--   PK: vehicle_id
--   Business fields: plate_number (UNIQUE), vehicle_type, capacity DECIMAL, status (available/maintenance)
--   Audit trio: created_at, updated_at, updated_by
-- );

-- [BRANCHES]
-- TODO: CREATE TABLE branches (
--   PK: branch_id
--   Business fields: branch_name, address (TEXT), city, contact_person, phone
--   Audit trio: created_at, updated_at, updated_by
-- );

-- NOTES:
-- • Keep foreign keys OUT of core tables. Relationships live in transaction tables (Phase C/E).
-- • Use snake_case; PK names = <table>_id; include DEFAULTs for numeric/text as appropriate.
-- • After implementing, run on a clean DB before pushing (see docs/runbook.md).

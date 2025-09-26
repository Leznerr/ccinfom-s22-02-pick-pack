-- PHASE B — EXCEPTION/EDGE SEEDS (≥3 per core) to exercise rules during demos.
-- These are intentionally odd/edge values to validate UI/DB checks later.
USE ccinfom_dev;

-- [PRODUCTS] (≥3)
-- TODO: Inactive product (active_flag = FALSE)
-- TODO: Very high/low unit_price or long name to test UI lengths
-- TODO: (Optional) Prepare a would-be duplicate SKU case (document expectation: UNIQUE violation)
INSERT INTO products
  (sku, product_name, category, unit_price, unit_of_measure, on_hand_qty, reserved_qty, active_flag, updated_by)
VALUES
('PRD-EXC1', 'Inactive Gadget', 'Gadgets', 150.00, 'pcs', 10, 0, FALSE, 'seed'), -- inactive
('PRD-EXC2', REPEAT('LongName', 10), 'Tools', 1.00, 'pcs', 5, 0, TRUE, 'seed'),  -- long name stress test
('PRD-EXC3', 'Duplicate SKU', 'Widgets', 99.99, 'pcs', 20, 0, TRUE, 'seed');     -- prepare duplicate, second insert will fail

-- [CUSTOMERS] (≥3)
-- TODO: Missing contact_person OR malformed phone/email (to see how UI handles)
INSERT INTO customers
  (customer_name,     contact_person, phone,   email,                 default_delivery_address,           updated_by)
VALUES
('Dormant Trading',    'Unknown',  NULL,     NULL,                  'Unknown address',                 'seed'), -- no phone/email
('Test Minimal Co.',   NULL,       '0000',  'info@test.local',     'No. 1 Test Rd, Test City, MM',     'seed'),  -- placeholder phone
('Retail Shell',       'TBD',      NULL,    'contact@retail.sh',   'TBD Address, TBD City, MM',        'seed');  -- no phone

-- [EMPLOYEES] (≥3)
-- TODO: status = 'inactive' OR unexpected role to test controlled values
INSERT INTO employees
  (last_name, first_name, employee_role, phone, email, employee_status, updated_by)
VALUES
  ('Test',     'NoRole',   NULL,       '09999999999', 'norole@example.com',   'active',    'seed'),   -- Missing role
  ('Duplicate','Email',    'picker',   '09998887777', 'miguel.santos@example.com', 'active','seed'),  -- Duplicate email
  ('Invalid',  'Status',   'packer',   '09110001111', 'invalid.status@example.com', 'on-leave','seed'); -- Invalid status

-- [VEHICLES] (≥3)
-- TODO: status = 'maintenance' OR capacity = 0
INSERT INTO vehicles (vehicle_id, plate_number, Vehicle_type, Vehicle_capacity, statos, updated_by) VALUES
	(12,'DCP-123','motorcycle',3.0,'maintenance','admin'),
    (13,'DCP-124','van',0,'maintenance','admin'),
    (14,'DCP-125','truck',6.7,'maintenance','admin')
    
-- [BRANCHES] (≥3)
-- TODO: Missing contact or phone OR generic address
INSERT INTO branches
  (branch_name,     address,     city,      contact_person, phone,       updated_by)
VALUES
  ('Dormant Branch','Unknown',   'Unknown', NULL,           NULL,        'seed'),  -- no contact or phone
  ('Unmanned Site', 'Lot 12',    'Pasig',   NULL,           '028000011', 'seed'),  -- no contact_person
  ('Phone Pending', 'Block 3',   'Makati',  'Coordinator',  NULL,        'seed');  -- no phone
  
  
-- NOTE:
-- • Exceptions need not count toward the “≥10 normal” target.
-- • Document any intended constraint errors in docs/decisions.md (what should fail vs pass).

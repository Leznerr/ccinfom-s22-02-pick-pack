-- PHASE B — EXCEPTION/EDGE SEEDS (≥3 per core) to exercise rules during demos.
-- These are intentionally odd/edge values to validate UI/DB checks later.
USE ccinfom_dev;

-- [PRODUCTS] (≥3)
-- TODO: Inactive product (active_flag = FALSE)
-- TODO: Very high/low unit_price or long name to test UI lengths
-- TODO: (Optional) Prepare a would-be duplicate SKU case (document expectation: UNIQUE violation)

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
  (last_name, first_name, role, phone, email, status, updated_by)
VALUES
  ('Test',     'NoRole',   NULL,       '09999999999', 'norole@example.com',   'active',    'seed'),   -- Missing role
  ('Duplicate','Email',    'picker',   '09998887777', 'miguel.santos@example.com', 'active','seed'),  -- Duplicate email
  ('Invalid',  'Status',   'packer',   '09110001111', 'invalid.status@example.com', 'on-leave','seed'); -- Invalid status

-- [VEHICLES] (≥3)
-- TODO: status = 'maintenance' OR capacity = 0

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

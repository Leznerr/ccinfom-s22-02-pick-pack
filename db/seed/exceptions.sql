USE ccinfom_dev;


INSERT INTO products
  (sku, product_name, category, unit_price, unit_of_measure, on_hand_qty, reserved_qty, active_flag, updated_by)
VALUES
('PRD-EXC1', 'Inactive Gadget', 'Gadgets', 150.00, 'pcs', 10, 0, FALSE, 'seed'), -- inactive
('PRD-EXC2', REPEAT('LongName', 10), 'Tools', 1.00, 'pcs', 5, 0, TRUE, 'seed'),  -- long name stress test
('PRD-EXC3', 'Widget OOS', 'Widgets', 99.99, 'pcs', 0, 0, TRUE, 'seed');     -- out of stock


INSERT INTO customers
(customer_name,        contact_person, phone,         email,                     default_delivery_address,   updated_by)
VALUES
('Dormant Trading',    NULL,           NULL,          NULL,                      'Unknown address',          'seed'), -- all contact fields null
('Retail Shell',       'TBD',          NULL,          'contact@retail.sh',       'TBD Address, City',        'seed'), -- null phone
('Test Minimal Co.',   'Placeholder',  '09190000001', 'info@test.local',         'No.1 Test Rd, Test City',  'seed'), -- minimal but valid phone
('Edge Unlimited',     NULL,           '09190000002', NULL,                      'Somewhere, Metro Manila',  'seed'); -- null email

INSERT INTO employees
(last_name,  first_name,   employee_role, phone,        email,                       employee_status, updated_by)
VALUES
('Qc','Edgecase','picker','09170000001','qc.edge1@example.com','inactive','seed'),       -- inactive
('Long','NamewithManyChars','packer','09170000002','qc.edge2@example.com','active','seed'),
('O''Connor','Jean-Paul','dispatcher','09170000003',NULL,'active','seed');               -- NULL email allowed by UNIQUE

INSERT INTO vehicles
(plate_number, vehicle_type, capacity, vehicle_status, updated_by)
VALUES
('DCP-1234','motorcycle',3.00,'maintenance','seed'),   -- maintenance
('DCP-1235','van',0.00,'maintenance','seed'),          -- zero capacity
('DCP-1236','truck',6.70,'inactive','seed');           -- inactive
    
INSERT INTO branches
(branch_name,      address,     city,       contact_person, phone,        updated_by)
VALUES
('Dormant Branch', 'Unknown',   'Unknown',  NULL,           NULL,         'seed'),    -- no contact/phone
('Unmanned Site',  'Lot 12',    'Pasig',    NULL,           '09190001111','seed'),    -- null contact, valid phone
('Phone Pending',  'Block 3',   'Makati',   'Coordinator',  NULL,         'seed'),    -- no phone
('Long Name Hub',  REPEAT('Addr-',10), 'Taguig', NULL,      '09190002222','seed');    -- long address, valid phone
  
  
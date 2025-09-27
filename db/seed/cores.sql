-- PHASE B — NORMAL SEEDS (add real-looking data).
-- DoD: ≥10 rows per core table (products, customers, employees, vehicles, branches).
-- Write INSERTs later. For now, list what you will cover:

/* =========================
   PRODUCTS (12 normal rows)
   ========================= */
INSERT INTO products
(sku, product_name, category, unit_price, unit_of_measure, on_hand_qty, reserved_qty, active_flag, updated_by)
VALUES
('WGT-1001','Widget A','Widgets',120.00,'pcs',500,0,TRUE,'seed'),
('WGT-1002','Widget B','Widgets', 95.50,'pcs',300,0,TRUE,'seed'),
('GDT-2001','Gadget Alpha','Gadgets',550.00,'pcs',100,0,TRUE,'seed'),
('GDT-2002','Gadget Beta','Gadgets',750.00,'pcs', 80,0,TRUE,'seed'),
('TOL-3001','Tool Hammer','Tools',250.00,'pcs',150,0,TRUE,'seed'),
('TOL-3002','Tool Screwdriver','Tools', 75.00,'pcs',400,0,TRUE,'seed'),
('ACC-4001','Accessory Cable','Accessories', 45.00,'pcs',1000,0,TRUE,'seed'),
('ACC-4002','Accessory Charger','Accessories',180.00,'pcs', 200,0,TRUE,'seed'),
('WGT-1003','Widget C','Widgets',130.00,'pcs',220,0,TRUE,'seed'),
('GDT-2003','Gadget Gamma','Gadgets',680.00,'pcs', 60,0,TRUE,'seed'),
('TOL-3003','Tool Wrench','Tools', 90.00,'pcs', 220,0,TRUE,'seed'),
('ACC-4003','Accessory Mount','Accessories',120.00,'pcs',140,0,TRUE,'seed');

/* =========================
   CUSTOMERS (12 normal rows)
   ========================= */
INSERT INTO customers (customer_name, contact_person, phone, email, default_delivery_address, updated_by) VALUES
('Apex Retail Corp','Maria Tan','09171230001','ops@apexretail.ph','6754 Ayala Ave, Makati','seed'),
('Bayview Trading Inc','Rafael Cruz','09181230002','sales@bayview.ph','35 Roxas Blvd, Pasay','seed'),
('Crown Merchants Co','Lara Go','09191230003','help@crownmerchants.ph','22 Pioneer St, Mandaluyong','seed'),
('Delta Hardware Supply','Nico Reyes','09201230004','orders@deltahw.ph','101 Boni Ave, Mandaluyong','seed'),
('Everest Distribution','Ivy Lim','09211230005','support@everestdist.ph','194 Ortigas Ave, Pasig','seed'),
('Frontline Foods MNL','Pia Ramos','09221230006','contact@frontlinefoods.ph','9 EDSA, Quezon City','seed'),
('Greenfields Pharma','Jose Co','09231230007','service@greenfields.ph','88 Timog Ave, Quezon City','seed'),
('Island Apparel','Rina Chua','09241230008','ops@islandapparel.ph','18 Jupiter St, Makati','seed'),
('Jade Homeware','Alex Yu','09251230009','orders@jadehome.ph','15 Katipunan Ave, Quezon City','seed'),
('Keystone Electronics','Ben Ong','09261230010','sales@keystone.ph','31 Shaw Blvd, Mandaluyong','seed'),
('Lighthouse Books','Seth Dy','09271230011','hello@lighthouse.ph','12 Recto Ave, Manila','seed'),
('MetroFresh Grocers','Ella Tan','09281230012','buy@metrofresh.ph','75 Boni Ave, Mandaluyong','seed');

/* =========================
   EMPLOYEES (12 normal rows; all active)
   ========================= */
INSERT INTO employees (last_name, first_name, employee_role, phone, email, employee_status, updated_by)
VALUES
('Santos','Miguel','picker','09171234567','miguel.santos@example.com','active','seed'),
('Reyes','Ana','packer','09183456789','ana.reyes@example.com','active','seed'),
('Dela Cruz','Juan','dispatcher','09281234567','juan.delacruz@example.com','active','seed'),
('Garcia','Liza','picker','09391234567','liza.garcia@example.com','active','seed'),
('Fernandez','Carlos','packer','09184561234','carlos.fernandez@example.com','active','seed'),
('Mendoza','Rosa','dispatcher','09274561234','rosa.mendoza@example.com','active','seed'),
('Lopez','Marco','picker','09173456721','marco.lopez@example.com','active','seed'),
('Torres','Elena','packer','09283456721','elena.torres@example.com','active','seed'),
('Cruz','Paolo','dispatcher','09194567890','paolo.cruz@example.com','active','seed'),
('Aquino','Julia','picker','09383456721','julia.aquino@example.com','active','seed'),
('Bautista','Noel','dispatcher','09178880001','noel.bautista@example.com','active','seed'),
('Go','Henry','picker','09178880002','henry.go@example.com','active','seed');

/* =========================
   VEHICLES (12 normal rows; all available)
   ========================= */
INSERT INTO vehicles
(plate_number, vehicle_type, capacity, vehicle_status, updated_by)
VALUES
('ABC-1234','van',12.00,'available','seed'),
('DEF-5678','truck', 5.00,'available','seed'),
('GHI-9012','motorcycle',2.00,'available','seed'),
('JKL-3456','van',15.00,'available','seed'),
('MNO-7890','truck', 5.50,'available','seed'),
('PQR-2345','motorcycle',2.00,'available','seed'),
('STU-6789','van', 7.00,'available','seed'),
('VWX-0123','truck', 4.50,'available','seed'),
('YZA-4567','motorcycle',2.00,'available','seed'),
('BCD-8901','van',10.00,'available','seed'),
('EFG-1235','truck', 4.00,'available','seed'),
('HIJ-4568','motorcycle',2.00,'available','seed');

/* =========================
   BRANCHES (12 normal rows)
   ========================= */
INSERT INTO branches (branch_name, address, city, contact_person, phone, updated_by) VALUES
('North Hub QC','101 North Ave','Quezon City','Leo Ramos','09190010001','seed'),
('East Depot Pasig','55 C-5 Road','Pasig','Mia Santos','09190010002','seed'),
('South Hub Muntinlupa','77 Alabang-Zapote Rd','Muntinlupa','Iris Lim','09190010003','seed'),
('West Depot Manila','90 Abad Santos Ave','Manila','Owen Cruz','09190010004','seed'),
('Central Hub Makati','12 Ayala Ave','Makati','Paula Tan','09190010005','seed'),
('Shaw Crossdock','122 Shaw Blvd','Mandaluyong','Ken Lee','09190010006','seed'),
('Taguig Dispatch','3rd Ave BGC','Taguig','Kay Uy','09190010007','seed'),
('Pasay Satellite','230 Taft Ave','Pasay','Rico Chua','09190010008','seed'),
('Parañaque Satellite','19 Sucat Rd','Parañaque','Nina Dee','09190010009','seed'),
('Caloocan Yard','800 Rizal Ave','Caloocan','Gio Te','09190010010','seed'),
('Marikina Yard','55 JP Rizal','Marikina','Hera Dy','09190010011','seed'),
('Valenzuela Depot','99 McArthur Hwy','Valenzuela','Ken Yu','09190010012','seed');


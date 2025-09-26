-- PHASE B — NORMAL SEEDS (add real-looking data).
-- DoD: ≥10 rows per core table (products, customers, employees, vehicles, branches).
-- Write INSERTs later. For now, list what you will cover:

-- [PRODUCTS] (≥10)
-- TODO: Add diverse SKUs across categories (Widget/Gadget/Tool/Accessory); set on_hand_qty and unit_price realistically.

-- [CUSTOMERS] (≥10)
-- TODO: Add company names with contact_person, phone, email, and delivery addresses from major Metro Manila cities.

-- [EMPLOYEES] (≥10)
-- TODO: Mix roles: picker/packer/dispatcher; status mostly 'active'; valid phones/emails.
INSERT INTO employees (last_name, first_name, employee_role, phone, email, employee_status, updated_by)
VALUES
('Santos', 'Miguel', 'picker', '09171234567', 'miguel.santos@example.com', 'active', 'seed'),
('Reyes', 'Ana', 'packer', '09183456789', 'ana.reyes@example.com', 'active', 'seed'),
('Dela Cruz', 'Juan', 'dispatcher', '09281234567', 'juan.delacruz@example.com', 'active', 'seed'),
('Garcia', 'Liza', 'picker', '09391234567', 'liza.garcia@example.com', 'active', 'seed'),
('Fernandez', 'Carlos', 'packer', '09184561234', 'carlos.fernandez@example.com', 'active', 'seed'),
('Mendoza', 'Rosa', 'dispatcher', '09274561234', 'rosa.mendoza@example.com', 'active', 'seed'),
('Lopez', 'Marco', 'picker', '09173456721', 'marco.lopez@example.com', 'active', 'seed'),
('Torres', 'Elena', 'packer', '09283456721', 'elena.torres@example.com', 'inactive', 'seed'),
('Cruz', 'Paolo', 'dispatcher', '09194567890', 'paolo.cruz@example.com', 'active', 'seed'),
('Aquino', 'Julia', 'picker', '09383456721', 'julia.aquino@example.com', 'active', 'seed');

-- [VEHICLES] (≥10)
-- TODO: Mix vehicle_type (van/truck/motorcycle); capacities vary; status mostly 'available'.
INSERT INTO vehicles (vehicle_id, plate_number, Vehicle_type, Vehicle_capacity, statos, updated_by) VALUES
	(0,'ABC-123', 'van', 12.0, 'available', 'admin'),
	(1,'DEF-456', 'truck', 3.0, 'available', 'admin'),
	(2,'GHI-789', 'motorcycle', 2.0, 'available', 'admin'),
	(3,'JKL-012', 'van', 15.0, 'available', 'admin'),
	(4,'MNO-345', 'truck', 5.0, 'maintenance', 'admin'),
	(5,'PQR-678', 'motorcycle', 2.0, 'available', 'admin'),
	(6,'STU-901', 'van', 7.0, 'available', 'admin'),
	(7,'VWX-234', 'truck', 5.0, 'available', 'admin'),
	(8,'YZA-567', 'motorcycle', 2.0, 'available', 'admin'),
	(9,'BCD-890', 'van', 10.0, 'available', 'admin'),
	(10,'EFG-123', 'truck', 4.0, 'available', 'admin'),
	(11,'HIJ-456', 'motorcycle', 2.0, 'rented', 'admin');

-- [BRANCHES] (≥10)
-- TODO: Branch names + address + city + contact_person + phone.

-- REMINDERS:
-- • Use consistent formats (phones, emails).
-- • Keep IDs implicit (AUTO_INCREMENT); do not hard-code unless necessary for demos.

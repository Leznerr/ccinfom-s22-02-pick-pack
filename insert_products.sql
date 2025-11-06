USE ccinfom_dev;
INSERT INTO products (sku, product_name, category, unit_price, unit_of_measure, on_hand_qty, reserved_qty, active_flag, updated_by)
VALUES
('PHASEE-0013','PhaseE Product 13','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0014','PhaseE Product 14','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0015','PhaseE Product 15','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0016','PhaseE Product 16','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0017','PhaseE Product 17','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0018','PhaseE Product 18','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0019','PhaseE Product 19','Phase E',120.00,'pcs',500,0,TRUE,'phaseE'),
('PHASEE-0020','PhaseE Product 20','Phase E',120.00,'pcs',500,0,TRUE,'phaseE');
SELECT product_id, sku FROM products WHERE product_id BETWEEN 13 AND 20;

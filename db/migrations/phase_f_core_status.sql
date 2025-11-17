USE ccinfom_dev;

-- Add status columns to customers and branches so core records can be deactivated.
ALTER TABLE customers
  ADD COLUMN customer_status ENUM('active','inactive') NOT NULL DEFAULT 'active' AFTER default_delivery_address;

ALTER TABLE branches
  ADD COLUMN branch_status ENUM('active','inactive') NOT NULL DEFAULT 'active' AFTER phone;

-- Ensure existing rows are marked active.
UPDATE customers SET customer_status = 'active' WHERE customer_status IS NULL;
UPDATE branches  SET branch_status = 'active'  WHERE branch_status IS NULL;

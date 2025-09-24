-- PHASE B — VALIDATION (COUNTS & BASIC SANITY).
-- Replace with actual SELECTs later; for now, capture what we will assert.

-- COUNTS (DoD): Expect ≥10 rows per core
-- TODO: SELECT COUNT(*) FROM products;
-- TODO: SELECT COUNT(*) FROM customers;
-- TODO: SELECT COUNT(*) FROM employees;
-- TODO: SELECT COUNT(*) FROM vehicles;
-- TODO: SELECT COUNT(*) FROM branches;

-- OPTIONAL QUICK CHECKS (after seeding)
-- TODO: SELECT DISTINCT role FROM employees;           -- expect picker/packer/dispatcher
-- TODO: SELECT COUNT(*) FROM products WHERE active_flag = FALSE;  -- exceptions present

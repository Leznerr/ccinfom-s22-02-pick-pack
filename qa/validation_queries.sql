USE ccinfom_dev;

-- ===========================================================
-- GATE A: Row counts (DoD ≥10; high bar ≥13 with exceptions)
-- ===========================================================
SELECT 'products'  AS table_name, COUNT(*) AS rows_count,
       IF(COUNT(*) >= 10,'OK','FAIL') AS meets_min_10,
       IF(COUNT(*) >= 13,'OK','—')   AS meets_high_bar_13
FROM products
UNION ALL
SELECT 'customers', COUNT(*), IF(COUNT(*) >= 10,'OK','FAIL'), IF(COUNT(*) >= 13,'OK','—') FROM customers
UNION ALL
SELECT 'branches',  COUNT(*), IF(COUNT(*) >= 10,'OK','FAIL'), IF(COUNT(*) >= 13,'OK','—') FROM branches
UNION ALL
SELECT 'employees', COUNT(*), IF(COUNT(*) >= 10,'OK','FAIL'), IF(COUNT(*) >= 13,'OK','—') FROM employees
UNION ALL
SELECT 'vehicles',  COUNT(*), IF(COUNT(*) >= 10,'OK','FAIL'), IF(COUNT(*) >= 13,'OK','—') FROM vehicles
ORDER BY table_name;

-- ===========================================================
-- GATE B: Audit trio spot checks (non-NULL, auto-filled)
-- (Sample first 5 of each core)
-- ===========================================================
SELECT product_id  AS id, 'products'  AS tbl, created_at, updated_at, updated_by FROM products  ORDER BY product_id  LIMIT 5;
SELECT customer_id AS id, 'customers' AS tbl, created_at, updated_at, updated_by FROM customers ORDER BY customer_id LIMIT 5;
SELECT branch_id   AS id, 'branches'  AS tbl, created_at, updated_at, updated_by FROM branches  ORDER BY branch_id   LIMIT 5;
SELECT employee_id AS id, 'employees' AS tbl, created_at, updated_at, updated_by FROM employees ORDER BY employee_id LIMIT 5;
SELECT vehicle_id  AS id, 'vehicles'  AS tbl, created_at, updated_at, updated_by FROM vehicles  ORDER BY vehicle_id  LIMIT 5;

-- ===========================================================
-- GATE C: Domain sanity (should return ZERO rows on clean data)
-- ===========================================================
-- Products: non-negative & reserved ≤ on_hand
SELECT product_id, sku, unit_price, on_hand_qty, reserved_qty
FROM products
WHERE unit_price < 0 OR on_hand_qty < 0 OR reserved_qty < 0 OR reserved_qty > on_hand_qty;

-- Customers: email pattern (NULL ok)
SELECT customer_id, email FROM customers
WHERE email IS NOT NULL AND email NOT LIKE '%@%';

-- Branches: mandatory fields present
SELECT branch_id, branch_name, address, city FROM branches
WHERE address IS NULL OR city IS NULL;

-- Vehicles: capacity non-negative (0 is allowed), valid enum enforced by DDL
SELECT vehicle_id, plate_number, capacity FROM vehicles
WHERE capacity < 0;

-- Phones (present values must be PH mobile 11-digit 09xxxxxxxxx)
SELECT 'customers' AS tbl, customer_id AS id, phone FROM customers
WHERE phone IS NOT NULL AND phone NOT REGEXP '^09[0-9]{9}$'
UNION ALL
SELECT 'employees', employee_id, phone FROM employees
WHERE phone NOT REGEXP '^09[0-9]{9}$'
UNION ALL
SELECT 'branches', branch_id, phone FROM branches
WHERE phone IS NOT NULL AND phone NOT REGEXP '^09[0-9]{9}$';

-- Uniqueness audits (should be empty/zero-delta)
SELECT sku, COUNT(*) c FROM products GROUP BY sku HAVING c > 1;
SELECT email, COUNT(*) c FROM employees GROUP BY email HAVING email IS NOT NULL AND c > 1;
SELECT plate_number, COUNT(*) c FROM vehicles GROUP BY plate_number HAVING c > 1;

-- Optional: name-based fuzzy dup for customers (informational)
SELECT LOWER(TRIM(customer_name)) AS normalized_name, COUNT(*) AS dup_count
FROM customers
GROUP BY LOWER(TRIM(customer_name))
HAVING COUNT(*) > 1;

-- ===========================================================
-- GATE D: Surface intended EXCEPTIONS (edges you seeded)
-- (These may be non-empty by design; if empty, that's fine.)
-- ===========================================================
-- Products: inactive
SELECT product_id, sku, active_flag
FROM products
WHERE active_flag = FALSE;

-- Products: suspicious reserves (may be empty depending on seeds)
SELECT product_id, sku, on_hand_qty, reserved_qty
FROM products
WHERE reserved_qty > on_hand_qty;

-- Customers: missing any contact channel (NULLs)
SELECT customer_id, customer_name, contact_person, phone, email
FROM customers
WHERE contact_person IS NULL OR phone IS NULL OR email IS NULL
ORDER BY customer_id;

-- Branches: missing contact fields (NULLs)
SELECT branch_id, branch_name, contact_person, phone
FROM branches
WHERE contact_person IS NULL OR phone IS NULL
ORDER BY branch_id;

-- Vehicles: non-available states (maintenance/inactive)
SELECT vehicle_id, plate_number, vehicle_status, capacity
FROM vehicles
WHERE vehicle_status <> 'available'
ORDER BY vehicle_id;

-- ===========================================================
-- EMPLOYEES QA (kept for clarity; overlaps Gate A/B/C)
-- ===========================================================
SELECT 'employees' AS table_name, COUNT(*) AS rows_count,
       IF(COUNT(*) >= 10,'OK','FAIL') AS meets_min_10
FROM employees;

-- Domain sanity (should be zero due to ENUM/NOT NULL)
SELECT employee_id, employee_role FROM employees
WHERE employee_role NOT IN ('picker','packer','dispatcher');

SELECT employee_id, employee_status FROM employees
WHERE employee_status NOT IN ('active','inactive');

-- Exceptions surfaced (edges you inserted)
SELECT employee_id, last_name, first_name, email, employee_role, employee_status
FROM employees
WHERE email IS NULL OR employee_status = 'inactive'
ORDER BY employee_id;



/* ==========================================
   PHASE C — QA EXTENSIONS (T1/T2)
   Append after Phase B gates.

   ROW COUNTS (all should be ≥ 1 after seeds)
   [ ] SELECT COUNT(*) FROM pick_ticket_hdr;
   [ ] SELECT COUNT(*) FROM pick_ticket_line;
   [ ] SELECT COUNT(*) FROM picking_hdr;
   [ ] SELECT COUNT(*) FROM picking_line;

   ORPHANS (should be zero)
   [ ] pick_ticket_line without header
   [ ] picking_line without header
   [ ] headers referencing missing customer/branch/product/employee

   INVENTORY INVARIANTS
   [ ] on_hand unchanged by T2 (no UPDATE to products.on_hand_qty during T2)
   [ ] Δreserved_qty BY product = SUM(picked_qty) from picking_line
   [ ] No product where reserved_qty > on_hand_qty
   [ ] Per ticket_line: SUM(picked_qty) ≤ requested_qty  -- requires ticket_line_id on picking_line

   BUSINESS/STATUS
   [ ] UNIQUE pairs hold: (pick_ticket_id,product_id) and (picking_id,ticket_line_id)
   [ ] Tickets with a picking_hdr have ticket_status = 'Picking'
   [ ] Picked SKU exists on ticket line (product match)  -- anti-join should return 0

   PERFORMANCE VIS
   [ ] EXPLAIN joins use indexes: ticket→lines→products, picking→lines

   DEFINITION OF DONE (QA)
   [ ] All checks return expected results; no orphans; all invariants pass
========================================== */


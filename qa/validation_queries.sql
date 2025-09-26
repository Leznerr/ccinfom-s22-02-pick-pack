USE ccinfom_dev;

-- ===========================================================
-- GATE A: Row counts (DoD ≥10; high bar ≥13 with exceptions)
-- ===========================================================
SELECT 'customers' AS table_name,
       COUNT(*)    AS rows_count,
       CASE WHEN COUNT(*) >= 10 THEN 'OK' ELSE 'FAIL' END AS meets_min_10,
       CASE WHEN COUNT(*) >= 13 THEN 'OK' ELSE '—'   END AS meets_high_bar_13
FROM customers
UNION ALL
SELECT 'branches',
       COUNT(*),
       CASE WHEN COUNT(*) >= 10 THEN 'OK' ELSE 'FAIL' END,
       CASE WHEN COUNT(*) >= 13 THEN 'OK' ELSE '—'   END
FROM branches;

-- ===========================================================
-- GATE B: Audit trio spot checks (non-NULL, auto-filled)
-- ===========================================================
SELECT customer_id AS id, created_at, updated_at, updated_by
FROM customers
ORDER BY customer_id
LIMIT 5;

SELECT branch_id AS id, created_at, updated_at, updated_by
FROM branches
ORDER BY branch_id
LIMIT 5;

-- ===========================================================
-- GATE C: Domain sanity (should return ZERO rows on clean data)
-- (Matches your Phase-B DDL: customers.email is NULL or contains '@';
--  address/city are NOT NULL in branches.)
-- ===========================================================
-- Invalid customer emails (should be none)
SELECT customer_id, email
FROM customers
WHERE email IS NOT NULL AND email NOT LIKE '%@%';

-- Missing mandatory branch fields (should be none due to NOT NULL)
SELECT branch_id, branch_name, address, city
FROM branches
WHERE address IS NULL OR city IS NULL;

-- Optional (informational): potential duplicate customers by name
SELECT LOWER(TRIM(customer_name)) AS normalized_name, COUNT(*) AS dup_count
FROM customers
GROUP BY LOWER(TRIM(customer_name))
HAVING COUNT(*) > 1;

-- ===========================================================
-- GATE D: Surface intended EXCEPTIONS (should list rows you seeded)
-- These do not fail Phase B; they prove your edge seeds exist.
-- ===========================================================
-- Customers missing any contact channel
SELECT customer_id, customer_name, contact_person, phone, email
FROM customers
WHERE contact_person IS NULL OR phone IS NULL OR email IS NULL
ORDER BY customer_id;

-- Branches missing contact fields
SELECT branch_id, branch_name, contact_person, phone
FROM branches
WHERE contact_person IS NULL OR phone IS NULL
ORDER BY branch_id;




-- ===========================================================
-- EMPLOYEES QA
-- ===========================================================

-- Row count (≥10 normal expected)
SELECT 'employees' AS table_name,
       COUNT(*) AS rows_count,
       CASE WHEN COUNT(*) >= 10 THEN 'OK' ELSE 'FAIL' END AS meets_min_10
FROM employees;

-- Audit trio check
SELECT employee_id, created_at, updated_at, updated_by
FROM employees
ORDER BY employee_id
LIMIT 5;

-- Domain sanity
SELECT employee_id, employee_role
FROM employees
WHERE employee_role NOT IN ('picker','packer','dispatcher');

SELECT employee_id, employee_status
FROM employees
WHERE employee_status NOT IN ('active','inactive');

-- Exceptions surfaced
SELECT employee_id, last_name, first_name, email, employee_role, employee_status
FROM employees
WHERE email IS NULL 
   OR employee_role IS NULL 
   OR employee_status NOT IN ('active','inactive');

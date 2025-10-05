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

/* ==========================================
   PHASE C — QA EXTENSIONS (T1 AND T2)
   ========================================== */

-- ROW COUNTS: Expect >=1 after seeds
SELECT 'pick_ticket_hdr' AS table_name, COUNT(*) AS rows_count FROM pick_ticket_hdr;
SELECT 'pick_ticket_line' AS table_name, COUNT(*) AS rows_count FROM pick_ticket_line;

-- ORPHANS: Lines without valid header (should return 0 rows)
SELECT line.ticket_line_id, line.pick_ticket_id
FROM pick_ticket_line AS line
LEFT JOIN pick_ticket_hdr AS hdr ON line.pick_ticket_id = hdr.pick_ticket_id
WHERE hdr.pick_ticket_id IS NULL;

-- ORPHANS: Headers referencing missing customers/branches (should return 0 rows)
SELECT hdr.pick_ticket_id, hdr.customer_id, hdr.branch_id
FROM pick_ticket_hdr AS hdr
LEFT JOIN customers AS c ON hdr.customer_id = c.customer_id
LEFT JOIN branches  AS b ON hdr.branch_id = b.branch_id
WHERE c.customer_id IS NULL OR b.branch_id IS NULL;

-- BUSINESS/STATUS RULES
-- UNIQUE guard: verify no duplicates (pick_ticket_id + product_id)
SELECT pick_ticket_id, product_id, COUNT(*) AS dup_count
FROM pick_ticket_line
GROUP BY pick_ticket_id, product_id
HAVING COUNT(*) > 1;

-- REQUESTED_QTY sanity: should all be > 0
SELECT ticket_line_id, requested_qty
FROM pick_ticket_line
WHERE requested_qty <= 0;

-- LINE_PRODUCT validity: every line must reference an existing product (should return 0 rows)
SELECT line.ticket_line_id, line.product_id
FROM pick_ticket_line AS line
LEFT JOIN products AS p ON line.product_id = p.product_id
WHERE p.product_id IS NULL;



-- ===================================================================
-- T2-GATE A — Row counts (expect ≥1 after seeding tx-T2.sql)
-- ===================================================================
SELECT 'picking_hdr'  AS table_name, COUNT(*) AS rows_count FROM picking_hdr
UNION ALL
SELECT 'picking_line', COUNT(*) FROM picking_line;

SELECT picking_status, COUNT(*) AS cnt FROM picking_hdr GROUP BY picking_status;

-- ===================================================================
-- T2-GATE B — Referential integrity / Orphans (expect ZERO rows)
-- FK constraints should already prevent these; we surface any defect.
-- ===================================================================
-- picking_line has a header
SELECT pl.picking_line_id
FROM picking_line pl
LEFT JOIN picking_hdr ph ON ph.picking_id = pl.picking_id
WHERE ph.picking_id IS NULL;

-- picking_line references an existing ticket line
SELECT pl.picking_line_id
FROM picking_line pl
LEFT JOIN pick_ticket_line tl ON tl.ticket_line_id = pl.ticket_line_id
WHERE tl.ticket_line_id IS NULL;

-- picking_hdr references existing ticket & employee
SELECT ph.picking_id
FROM picking_hdr ph
LEFT JOIN pick_ticket_hdr h ON h.pick_ticket_id = ph.pick_ticket_id
LEFT JOIN employees e       ON e.employee_id   = ph.picker_employee_id
WHERE h.pick_ticket_id IS NULL OR e.employee_id IS NULL;

-- ===================================================================
-- T2-GATE C — Business rules (expect ZERO rows)
-- ===================================================================
-- C1) Picked SKU must match the ticket line’s product
SELECT pl.picking_line_id, pl.product_id AS picked_product, tl.product_id AS ticket_product
FROM picking_line pl
JOIN pick_ticket_line tl ON tl.ticket_line_id = pl.ticket_line_id
WHERE pl.product_id <> tl.product_id;

-- C2) Ticket line must belong to the SAME ticket as the picking header
SELECT pl.picking_line_id, ph.pick_ticket_id AS picking_ticket, tl.pick_ticket_id AS line_ticket
FROM picking_line pl
JOIN picking_hdr ph  ON ph.picking_id      = pl.picking_id
JOIN pick_ticket_line tl ON tl.ticket_line_id = pl.ticket_line_id
WHERE ph.pick_ticket_id <> tl.pick_ticket_id;

-- C3) Over-pick guard: SUM(picked_qty) per ticket_line ≤ requested_qty
SELECT tl.ticket_line_id, tl.requested_qty,
       COALESCE(SUM(pl.picked_qty),0) AS picked_qty
FROM pick_ticket_line tl
LEFT JOIN picking_line pl ON pl.ticket_line_id = tl.ticket_line_id
GROUP BY tl.ticket_line_id, tl.requested_qty
HAVING COALESCE(SUM(pl.picked_qty),0) > tl.requested_qty;

-- C4) Duplicate protection at app level (should be zero; DB has UNIQUE too)
SELECT picking_id, ticket_line_id, COUNT(*) AS dup_count
FROM picking_line
GROUP BY picking_id, ticket_line_id
HAVING COUNT(*) > 1;

-- ===================================================================
-- T2-GATE D — Inventory invariants (expect ZERO rows in Phase C)
-- In Phase C (before Close), reserved_qty should equal SUM of picked_qty.
-- ===================================================================
-- D1) Per product: reserved_qty = SUM(picked_qty)
WITH s AS (
  SELECT product_id, SUM(picked_qty) AS picked_sum
  FROM picking_line
  GROUP BY product_id
)
SELECT p.product_id, p.reserved_qty, COALESCE(s.picked_sum,0) AS picked_sum,
       (p.reserved_qty - COALESCE(s.picked_sum,0)) AS delta
FROM products p
LEFT JOIN s ON s.product_id = p.product_id
WHERE p.reserved_qty <> COALESCE(s.picked_sum,0);

-- D2) No product where reserved exceeds on_hand (should be zero)
SELECT product_id, on_hand_qty, reserved_qty
FROM products
WHERE reserved_qty > on_hand_qty;


-- ===================================================================
-- T2-GATE E — Status transitions (expect ZERO rows in Phase C)
-- Tickets that have a picking_hdr should no longer be 'Open'
-- (Typically they are 'Picking' until Packed in T3.)
-- ===================================================================
SELECT h.pick_ticket_id, h.ticket_status
FROM pick_ticket_hdr h
JOIN picking_hdr ph ON ph.pick_ticket_id = h.pick_ticket_id
WHERE h.ticket_status = 'Open';

-- ===================================================================
-- T2-GATE F — Picking completion consistency (expect ZERO rows)
-- If SUM(requested) == SUM(picked) for a picking_hdr, status must be 'Done'.
-- If SUM differs, status must NOT be 'Done'.
-- ===================================================================
WITH req_vs_got AS (
  SELECT ph.picking_id, ph.picking_status,
         SUM(tl.requested_qty)                         AS req,
         COALESCE(SUM(pl.picked_qty),0)                AS got
  FROM picking_hdr ph
  JOIN pick_ticket_line tl ON tl.pick_ticket_id = ph.pick_ticket_id
  LEFT JOIN picking_line pl ON pl.picking_id = ph.picking_id
                           AND pl.ticket_line_id = tl.ticket_line_id
  GROUP BY ph.picking_id, ph.picking_status
)
-- Mismatch set (should return zero)
SELECT picking_id, picking_status, req, got
FROM req_vs_got
WHERE (req = got    AND picking_status <> 'Done')
   OR (req <> got   AND picking_status  = 'Done');
   
   
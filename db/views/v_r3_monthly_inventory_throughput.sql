/* ============================================================================
   Phase F - R3: Monthly Return Cost & Shortage Exposure
   Owner: Mark Gabriel Abenes
   Purpose: Quantify monthly financial impact of short/return activity
   ============================================================================ */

USE ccinfom_dev;

/* ============================================================================
   Main View: v_monthly_return_cost_shortage
   Time Grain: Month (Year + Month) using close_hdr.pod_ts
   ============================================================================ */

DROP VIEW IF EXISTS v_monthly_return_cost_shortage;

CREATE VIEW v_monthly_return_cost_shortage AS
SELECT 
    /* Time Dimensions */
    d.calendar_year AS `year`,
    d.calendar_month AS `month`,
    d.year_month_label AS `year_month`,
    
    /* Metrics from R3 Specification */
    
    /* 1. Total tickets closed */
    COUNT(DISTINCT ch.close_id) AS total_tickets_closed,
    
    /* 2. Short-closed tickets */
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Short-Closed' 
        THEN ch.close_id 
    END) AS short_closed_tickets,
    
    /* 3. Short-close rate percent */
    ROUND(
        100.0 * COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) / 
        NULLIF(COUNT(DISTINCT ch.close_id), 0),
        2
    ) AS short_close_rate_pct,
    
    /* 4. Total requested qty */
    SUM(cv.requested_qty) AS total_requested_qty,
    
    /* 5. Total delivered qty */
    SUM(cv.delivered_qty) AS total_delivered_qty,
    
    /* 6. Total short qty */
    SUM(cv.short_qty) AS total_short_qty,
    
    /* 7. Estimated return cost */
    ROUND(
        SUM(cv.short_qty * COALESCE(p.unit_price, 0)),
        2
    ) AS estimated_return_cost,
    
    /* 8. Fulfillment rate percent */
    ROUND(
        100.0 * SUM(cv.delivered_qty) / NULLIF(SUM(cv.requested_qty), 0),
        2
    ) AS fulfillment_rate_pct,
    
    /* Additional Throughput Metrics */
    
    /* Boxes packed in this month */
    COUNT(DISTINCT pb.box_id) AS boxes_packed,
    
    /* Manifests created in this month */
    COUNT(DISTINCT dh.dispatch_id) AS manifests_created,
    
    /* Inventory delta - Reserved */
    COALESCE(SUM(itl.delta_reserved), 0) AS inventory_delta_reserved,
    
    /* Inventory delta - On Hand */
    COALESCE(SUM(itl.delta_on_hand), 0) AS inventory_delta_on_hand,
    
    /* Dimensional Breakdowns */
    
    /* Customer dimension */
    c.customer_id,
    c.customer_name,
    
    /* Branch dimension */
    b.branch_id,
    b.branch_name,
    
    /* Product category aggregation */
    GROUP_CONCAT(DISTINCT p.category ORDER BY p.category SEPARATOR ', ') AS product_categories
    
FROM close_hdr ch

INNER JOIN dim_date d 
    ON DATE(ch.pod_ts) = d.calendar_date

INNER JOIN close_variance cv 
    ON ch.close_id = cv.close_id

INNER JOIN pick_ticket_line ptl 
    ON cv.ticket_line_id = ptl.ticket_line_id

LEFT JOIN products p 
    ON ptl.product_id = p.product_id

LEFT JOIN pick_ticket_hdr pth 
    ON ch.pick_ticket_id = pth.pick_ticket_id

LEFT JOIN customers c 
    ON pth.customer_id = c.customer_id

LEFT JOIN branches b 
    ON pth.branch_id = b.branch_id

LEFT JOIN pack_box_hdr pb 
    ON pth.pick_ticket_id = pb.pick_ticket_id
    AND pb.sealed_flag = TRUE
    AND DATE(pb.sealed_at) = d.calendar_date

LEFT JOIN dispatch_hdr dh 
    ON pth.pick_ticket_id = dh.pick_ticket_id
    AND DATE(dh.created_at) = d.calendar_date

LEFT JOIN inventory_txn_log itl 
    ON itl.ticket_id = pth.pick_ticket_id
    AND itl.source_txn_type = 'CLOSE'
    AND DATE(itl.created_at) = d.calendar_date

WHERE 
    ch.pod_ts IS NOT NULL
    AND ch.final_status IN ('Delivered', 'Short-Closed')

GROUP BY 
    d.calendar_year,
    d.calendar_month,
    d.year_month_label,
    c.customer_id,
    c.customer_name,
    b.branch_id,
    b.branch_name

ORDER BY 
    d.calendar_year DESC,
    d.calendar_month DESC,
    c.customer_name,
    b.branch_name;


/* ============================================================================
   Supporting Query 1: Monthly Summary (No dimensional breakdown)
   Use this for high-level KPI dashboard
   ============================================================================ */

DROP VIEW IF EXISTS v_monthly_summary;

CREATE VIEW v_monthly_summary AS
SELECT 
    d.calendar_year AS `year`,
    d.calendar_month AS `month`,
    d.year_month_label AS `year_month`,
    
    COUNT(DISTINCT ch.close_id) AS total_tickets_closed,
    COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) AS short_closed_tickets,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) / 
          NULLIF(COUNT(DISTINCT ch.close_id), 0), 2) AS short_close_rate_pct,
    
    SUM(cv.requested_qty) AS total_requested_qty,
    SUM(cv.delivered_qty) AS total_delivered_qty,
    SUM(cv.short_qty) AS total_short_qty,
    
    ROUND(SUM(cv.short_qty * COALESCE(p.unit_price, 0)), 2) AS estimated_return_cost,
    ROUND(100.0 * SUM(cv.delivered_qty) / NULLIF(SUM(cv.requested_qty), 0), 2) AS fulfillment_rate_pct,
    
    COUNT(DISTINCT pb.box_id) AS boxes_packed,
    COUNT(DISTINCT dh.dispatch_id) AS manifests_created,
    COALESCE(SUM(itl.delta_reserved), 0) AS inventory_delta_reserved,
    COALESCE(SUM(itl.delta_on_hand), 0) AS inventory_delta_on_hand

FROM close_hdr ch

INNER JOIN dim_date d 
    ON DATE(ch.pod_ts) = d.calendar_date

INNER JOIN close_variance cv 
    ON ch.close_id = cv.close_id

INNER JOIN pick_ticket_line ptl 
    ON cv.ticket_line_id = ptl.ticket_line_id

LEFT JOIN products p 
    ON ptl.product_id = p.product_id

LEFT JOIN pick_ticket_hdr pth 
    ON ch.pick_ticket_id = pth.pick_ticket_id

LEFT JOIN pack_box_hdr pb 
    ON pth.pick_ticket_id = pb.pick_ticket_id 
    AND pb.sealed_flag = TRUE
    AND DATE(pb.sealed_at) = d.calendar_date

LEFT JOIN dispatch_hdr dh 
    ON pth.pick_ticket_id = dh.pick_ticket_id
    AND DATE(dh.created_at) = d.calendar_date

LEFT JOIN inventory_txn_log itl 
    ON itl.ticket_id = pth.pick_ticket_id
    AND itl.source_txn_type = 'CLOSE'
    AND DATE(itl.created_at) = d.calendar_date

WHERE ch.pod_ts IS NOT NULL
  AND ch.final_status IN ('Delivered', 'Short-Closed')

GROUP BY d.calendar_year, d.calendar_month, d.year_month_label
ORDER BY d.calendar_year DESC, d.calendar_month DESC;


/* ============================================================================
   Supporting Query 2: Top Shortage Reasons by Month
   Use this to understand why items are being shorted
   ============================================================================ */

DROP VIEW IF EXISTS v_monthly_shortage_reasons;

CREATE VIEW v_monthly_shortage_reasons AS
SELECT 
    d.calendar_year AS `year`,
    d.calendar_month AS `month`,
    d.year_month_label AS `year_month`,
    
    cv.reason AS shortage_reason,
    
    COUNT(DISTINCT ch.close_id) AS tickets_affected,
    COUNT(cv.variance_id) AS shortage_line_count,
    SUM(cv.short_qty) AS total_short_qty,
    ROUND(SUM(cv.short_qty * COALESCE(p.unit_price, 0)), 2) AS cost_impact

FROM close_hdr ch

INNER JOIN dim_date d 
    ON DATE(ch.pod_ts) = d.calendar_date

INNER JOIN close_variance cv 
    ON ch.close_id = cv.close_id

INNER JOIN pick_ticket_line ptl 
    ON cv.ticket_line_id = ptl.ticket_line_id

LEFT JOIN products p 
    ON ptl.product_id = p.product_id

WHERE ch.pod_ts IS NOT NULL
  AND cv.short_qty > 0
  AND cv.reason IS NOT NULL

GROUP BY 
    d.calendar_year,
    d.calendar_month,
    d.year_month_label,
    cv.reason

ORDER BY 
    d.calendar_year DESC,
    d.calendar_month DESC,
    total_short_qty DESC;


/* ============================================================================
   Supporting Query 3: Top Products with Shortages by Month
   Use this to identify problem items
   ============================================================================ */

DROP VIEW IF EXISTS v_monthly_shortage_products;

CREATE VIEW v_monthly_shortage_products AS
SELECT 
    d.calendar_year AS `year`,
    d.calendar_month AS `month`,
    d.year_month_label AS `year_month`,
    
    p.product_id,
    p.product_name,
    p.sku,
    p.category,
    
    COUNT(DISTINCT ch.close_id) AS tickets_affected,
    SUM(cv.short_qty) AS total_short_qty,
    SUM(cv.requested_qty) AS total_requested_qty,
    ROUND(100.0 * SUM(cv.short_qty) / NULLIF(SUM(cv.requested_qty), 0), 2) AS shortage_rate_pct,
    ROUND(SUM(cv.short_qty * COALESCE(p.unit_price, 0)), 2) AS cost_impact

FROM close_hdr ch

INNER JOIN dim_date d 
    ON DATE(ch.pod_ts) = d.calendar_date

INNER JOIN close_variance cv 
    ON ch.close_id = cv.close_id

INNER JOIN pick_ticket_line ptl 
    ON cv.ticket_line_id = ptl.ticket_line_id

INNER JOIN products p 
    ON ptl.product_id = p.product_id

WHERE ch.pod_ts IS NOT NULL
  AND cv.short_qty > 0

GROUP BY 
    d.calendar_year,
    d.calendar_month,
    d.year_month_label,
    p.product_id,
    p.product_name,
    p.sku,
    p.category

ORDER BY 
    d.calendar_year DESC,
    d.calendar_month DESC,
    total_short_qty DESC;


/* ============================================================================
   Validation Query: Test against seed data
   Expected results for seeds seed-T5-hdr-delivered & seed-T5-hdr-short in Nov 2024:
   - Tickets closed = 2
   - Short-closed = 1
   - Short qty = 10 (example)
   - Fulfillment approximately 71%
   ============================================================================ */

SELECT 
    'R3 Validation - Seed Data' AS test_name,
    `year`,
    `month`,
    total_tickets_closed,
    short_closed_tickets,
    short_close_rate_pct,
    total_short_qty,
    estimated_return_cost,
    fulfillment_rate_pct
FROM v_monthly_summary
WHERE `year` = 2024 AND `month` = 11;


/* ============================================================================
   Sample Usage Queries
   ============================================================================ */

/* Example 1: Get current month summary */
SELECT * FROM v_monthly_summary
WHERE `year` = YEAR(CURDATE()) 
  AND `month` = MONTH(CURDATE());

/* Example 2: Get last 6 months trend */
SELECT 
    `year_month`,
    total_tickets_closed,
    short_close_rate_pct,
    fulfillment_rate_pct,
    estimated_return_cost
FROM v_monthly_summary
WHERE STR_TO_DATE(CONCAT(`year_month`, '-01'), '%Y-%m-%d') >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
ORDER BY `year` DESC, `month` DESC;

/* Example 3: Get shortage breakdown by customer for specific month */
SELECT 
    customer_name,
    total_tickets_closed,
    short_closed_tickets,
    short_close_rate_pct,
    total_short_qty,
    estimated_return_cost
FROM v_monthly_return_cost_shortage
WHERE `year` = 2024 AND `month` = 11
ORDER BY estimated_return_cost DESC;

/* Example 4: Top 10 problematic products this month */
SELECT 
    product_name,
    sku,
    category,
    total_short_qty,
    shortage_rate_pct,
    cost_impact
FROM v_monthly_shortage_products
WHERE `year` = YEAR(CURDATE()) 
  AND `month` = MONTH(CURDATE())
ORDER BY cost_impact DESC
LIMIT 10;

/* Example 5: Shortage reasons breakdown for current month */
SELECT 
    shortage_reason,
    tickets_affected,
    shortage_line_count,
    total_short_qty,
    cost_impact
FROM v_monthly_shortage_reasons
WHERE `year` = YEAR(CURDATE()) 
  AND `month` = MONTH(CURDATE())
ORDER BY cost_impact DESC;
-- TODO[Phase F - R4]:
-- Build on-time delivery / PoD compliance view joining dispatch_hdr/line, close_hdr, ticket/customer/vehicle tables, plus dim_date.
-- Implement the on-time flag logic and PoD metrics specified in /docs/reports-spec.md.

USE ccinfom_dev;

DROP VIEW IF EXISTS v_r4_on_time_delivery;

CREATE VIEW v_r4_on_time_delivery AS
SELECT
    -- Time Dimensions using shared dim_date
    dd.calendar_year AS delivery_year,
    dd.calendar_month AS delivery_month,
    dd.year_month_label,
    dd.month_name_label,
    
    -- Core Delivery Metrics (EXACTLY as specified)
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Delivered' 
        AND ch.pod_ts <= dh.depart_ts + INTERVAL IFNULL(v.sla_hours, 24) HOUR 
        THEN ch.pick_ticket_id 
    END) AS on_time_deliveries,
    
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Delivered' 
        AND ch.pod_ts > dh.depart_ts + INTERVAL IFNULL(v.sla_hours, 24) HOUR 
        THEN ch.pick_ticket_id 
    END) AS late_deliveries,
    
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Short-Closed' 
        THEN ch.pick_ticket_id 
    END) AS short_closed_shipments,
    
    -- Shortage Reasons Count (EXACTLY as specified - count occurrences, not distinct)
    COUNT(CASE WHEN cv.short_qty > 0 THEN cv.reason END) AS shortage_reasons_count,
    
    -- PoD Compliance Metrics
    COUNT(DISTINCT CASE 
        WHEN ch.pod_ref IS NOT NULL AND ch.pod_ts IS NOT NULL 
        THEN ch.pick_ticket_id 
    END) AS pod_compliant_deliveries,
    
    COUNT(DISTINCT CASE 
        WHEN (ch.pod_ref IS NULL OR ch.pod_ts IS NULL) 
        AND ch.final_status IN ('Delivered', 'Short-Closed')
        THEN ch.pick_ticket_id 
    END) AS pod_missing_shipments,
    
    -- Calculated Percentages (EXACTLY as specified)
    CASE 
        WHEN COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.pick_ticket_id END) > 0
        THEN ROUND(100.0 * 
            COUNT(DISTINCT CASE 
                WHEN ch.final_status = 'Delivered' 
                AND ch.pod_ts <= dh.depart_ts + INTERVAL IFNULL(v.sla_hours, 24) HOUR 
                THEN ch.pick_ticket_id 
            END) / 
            COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.pick_ticket_id END), 2)
        ELSE 0 
    END AS on_time_percentage,

    -- Additional Metrics for Context
    COUNT(DISTINCT ch.pick_ticket_id) AS total_shipments,
    COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.pick_ticket_id END) AS delivered_shipments,
    
    -- Product & Line Level Metrics (from required joins)
    COUNT(DISTINCT cv.variance_id) AS shortage_incidents,
    SUM(CASE WHEN cv.short_qty > 0 THEN cv.short_qty ELSE 0 END) AS total_shorted_units,

    -- Additional Dimensions for Filtering/Drill-down
    c.customer_id,
    c.customer_name,
    v.vehicle_id,
    v.plate_number,
    v.vehicle_type,
    v.sla_hours,  -- Include SLA hours for transparency
    e.employee_id AS driver_id,
    CONCAT(e.first_name, ' ', e.last_name) AS driver_name,
    b.branch_id,
    b.branch_name,
    b.city AS branch_city,
    p.product_id,
    p.sku,
    p.product_name,
    p.category

FROM close_hdr ch
-- REQUIRED JOINS as per specification
INNER JOIN dispatch_hdr dh ON ch.dispatch_id = dh.dispatch_id
INNER JOIN dispatch_line dl ON dh.dispatch_id = dl.dispatch_id  -- REQUIRED join
INNER JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
INNER JOIN pick_ticket_line ptl ON pth.pick_ticket_id = ptl.pick_ticket_id  -- REQUIRED join
INNER JOIN products p ON ptl.product_id = p.product_id  -- REQUIRED join
INNER JOIN customers c ON pth.customer_id = c.customer_id
INNER JOIN vehicles v ON dh.vehicle_id = v.vehicle_id
INNER JOIN employees e ON dh.driver_id = e.employee_id
INNER JOIN branches b ON pth.branch_id = b.branch_id
-- REQUIRED join for shortage reasons
LEFT JOIN close_variance cv ON ch.close_id = cv.close_id
-- Time dimension join
INNER JOIN dim_date dd ON DATE(ch.pod_ts) = dd.calendar_date

WHERE ch.pod_ts IS NOT NULL  -- Only include completed deliveries with timestamps

GROUP BY 
    dd.calendar_year,
    dd.calendar_month,
    dd.year_month_label,
    dd.month_name_label,
    c.customer_id,
    c.customer_name,
    v.vehicle_id,
    v.plate_number,
    v.vehicle_type,
    v.sla_hours,
    e.employee_id,
    CONCAT(e.first_name, ' ', e.last_name),
    b.branch_id,
    b.branch_name,
    b.city,
    p.product_id,
    p.sku,
    p.product_name,
    p.category

ORDER BY 
    delivery_year DESC,
    delivery_month DESC,
    customer_name,
    product_name;
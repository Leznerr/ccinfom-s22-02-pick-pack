-- TODO[Phase F - R4]:
-- Build on-time delivery / PoD compliance view joining dispatch_hdr/line, close_hdr, ticket/customer/vehicle tables, plus dim_date.
-- Implement the on-time flag logic and PoD metrics specified in /docs/reports-spec.md.

USE ccinfom_dev;

CREATE OR REPLACE VIEW v_r4_on_time_delivery AS
SELECT
    -- Time Dimensions (Monthly)
    YEAR(ch.pod_ts) AS delivery_year,
    MONTH(ch.pod_ts) AS delivery_month,
    CONCAT(YEAR(ch.pod_ts), '-', LPAD(MONTH(ch.pod_ts), 2, '0')) AS year_month_label,
    DATE_FORMAT(ch.pod_ts, '%Y-%m') AS delivery_year_month,
    
    -- Delivery Performance Metrics
    COUNT(DISTINCT ch.pick_ticket_id) AS total_shipments,
    
    -- On-Time vs Late Deliveries (using 24-hour SLA as specified in specs)
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Delivered' 
        AND ch.pod_ts <= dh.depart_ts + INTERVAL 24 HOUR 
        THEN ch.pick_ticket_id 
    END) AS on_time_deliveries,
    
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Delivered' 
        AND ch.pod_ts > dh.depart_ts + INTERVAL 24 HOUR 
        THEN ch.pick_ticket_id 
    END) AS late_deliveries,
    
    -- Short-Closed Shipments
    COUNT(DISTINCT CASE 
        WHEN ch.final_status = 'Short-Closed' 
        THEN ch.pick_ticket_id 
    END) AS short_closed_shipments,
    
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
    
    -- Shortage Reasons Analysis
    COUNT(DISTINCT CASE 
        WHEN cv.short_qty > 0 
        THEN cv.variance_id 
    END) AS shortage_incidents,
    
    COUNT(DISTINCT CASE 
        WHEN cv.short_qty > 0 
        THEN cv.reason 
    END) AS distinct_shortage_reasons,
    
    -- Top Shortage Reasons (comma separated for this month)
    GROUP_CONCAT(DISTINCT 
        CASE WHEN cv.short_qty > 0 THEN cv.reason END 
        SEPARATOR ', '
    ) AS shortage_reasons_list,
    
    -- Quantity Metrics
    SUM(CASE WHEN cv.short_qty > 0 THEN cv.short_qty ELSE 0 END) AS total_shorted_units,
    
    -- Calculated Percentages
    CASE 
        WHEN COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.pick_ticket_id END) > 0
        THEN ROUND(100.0 * 
            COUNT(DISTINCT CASE 
                WHEN ch.final_status = 'Delivered' 
                AND ch.pod_ts <= dh.depart_ts + INTERVAL 24 HOUR 
                THEN ch.pick_ticket_id 
            END) / 
            COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.pick_ticket_id END), 2)
        ELSE 0 
    END AS on_time_percentage,
    
    CASE 
        WHEN COUNT(DISTINCT ch.pick_ticket_id) > 0
        THEN ROUND(100.0 * 
            COUNT(DISTINCT CASE 
                WHEN ch.pod_ref IS NOT NULL AND ch.pod_ts IS NOT NULL 
                THEN ch.pick_ticket_id 
            END) / 
            COUNT(DISTINCT ch.pick_ticket_id), 2)
        ELSE 0 
    END AS pod_compliance_percentage,

    -- Additional Dimensions for Filtering/Drill-down
    c.customer_id,
    c.customer_name,
    v.vehicle_id,
    v.plate_number,
    v.vehicle_type,
    e.employee_id AS driver_id,
    CONCAT(e.first_name, ' ', e.last_name) AS driver_name,
    b.branch_id,
    b.branch_name,
    b.city AS branch_city

FROM close_hdr ch
-- Join through dispatch to get departure time for SLA calculation
INNER JOIN dispatch_hdr dh ON ch.dispatch_id = dh.dispatch_id
INNER JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
INNER JOIN customers c ON pth.customer_id = c.customer_id
INNER JOIN vehicles v ON dh.vehicle_id = v.vehicle_id
INNER JOIN employees e ON dh.driver_id = e.employee_id
INNER JOIN branches b ON pth.branch_id = b.branch_id
-- Left join to capture shortage reasons (will be NULL for fully delivered tickets)
LEFT JOIN close_variance cv ON ch.close_id = cv.close_id

WHERE ch.pod_ts IS NOT NULL  -- Only include completed deliveries with timestamps

GROUP BY 
    YEAR(ch.pod_ts),
    MONTH(ch.pod_ts),
    CONCAT(YEAR(ch.pod_ts), '-', LPAD(MONTH(ch.pod_ts), 2, '0')),
    DATE_FORMAT(ch.pod_ts, '%Y-%m'),
    c.customer_id,
    c.customer_name,
    v.vehicle_id,
    v.plate_number,
    v.vehicle_type,
    e.employee_id,
    CONCAT(e.first_name, ' ', e.last_name),
    b.branch_id,
    b.branch_name,
    b.city

ORDER BY 
    delivery_year DESC,
    delivery_month DESC,
    customer_name;
    
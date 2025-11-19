-- TODO[Phase F - R4]:
-- Build on-time delivery / PoD compliance view joining dispatch_hdr/line, close_hdr, ticket/customer/vehicle tables, plus dim_date.
-- Implement the on-time flag logic and PoD metrics specified in /docs/reports-spec.md.

USE ccinfom_dev;

DROP VIEW IF EXISTS v_r4_on_time_delivery;

CREATE VIEW v_r4_on_time_delivery AS
SELECT
    -- Time Dimensions using dim_date
    dd.calendar_year AS delivery_year,
    dd.calendar_month AS delivery_month,
    dd.year_month_label,
    
    -- Customer Dimension (for "per customer" breakdown)
    c.customer_id,
    c.customer_name,
    
    -- Product Dimension (for "per product" breakdown)  
    p.product_id,
    p.sku,
    p.product_name,
    p.category,
    
    -- Core Metrics (EXACTLY as specified - ONLY these 5)
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
    
    -- Calculated Percentage (EXACTLY as specified)
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
    END AS on_time_percentage

FROM close_hdr ch
-- REQUIRED TABLES/JOINS (EXACTLY as specified)
INNER JOIN dispatch_hdr dh ON ch.dispatch_id = dh.dispatch_id
INNER JOIN dispatch_line dl ON dh.dispatch_id = dl.dispatch_id
INNER JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
INNER JOIN close_variance cv ON ch.close_id = cv.close_id
INNER JOIN pick_ticket_line ptl ON cv.ticket_line_id = ptl.ticket_line_id
INNER JOIN products p ON ptl.product_id = p.product_id
INNER JOIN customers c ON pth.customer_id = c.customer_id
INNER JOIN vehicles v ON dh.vehicle_id = v.vehicle_id
INNER JOIN employees e ON dh.driver_id = e.employee_id
-- Time dimension join
INNER JOIN dim_date dd ON DATE(ch.pod_ts) = dd.calendar_date

WHERE ch.pod_ts IS NOT NULL

GROUP BY 
    dd.calendar_year,
    dd.calendar_month,
    dd.year_month_label,
    c.customer_id,
    c.customer_name,
    p.product_id,
    p.sku,
    p.product_name,
    p.category

ORDER BY 
    delivery_year DESC,
    delivery_month DESC,
    customer_name,
    product_name;

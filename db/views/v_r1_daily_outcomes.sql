USE ccinfom_dev;

DROP VIEW IF EXISTS v_r1_daily_outcomes;
CREATE VIEW v_r1_daily_outcomes AS
SELECT
    d.calendar_date,
    d.calendar_year,
    d.calendar_month,
    d.month_name_label,
    d.day_name_label,
    COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.close_id END)        AS delivered_ticket_count,
    COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END)     AS short_closed_ticket_count,
    COUNT(DISTINCT ch.close_id)                                                         AS tickets_closed,
    COUNT(CASE WHEN cv.short_qty > 0 THEN cv.variance_id END)                           AS shortage_line_count,
    COALESCE(SUM(CASE WHEN cv.short_qty > 0 THEN cv.short_qty ELSE 0 END), 0)           AS shortage_units
FROM close_hdr ch
JOIN close_variance cv
  ON cv.close_id = ch.close_id
JOIN dim_date d
  ON d.calendar_date = DATE(ch.created_at)
GROUP BY
    d.calendar_date,
    d.calendar_year,
    d.calendar_month,
    d.month_name_label,
    d.day_name_label;

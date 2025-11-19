-- TODO[Phase F - R2]:
-- Implement ISO week productivity view using picking_hdr/line, employees, products, and dim_date.
-- Include all metrics (lines picked, units/hour, error counts) defined in /docs/reports-spec.md.

DROP VIEW IF EXISTS v_r2_weekly_picker_productivity;

CREATE VIEW v_r2_weekly_picker_productivity AS
SELECT
    -- ISO week metadata from dim_date
    dd.iso_year,
    dd.iso_week,
    diw.iso_week_start,
    diw.iso_week_end,

    -- Picker info
    ph.picker_employee_id,
    e.first_name,
    e.last_name,

    -- Product category
    p.category,

    -- Metrics
    COUNT(DISTINCT pl.picking_line_id) AS lines_picked,
    SUM(pl.picked_qty) AS units_picked,

    -- Safer units/hour calculation (prevents division by zero)
    ROUND(
        SUM(pl.picked_qty) /
        NULLIF(SUM(GREATEST(TIMESTAMPDIFF(MINUTE, ph.started_at, ph.completed_at), 1)) / 60, 0),
        2
    ) AS units_per_hour,

    -- Average pick time (ensures minimum 1 minute to avoid zeros)
    ROUND(
        AVG(GREATEST(TIMESTAMPDIFF(MINUTE, ph.started_at, ph.completed_at), 1)),
        0
    ) AS avg_pick_time_min,

    -- Error tracking
    COUNT(DISTINCT CASE WHEN pl.short_reason IS NOT NULL THEN pl.picking_line_id END) AS short_error_lines

FROM picking_hdr ph
JOIN picking_line pl ON pl.picking_id = ph.picking_id
JOIN products p ON p.product_id = pl.product_id
JOIN employees e ON e.employee_id = ph.picker_employee_id
JOIN dim_date dd ON DATE(ph.completed_at) = dd.calendar_date
JOIN dim_iso_week diw ON dd.iso_year = diw.iso_year AND dd.iso_week = diw.iso_week
LEFT JOIN pick_ticket_hdr t ON ph.pick_ticket_id = t.pick_ticket_id

WHERE ph.picking_status = 'Done'
  AND ph.completed_at IS NOT NULL
  AND ph.started_at IS NOT NULL
  AND ph.completed_at > ph.started_at

GROUP BY
    dd.iso_year,
    dd.iso_week,
    diw.iso_week_start,
    diw.iso_week_end,
    ph.picker_employee_id,
    e.first_name,
    e.last_name,
    p.category

ORDER BY
    dd.iso_year DESC,
    dd.iso_week DESC,
    ph.picker_employee_id,
    p.category;
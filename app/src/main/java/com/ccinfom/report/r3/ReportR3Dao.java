package com.ccinfom.report.r3;

import com.ccinfom.report.ReportDaoBase;
import com.ccinfom.report.r4.ReportFilters;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * TODO[Phase F - R3]:
 *  - Pull monthly throughput metrics from v_r3_monthly_inventory_throughput.
 *  - Support optional filters (product, branch, warehouse) as defined in the spec.
 *  - Provide summary aggregations if the UI needs totals.
 */
public class ReportR3Dao extends ReportDaoBase {

    private static final Logger LOGGER = Logger.getLogger(ReportR3Dao.class.getName());

    // TODO: Implement methods such as List<ReportR3Row> findMonthlyThroughput(ReportFilters filters)
    /**
     * Fetch monthly throughput data with flexible filtering.
     * Uses ReportFilters object for standardized filter handling.
     *
     * @param filters ReportFilters object containing year, month, and optional filters
     * @return List of R3MonthlyThroughputRow objects
     * @throws SQLException if database error occurs
     */
    public List<R3MonthlyThroughputRow> findMonthlyThroughput(ReportFilters filters) throws SQLException {
        ensureViews();

        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "  `year`, `month`, year_month, " +
                        "  customer_id, customer_name, " +
                        "  branch_id, branch_name, " +
                        "  total_tickets_closed, short_closed_tickets, short_close_rate_pct, " +
                        "  total_requested_qty, total_delivered_qty, total_short_qty, " +
                        "  estimated_return_cost, fulfillment_rate_pct, " +
                        "  boxes_packed, manifests_created, " +
                        "  inventory_delta_reserved, inventory_delta_on_hand, " +
                        "  product_categories " +
                        "FROM v_monthly_return_cost_shortage " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        /* Build WHERE clause based on filters */
        if (filters.getYear() != null) {
            sql.append("AND `year` = ? ");
            params.add(filters.getYear());
        }

        if (filters.getMonth() != null) {
            sql.append("AND `month` = ? ");
            params.add(filters.getMonth());
        }

        if (filters.getCustomerId() != null) {
            sql.append("AND customer_id = ? ");
            params.add(filters.getCustomerId());
        }

        if (filters.getBranchId() != null) {
            sql.append("AND branch_id = ? ");
            params.add(filters.getBranchId());
        }

        if (filters.getProductCategory() != null && !filters.getProductCategory().isBlank()) {
            sql.append("AND product_categories LIKE ? ");
            params.add("%" + filters.getProductCategory().trim() + "%");
        }

        sql.append("ORDER BY `year` DESC, `month` DESC, customer_name, branch_name");

        logSql(sql, params);

        /* Execute using ReportDaoBase helper */
        return executeQuery(
                sql.toString(),
                ps -> {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                },
                this::mapRow
        );
    }

    /**
     * Fetch summary aggregation (no customer/branch breakdown).
     * Aggregates all data matching the filters into a single summary row.
     *
     * @param filters ReportFilters object containing year, month, and optional filters
     * @return Single aggregated R3MonthlyThroughputRow (or null if no data)
     * @throws SQLException if database error occurs
     */
    public R3MonthlyThroughputRow findMonthlySummary(ReportFilters filters) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "  `year`, `month`, MIN(year_month) AS year_month, " +
                        "  NULL AS customer_id, 'All Customers' AS customer_name, " +
                        "  NULL AS branch_id, 'All Branches' AS branch_name, " +
                        "  SUM(total_tickets_closed) AS total_tickets_closed, " +
                        "  SUM(short_closed_tickets) AS short_closed_tickets, " +
                        "  ROUND(100.0 * SUM(short_closed_tickets) / NULLIF(SUM(total_tickets_closed), 0), 2) AS short_close_rate_pct, " +
                        "  SUM(total_requested_qty) AS total_requested_qty, " +
                        "  SUM(total_delivered_qty) AS total_delivered_qty, " +
                        "  SUM(total_short_qty) AS total_short_qty, " +
                        "  SUM(estimated_return_cost) AS estimated_return_cost, " +
                        "  ROUND(100.0 * SUM(total_delivered_qty) / NULLIF(SUM(total_requested_qty), 0), 2) AS fulfillment_rate_pct, " +
                        "  SUM(boxes_packed) AS boxes_packed, " +
                        "  SUM(manifests_created) AS manifests_created, " +
                        "  SUM(inventory_delta_reserved) AS inventory_delta_reserved, " +
                        "  SUM(inventory_delta_on_hand) AS inventory_delta_on_hand, " +
                        "  NULL AS product_categories " +
                        "FROM v_monthly_summary " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (filters.getYear() != null) {
            sql.append("AND `year` = ? ");
            params.add(filters.getYear());
        }

        if (filters.getMonth() != null) {
            sql.append("AND `month` = ? ");
            params.add(filters.getMonth());
        }

        if (filters.getCustomerId() != null) {
            sql.append("AND customer_id = ? ");
            params.add(filters.getCustomerId());
        }

        if (filters.getBranchId() != null) {
            sql.append("AND branch_id = ? ");
            params.add(filters.getBranchId());
        }

        if (filters.getProductCategory() != null && !filters.getProductCategory().isBlank()) {
            sql.append("AND product_categories LIKE ? ");
            params.add("%" + filters.getProductCategory().trim() + "%");
        }

        sql.append("GROUP BY `year`, `month`");

        logSql(sql, params);

        List<R3MonthlyThroughputRow> results = executeQuery(
                sql.toString(),
                ps -> {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                },
                this::mapRow
        );

        return results.isEmpty() ? null : results.get(0);
    }

    private void logSql(StringBuilder sql, List<Object> params) {
        LOGGER.info(() -> "[R3] SQL: " + sql + " | params=" + params);
    }

    /**
     * Defensive guard: ensure required R3 views exist (dim_date and monthly views).
     * Avoids runtime failures when DB reset scripts were not applied.
     */
    private void ensureViews() throws SQLException {
        try (var conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(createDimDateViewSql());
            stmt.execute(createReturnCostViewSql());
            stmt.execute(createMonthlySummaryViewSql());
        }
    }

    private String createDimDateViewSql() {
        return """
            CREATE OR REPLACE VIEW dim_date AS
            SELECT
              calendar_date,
              YEAR(calendar_date)                    AS calendar_year,
              MONTH(calendar_date)                   AS calendar_month,
              DAY(calendar_date)                     AS calendar_day,
              QUARTER(calendar_date)                 AS quarter_of_year,
              CONCAT(YEAR(calendar_date), '-', LPAD(MONTH(calendar_date),2,'0')) AS year_month_label,
              MONTHNAME(calendar_date) AS month_name_label,
              DAYNAME(calendar_date) AS day_name_label,
              DAYOFWEEK(calendar_date)               AS day_of_week_sun1,
              (DAYOFWEEK(calendar_date) + 5) % 7 + 1 AS day_of_week_mon1,
              WEEK(calendar_date, 0)                 AS week_of_year,
              MOD(YEARWEEK(calendar_date, 3), 100)   AS iso_week,
              FLOOR(YEARWEEK(calendar_date, 3) / 100) AS iso_year,
              CASE WHEN DAYOFWEEK(calendar_date) IN (1,7) THEN 1 ELSE 0 END AS is_weekend,
              DATE_SUB(calendar_date, INTERVAL ((DAYOFWEEK(calendar_date) + 5) % 7) DAY) AS week_start_monday,
              DATE_ADD(DATE_SUB(calendar_date, INTERVAL ((DAYOFWEEK(calendar_date) + 5) % 7) DAY), INTERVAL 6 DAY) AS week_end_sunday,
              DATE_SUB(calendar_date, INTERVAL DAY(calendar_date) - 1 DAY) AS month_start,
              LAST_DAY(calendar_date)                AS month_end
            FROM (
              SELECT DATE('2023-01-01') + INTERVAL offsets.day_offset DAY AS calendar_date
              FROM (
                SELECT
                  d4.d * 1000 + d3.d * 100 + d2.d * 10 + d1.d AS day_offset
                FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1
                CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2
                CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d3
                CROSS JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d4
              ) offsets
              WHERE DATE('2023-01-01') + INTERVAL offsets.day_offset DAY <= DATE('2030-12-31')
            ) span
            """;
    }

    private String createReturnCostViewSql() {
        return """
            CREATE OR REPLACE VIEW v_monthly_return_cost_shortage AS
            SELECT
                d.calendar_year AS `year`,
                d.calendar_month AS `month`,
                d.year_month_label AS `year_month`,
                COUNT(DISTINCT ch.close_id) AS total_tickets_closed,
                COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) AS short_closed_tickets,
                ROUND(
                    100.0 * COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) /
                    NULLIF(COUNT(DISTINCT ch.close_id), 0),
                    2
                ) AS short_close_rate_pct,
                SUM(cv.requested_qty) AS total_requested_qty,
                SUM(cv.delivered_qty) AS total_delivered_qty,
                SUM(cv.short_qty) AS total_short_qty,
                ROUND(SUM(cv.short_qty * COALESCE(p.unit_price, 0)), 2) AS estimated_return_cost,
                ROUND(100.0 * SUM(cv.delivered_qty) / NULLIF(SUM(cv.requested_qty), 0), 2) AS fulfillment_rate_pct,
                COUNT(DISTINCT pb.box_id) AS boxes_packed,
                COUNT(DISTINCT dh.dispatch_id) AS manifests_created,
                COALESCE(SUM(itl.delta_reserved), 0) AS inventory_delta_reserved,
                COALESCE(SUM(itl.delta_on_hand), 0) AS inventory_delta_on_hand,
                c.customer_id,
                c.customer_name,
                b.branch_id,
                b.branch_name,
                GROUP_CONCAT(DISTINCT p.category ORDER BY p.category SEPARATOR ', ') AS product_categories
            FROM close_hdr ch
            INNER JOIN dim_date d ON DATE(ch.pod_ts) = d.calendar_date
            INNER JOIN close_variance cv ON ch.close_id = cv.close_id
            INNER JOIN pick_ticket_line ptl ON cv.ticket_line_id = ptl.ticket_line_id
            LEFT JOIN products p ON ptl.product_id = p.product_id
            LEFT JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
            LEFT JOIN customers c ON pth.customer_id = c.customer_id
            LEFT JOIN branches b ON pth.branch_id = b.branch_id
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
                b.branch_name
            """;
    }

    private String createMonthlySummaryViewSql() {
        return """
            CREATE OR REPLACE VIEW v_monthly_summary AS
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
            INNER JOIN dim_date d ON DATE(ch.pod_ts) = d.calendar_date
            INNER JOIN close_variance cv ON ch.close_id = cv.close_id
            INNER JOIN pick_ticket_line ptl ON cv.ticket_line_id = ptl.ticket_line_id
            LEFT JOIN products p ON ptl.product_id = p.product_id
            LEFT JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
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
                d.year_month_label
            ORDER BY
                d.calendar_year DESC,
                d.calendar_month DESC
            """;
    }
    /**
     * Map a ResultSet row to R3MonthlyThroughputRow.
     * Uses ReportDaoBase RowMapper pattern.
     *
     * @param rs ResultSet positioned at current row
     * @return Mapped R3MonthlyThroughputRow object
     * @throws SQLException if mapping error occurs
     */
    private R3MonthlyThroughputRow mapRow(ResultSet rs) throws SQLException {
        R3MonthlyThroughputRow row = new R3MonthlyThroughputRow();

        /* Time Dimensions */
        row.setYear(rs.getInt("year"));
        row.setMonth(rs.getInt("month"));
        row.setYearMonth(rs.getString("year_month"));

        /* Dimensional Attributes */
        Long customerId = rs.getLong("customer_id");
        row.setCustomerId(rs.wasNull() ? null : customerId);
        row.setCustomerName(rs.getString("customer_name"));

        Long branchId = rs.getLong("branch_id");
        row.setBranchId(rs.wasNull() ? null : branchId);
        row.setBranchName(rs.getString("branch_name"));

        /* Core Metrics */
        row.setTotalTicketsClosed(rs.getInt("total_tickets_closed"));
        row.setShortClosedTickets(rs.getInt("short_closed_tickets"));
        row.setShortCloseRatePct(rs.getBigDecimal("short_close_rate_pct"));

        row.setTotalRequestedQty(rs.getBigDecimal("total_requested_qty"));
        row.setTotalDeliveredQty(rs.getBigDecimal("total_delivered_qty"));
        row.setTotalShortQty(rs.getBigDecimal("total_short_qty"));

        row.setEstimatedReturnCost(rs.getBigDecimal("estimated_return_cost"));
        row.setFulfillmentRatePct(rs.getBigDecimal("fulfillment_rate_pct"));

        /* Throughput Metrics */
        row.setBoxesPacked(rs.getInt("boxes_packed"));
        row.setManifestsCreated(rs.getInt("manifests_created"));

        /* Inventory Deltas */
        row.setInventoryDeltaReserved(rs.getBigDecimal("inventory_delta_reserved"));
        row.setInventoryDeltaOnHand(rs.getBigDecimal("inventory_delta_on_hand"));

        /* Product Categories */
        row.setProductCategories(rs.getString("product_categories"));

        return row;
    }
}


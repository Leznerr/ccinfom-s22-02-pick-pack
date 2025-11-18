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
 * R3 DAO: Monthly inventory / pack-dispatch throughput.
 * This implementation performs per-ticket aggregation first to avoid
 * double counting from line-level joins, then rolls up to month/customer/branch.
 */
public class ReportR3Dao extends ReportDaoBase {

    private static final Logger log = Logger.getLogger(ReportR3Dao.class.getName());

    /**
     * Returns detail rows for the selected period and optional filters.
     */
    public List<R3MonthlyThroughputRow> findMonthlyThroughput(ReportFilters filters) throws SQLException {
        String sql = buildSql(false);
        List<Object> params = buildParams(filters, false);
        logSql(sql, params);

        return executeQuery(sql, ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }, this::mapRow);
    }

    /**
     * Returns a single rolled-up summary row for the selected filters.
     */
    public R3MonthlyThroughputRow findMonthlySummary(ReportFilters filters) throws SQLException {
        String sql = buildSql(true);
        List<Object> params = buildParams(filters, true);
        logSql(sql, params);

        List<R3MonthlyThroughputRow> rows = executeQuery(sql, ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }, this::mapRow);

        return rows.isEmpty() ? null : rows.get(0);
    }

    private String buildSql(boolean summaryOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    tr.yr   AS `year`,
                    tr.mon  AS `month`,
                    tr.year_month_label,
                    tr.customer_id,
                    tr.customer_name,
                    tr.branch_id,
                    tr.branch_name,
                    COUNT(*) AS total_tickets_closed,
                    SUM(tr.is_short_closed) AS short_closed_tickets,
                    ROUND(100.0 * SUM(tr.is_short_closed) / NULLIF(COUNT(*), 0), 2) AS short_close_rate_pct,
                    SUM(tr.total_requested_qty) AS total_requested_qty,
                    SUM(tr.total_delivered_qty) AS total_delivered_qty,
                    SUM(tr.total_short_qty)     AS total_short_qty,
                    ROUND(SUM(tr.estimated_return_cost), 2) AS estimated_return_cost,
                    ROUND(100.0 * SUM(tr.total_delivered_qty) / NULLIF(SUM(tr.total_requested_qty), 0), 2) AS fulfillment_rate_pct,
                    SUM(COALESCE(ta.boxes_packed, 0))       AS boxes_packed,
                    SUM(COALESCE(ta.manifests_created, 0))  AS manifests_created,
                    SUM(COALESCE(ta.inventory_delta_reserved, 0)) AS inventory_delta_reserved,
                    SUM(COALESCE(ta.inventory_delta_on_hand, 0))  AS inventory_delta_on_hand,
                    GROUP_CONCAT(DISTINCT tr.product_categories SEPARATOR ', ') AS product_categories
                FROM (
                    SELECT
                        YEAR(ch.pod_ts)   AS yr,
                        MONTH(ch.pod_ts)  AS mon,
                        DATE_FORMAT(ch.pod_ts, '%Y-%m') AS year_month_label,
                        pth.pick_ticket_id,
                        pth.customer_id,
                        c.customer_name,
                        pth.branch_id,
                        b.branch_name,
                        SUM(cv.requested_qty) AS total_requested_qty,
                        SUM(cv.delivered_qty) AS total_delivered_qty,
                        SUM(cv.short_qty)     AS total_short_qty,
                        SUM(cv.short_qty * COALESCE(p.unit_price, 0)) AS estimated_return_cost,
                        -- Aggregate final_status to satisfy ONLY_FULL_GROUP_BY
                        SUM(CASE WHEN ch.final_status = 'Short-Closed' THEN 1 ELSE 0 END) AS is_short_closed,
                        GROUP_CONCAT(DISTINCT p.category ORDER BY p.category SEPARATOR ', ') AS product_categories
                    FROM close_hdr ch
                    JOIN pick_ticket_hdr pth ON pth.pick_ticket_id = ch.pick_ticket_id
                    JOIN close_variance cv    ON cv.close_id = ch.close_id
                    JOIN pick_ticket_line ptl ON ptl.ticket_line_id = cv.ticket_line_id
                    LEFT JOIN products p      ON p.product_id = ptl.product_id
                    LEFT JOIN customers c     ON c.customer_id = pth.customer_id
                    LEFT JOIN branches b      ON b.branch_id = pth.branch_id
                    WHERE ch.pod_ts IS NOT NULL
                      AND ch.final_status IN ('Delivered', 'Short-Closed')
                    GROUP BY
                        YEAR(ch.pod_ts), MONTH(ch.pod_ts), DATE_FORMAT(ch.pod_ts, '%Y-%m'),
                        pth.pick_ticket_id, pth.customer_id, c.customer_name,
                        pth.branch_id, b.branch_name
                ) tr
                LEFT JOIN (
                    SELECT
                        pth.pick_ticket_id,
                        COUNT(DISTINCT CASE WHEN pb.sealed_flag = TRUE THEN pb.box_id END) AS boxes_packed,
                        COUNT(DISTINCT dh.dispatch_id) AS manifests_created,
                        COALESCE(SUM(itl.delta_reserved), 0)   AS inventory_delta_reserved,
                        COALESCE(SUM(itl.delta_on_hand), 0)    AS inventory_delta_on_hand
                    FROM pick_ticket_hdr pth
                    LEFT JOIN pack_box_hdr pb ON pb.pick_ticket_id = pth.pick_ticket_id AND pb.sealed_flag = TRUE
                    LEFT JOIN dispatch_hdr dh ON dh.pick_ticket_id = pth.pick_ticket_id
                    LEFT JOIN inventory_txn_log itl ON itl.ticket_id = pth.pick_ticket_id AND itl.source_txn_type = 'CLOSE'
                    GROUP BY pth.pick_ticket_id
                ) ta ON ta.pick_ticket_id = tr.pick_ticket_id
                WHERE 1=1
                """);

        sql.append("\n  AND tr.yr = ?");
        sql.append("\n  AND tr.mon = ?");

        sql.append("\n  AND (? IS NULL OR tr.customer_id = ?)");
        sql.append("\n  AND (? IS NULL OR tr.branch_id = ?)");
        sql.append("\n  AND (? IS NULL OR tr.product_categories LIKE CONCAT('%', ?, '%'))");

                sql.append("""
                \nGROUP BY
                    tr.yr, tr.mon, tr.year_month_label,
                    tr.customer_id, tr.customer_name,
                    tr.branch_id, tr.branch_name
                """);

        if (summaryOnly) {
            sql.append("\nORDER BY tr.yr DESC, tr.mon DESC LIMIT 1");
        } else {
            sql.append("\nORDER BY tr.yr DESC, tr.mon DESC, tr.customer_name, tr.branch_name");
        }

        return sql.toString();
    }

    private List<Object> buildParams(ReportFilters filters, boolean summaryOnly) {
        List<Object> params = new ArrayList<>();

        // Required year/month (UI enforces via ReportFilterPanel)
        params.add(filters.getYear());
        params.add(filters.getMonth());

        params.add(filters.getCustomerId());
        params.add(filters.getCustomerId());

        params.add(filters.getBranchId());
        params.add(filters.getBranchId());

        params.add(filters.getProductCategory());
        params.add(filters.getProductCategory());

        return params;
    }

    private R3MonthlyThroughputRow mapRow(ResultSet rs) throws SQLException {
        R3MonthlyThroughputRow row = new R3MonthlyThroughputRow();

        row.setYear(rs.getInt("year"));
        row.setMonth(rs.getInt("month"));
        row.setYearMonth(rs.getString("year_month_label"));

        row.setCustomerId(rs.getObject("customer_id", Long.class));
        row.setCustomerName(rs.getString("customer_name"));
        row.setBranchId(rs.getObject("branch_id", Long.class));
        row.setBranchName(rs.getString("branch_name"));

        row.setTotalTicketsClosed(rs.getInt("total_tickets_closed"));
        row.setShortClosedTickets(rs.getInt("short_closed_tickets"));
        row.setShortCloseRatePct(rs.getBigDecimal("short_close_rate_pct"));

        row.setTotalRequestedQty(rs.getBigDecimal("total_requested_qty"));
        row.setTotalDeliveredQty(rs.getBigDecimal("total_delivered_qty"));
        row.setTotalShortQty(rs.getBigDecimal("total_short_qty"));

        row.setEstimatedReturnCost(rs.getBigDecimal("estimated_return_cost"));
        row.setFulfillmentRatePct(rs.getBigDecimal("fulfillment_rate_pct"));

        row.setBoxesPacked(rs.getInt("boxes_packed"));
        row.setManifestsCreated(rs.getInt("manifests_created"));

        row.setInventoryDeltaReserved(rs.getBigDecimal("inventory_delta_reserved"));
        row.setInventoryDeltaOnHand(rs.getBigDecimal("inventory_delta_on_hand"));

        row.setProductCategories(rs.getString("product_categories"));

        return row;
    }

    private void logSql(String sql, List<Object> params) {
        log.info(() -> "[R3] SQL: " + sql.replaceAll("\\s+", " ").trim() + " | params=" + params);
    }
}

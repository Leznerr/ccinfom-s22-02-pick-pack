package com.ccinfom.report.r3;

import com.ccinfom.report.ReportDaoBase;
import com.ccinfom.report.r4.ReportFilters;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ReportR3Dao extends ReportDaoBase {

    private static final Logger LOGGER = Logger.getLogger(ReportR3Dao.class.getName());

    /**
     * Inline aggregation; no dependency on views.
     */
    public List<R3MonthlyThroughputRow> findMonthlyThroughput(ReportFilters filters) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "  YEAR(ch.pod_ts) AS `year`, " +
                        "  MONTH(ch.pod_ts) AS `month`, " +
                        "  DATE_FORMAT(ch.pod_ts, '%Y-%m') AS year_month, " +
                        "  c.customer_id, c.customer_name, " +
                        "  b.branch_id, b.branch_name, " +
                        "  COUNT(DISTINCT ch.close_id) AS total_tickets_closed, " +
                        "  COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) AS short_closed_tickets, " +
                        "  ROUND(100.0 * COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) / NULLIF(COUNT(DISTINCT ch.close_id), 0), 2) AS short_close_rate_pct, " +
                        "  SUM(cv.requested_qty) AS total_requested_qty, " +
                        "  SUM(cv.delivered_qty) AS total_delivered_qty, " +
                        "  SUM(cv.short_qty) AS total_short_qty, " +
                        "  ROUND(SUM(cv.short_qty * COALESCE(p.unit_price, 0)), 2) AS estimated_return_cost, " +
                        "  ROUND(100.0 * SUM(cv.delivered_qty) / NULLIF(SUM(cv.requested_qty), 0), 2) AS fulfillment_rate_pct, " +
                        "  COUNT(DISTINCT pb.box_id) AS boxes_packed, " +
                        "  COUNT(DISTINCT dh.dispatch_id) AS manifests_created, " +
                        "  COALESCE(SUM(itl.delta_reserved), 0) AS inventory_delta_reserved, " +
                        "  COALESCE(SUM(itl.delta_on_hand), 0) AS inventory_delta_on_hand, " +
                        "  GROUP_CONCAT(DISTINCT p.category ORDER BY p.category SEPARATOR ', ') AS product_categories " +
                        "FROM close_hdr ch " +
                        "INNER JOIN close_variance cv ON ch.close_id = cv.close_id " +
                        "INNER JOIN pick_ticket_line ptl ON cv.ticket_line_id = ptl.ticket_line_id " +
                        "LEFT JOIN products p ON ptl.product_id = p.product_id " +
                        "LEFT JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id " +
                        "LEFT JOIN customers c ON pth.customer_id = c.customer_id " +
                        "LEFT JOIN branches b ON pth.branch_id = b.branch_id " +
                        "LEFT JOIN pack_box_hdr pb ON pth.pick_ticket_id = pb.pick_ticket_id AND pb.sealed_flag = TRUE AND DATE(pb.sealed_at) = DATE(ch.pod_ts) " +
                        "LEFT JOIN dispatch_hdr dh ON pth.pick_ticket_id = dh.pick_ticket_id AND DATE(dh.created_at) = DATE(ch.pod_ts) " +
                        "LEFT JOIN inventory_txn_log itl ON itl.ticket_id = pth.pick_ticket_id AND itl.source_txn_type = 'CLOSE' AND DATE(itl.created_at) = DATE(ch.pod_ts) " +
                        "WHERE ch.pod_ts IS NOT NULL AND ch.final_status IN ('Delivered','Short-Closed') "
        );

        List<Object> params = new ArrayList<>();

        if (filters.getYear() != null) {
            sql.append("AND YEAR(ch.pod_ts) = ? ");
            params.add(filters.getYear());
        }
        if (filters.getMonth() != null) {
            sql.append("AND MONTH(ch.pod_ts) = ? ");
            params.add(filters.getMonth());
        }
        if (filters.getCustomerId() != null) {
            sql.append("AND c.customer_id = ? ");
            params.add(filters.getCustomerId());
        }
        if (filters.getBranchId() != null) {
            sql.append("AND b.branch_id = ? ");
            params.add(filters.getBranchId());
        }
        if (filters.getProductCategory() != null && !filters.getProductCategory().isBlank()) {
            sql.append("AND p.category LIKE ? ");
            params.add("%" + filters.getProductCategory().trim() + "%");
        }

        sql.append(
                "GROUP BY YEAR(ch.pod_ts), MONTH(ch.pod_ts), DATE_FORMAT(ch.pod_ts, '%Y-%m'), " +
                        "c.customer_id, c.customer_name, b.branch_id, b.branch_name " +
                        "ORDER BY YEAR(ch.pod_ts) DESC, MONTH(ch.pod_ts) DESC, c.customer_name, b.branch_name"
        );

        logSql(sql, params);

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

    private void logSql(StringBuilder sql, List<Object> params) {
        LOGGER.info(() -> "[R3] SQL: " + sql + " | params=" + params);
    }

    private R3MonthlyThroughputRow mapRow(ResultSet rs) throws SQLException {
        R3MonthlyThroughputRow row = new R3MonthlyThroughputRow();
        row.setYear(rs.getInt("year"));
        row.setMonth(rs.getInt("month"));
        row.setYearMonth(rs.getString("year_month"));
        Long cid = rs.getLong("customer_id");
        row.setCustomerId(rs.wasNull() ? null : cid);
        row.setCustomerName(rs.getString("customer_name"));
        Long bid = rs.getLong("branch_id");
        row.setBranchId(rs.wasNull() ? null : bid);
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
}

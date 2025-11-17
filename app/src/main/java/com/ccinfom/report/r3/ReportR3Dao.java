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

        if (filters.getProductId() != null) {
            sql.append("AND product_categories LIKE ? ");
            params.add("%" + filters.getProductId() + "%");
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

        if (filters.getProductId() != null) {
            sql.append("AND product_categories LIKE ? ");
            params.add("%" + filters.getProductId() + "%");
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


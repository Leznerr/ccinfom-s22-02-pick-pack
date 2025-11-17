package com.ccinfom.report.r4;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.ccinfom.report.ReportDaoBase;

/**
 * TODO[Phase F - R4]:
 *  - Query v_r4_on_time_delivery to retrieve on-time / late / PoD compliance data.
 *  - Handle optional filters like vehicle, driver, customer, or date range.
 *  - Ensure the DAO exposes both detailed rows and aggregate KPIs for the UI.
 */
public class ReportR4Dao extends ReportDaoBase {

    // Stored ONLY because the UI passes it.
    // ReportDaoBase still uses DbConnection.getConnection() internally.
    @SuppressWarnings("unused")
    private final Connection externalConn;

    /** Constructor required by ReportR4Form */
    public ReportR4Dao(Connection conn) {
        this.externalConn = conn;
    }

    /**
     * UI calls this:
     *    List<R4OnTimeDeliveryRow> rows = dao.findR4(...)
     *
     * @param year  selected year
     * @param isMonth true if MONTH mode; false if WEEK (ISO week)
     * @param period month number (1–12) OR ISO week number
     * @param vehicle optional vehicle filter
     * @param driver optional driver filter
     */
    public List<R4OnTimeDeliveryRow> findR4(
            int year,
            boolean isMonth,
            int period,
            String vehicle,
            String driver) throws SQLException {

        ReportFilters filters = new ReportFilters();

        filters.setYear(year);

        if (isMonth) {
            filters.setMonth(period);
        } else {
            filters.setIsoWeek(period);
        }

        if (vehicle != null && !vehicle.equals("(All)") && !vehicle.isBlank()) {
            filters.setVehicleName(vehicle);
        }

        if (driver != null && !driver.equals("(All)") && !driver.isBlank()) {
            filters.setDriverName(driver);
        }

        return findRows(filters);
    }

    /**
     * Main query method.
     * Pulls rows from v_r4_on_time_delivery using dynamic ReportFilters.
     */
    public List<R4OnTimeDeliveryRow> findRows(ReportFilters filters) throws SQLException {
        String sql = buildQuery(filters);

        return executeQuery(sql,
                ps -> bindFilters(ps, filters),
                this::mapRow
        );
    }

    /**
     * Builds SQL with optional WHERE clauses based on provided filters.
     */
    private String buildQuery(ReportFilters filters) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ")
           .append("delivery_year, delivery_month, year_month_label, ")
           .append("customer_id, customer_name, ")
           .append("product_id, sku, product_name, category, ")
           .append("on_time_deliveries, late_deliveries, short_closed_shipments, ")
           .append("shortage_reasons_count, on_time_percentage ")
           .append("FROM v_r4_on_time_delivery ")
           .append("WHERE 1=1 ");

        if (filters.getYear() != null) {
            sql.append(" AND delivery_year = ?");
        }
        if (filters.getMonth() != null) {
            sql.append(" AND delivery_month = ?");
        }
        if (filters.getIsoWeek() != null) {
            sql.append(" AND iso_week = ?");
        }
        if (filters.getVehicleName() != null) {
            sql.append(" AND vehicle = ?");
        }
        if (filters.getDriverName() != null) {
            sql.append(" AND driver = ?");
        }
        if (filters.getCustomerId() != null) {
            sql.append(" AND customer_id = ?");
        }
        if (filters.getProductCategory() != null && !filters.getProductCategory().isBlank()) {
            sql.append(" AND product_category LIKE ?");
        }

        sql.append(" ORDER BY delivery_year DESC, delivery_month DESC, customer_name, product_name");

        return sql.toString();
    }

    /**
     * Binds filter parameters to the PreparedStatement in the same order
     * used in buildQuery().
     */
    private void bindFilters(java.sql.PreparedStatement ps, ReportFilters filters) throws SQLException {

        int idx = 1;

        if (filters.getYear() != null) {
            ps.setInt(idx++, filters.getYear());
        }
        if (filters.getMonth() != null) {
            ps.setInt(idx++, filters.getMonth());
        }
        if (filters.getIsoWeek() != null) {
            ps.setInt(idx++, filters.getIsoWeek());
        }
        if (filters.getVehicleName() != null) {
            ps.setString(idx++, filters.getVehicleName());
        }
        if (filters.getDriverName() != null) {
            ps.setString(idx++, filters.getDriverName());
        }
        if (filters.getCustomerId() != null) {
            ps.setLong(idx++, filters.getCustomerId());
        }
        if (filters.getProductCategory() != null && !filters.getProductCategory().isBlank()) {
            ps.setString(idx++, "%" + filters.getProductCategory().trim() + "%");
        }
    }

    /**
     * Maps ResultSet row → R4OnTimeDeliveryRow DTO.
     * Must exactly match view column names.
     */
    private R4OnTimeDeliveryRow mapRow(java.sql.ResultSet rs) throws SQLException {

        R4OnTimeDeliveryRow row = new R4OnTimeDeliveryRow();

        row.setDeliveryYear(rs.getInt("delivery_year"));
        row.setDeliveryMonth(rs.getInt("delivery_month"));
        row.setYearMonthLabel(rs.getString("year_month_label"));

        row.setCustomerId(rs.getLong("customer_id"));
        row.setCustomerName(rs.getString("customer_name"));

        row.setProductId(rs.getLong("product_id"));
        row.setSku(rs.getString("sku"));
        row.setProductName(rs.getString("product_name"));
        row.setCategory(rs.getString("category"));

        row.setOnTimeDeliveries(rs.getInt("on_time_deliveries"));
        row.setLateDeliveries(rs.getInt("late_deliveries"));
        row.setShortClosedShipments(rs.getInt("short_closed_shipments"));
        row.setShortageReasonsCount(rs.getInt("shortage_reasons_count"));

        row.setOnTimePercentage(rs.getBigDecimal("on_time_percentage"));

        return row;
    }


} // end

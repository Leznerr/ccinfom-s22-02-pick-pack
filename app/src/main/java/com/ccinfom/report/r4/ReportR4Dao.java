package com.ccinfom.report.r4;

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

    // TODO: Implement methods such as List<ReportR4Row> findOnTimeDeliveries(ReportFilters filters)

    /**
     * Returns detailed per-month/customer/product rows from v_r4_on_time_delivery.
     */
    public List<R4OnTimeDeliveryRow> findRows(ReportFilters filters) throws SQLException {
        String sql = buildQuery(filters);
        
        return executeQuery(sql, 
            ps -> bindFilters(ps, filters),
            this::mapRow
        );
    }

    /**
     * Builds the SQL query with dynamic WHERE clauses based on filters.
     */
    private String buildQuery(ReportFilters filters) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append("delivery_year, delivery_month, year_month_label, ")
           .append("customer_id, customer_name, product_id, sku, product_name, category, ")
           .append("on_time_deliveries, late_deliveries, short_closed_shipments, ")
           .append("shortage_reasons_count, on_time_percentage ")
           .append("FROM v_r4_on_time_delivery ")
           .append("WHERE 1=1");
        
        // Add dynamic filters
        if (filters.getYear() != null) {
            sql.append(" AND delivery_year = ?");
        }
        if (filters.getMonth() != null) {
            sql.append(" AND delivery_month = ?");
        }
        if (filters.getCustomerId() != null) {
            sql.append(" AND customer_id = ?");
        }
        if (filters.getProductId() != null) {
            sql.append(" AND product_id = ?");
        }
        
        sql.append(" ORDER BY delivery_year DESC, delivery_month DESC, customer_name, product_name");
        
        return sql.toString();
    }

    /**
     * Binds filter parameters to the prepared statement.
     */
    private void bindFilters(java.sql.PreparedStatement ps, ReportFilters filters) throws SQLException {
        int paramIndex = 1;
        
        if (filters.getYear() != null) {
            ps.setInt(paramIndex++, filters.getYear());
        }
        if (filters.getMonth() != null) {
            ps.setInt(paramIndex++, filters.getMonth());
        }
        if (filters.getCustomerId() != null) {
            ps.setLong(paramIndex++, filters.getCustomerId());
        }
        if (filters.getProductId() != null) {
            ps.setLong(paramIndex++, filters.getProductId());
        }
    }

    /**
     * Maps a single ResultSet row to R4OnTimeDeliveryRow DTO.
     * Uses correct column names from v_r4_on_time_delivery view.
     */
    private R4OnTimeDeliveryRow mapRow(java.sql.ResultSet rs) throws SQLException {
        R4OnTimeDeliveryRow row = new R4OnTimeDeliveryRow();
        
        // Time dimensions - matches your view columns
        row.setDeliveryYear(rs.getInt("delivery_year"));
        row.setDeliveryMonth(rs.getInt("delivery_month"));
        row.setYearMonthLabel(rs.getString("year_month_label"));
        
        // Customer dimensions - matches your view columns
        row.setCustomerId(rs.getLong("customer_id"));
        row.setCustomerName(rs.getString("customer_name"));
        
        // Product dimensions - matches your view columns
        row.setProductId(rs.getLong("product_id"));
        row.setSku(rs.getString("sku"));
        row.setProductName(rs.getString("product_name"));
        row.setCategory(rs.getString("category"));
        
        // Core metrics - matches your view columns
        row.setOnTimeDeliveries(rs.getInt("on_time_deliveries"));
        row.setLateDeliveries(rs.getInt("late_deliveries"));
        row.setShortClosedShipments(rs.getInt("short_closed_shipments"));
        row.setShortageReasonsCount(rs.getInt("shortage_reasons_count"));
        row.setOnTimePercentage(rs.getBigDecimal("on_time_percentage"));
        
        return row;
    }

} // end

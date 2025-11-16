package com.ccinfom.report.r2;

import com.ccinfom.report.ReportDaoBase;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO wrapper for v_r2_weekly_picker_productivity.
 */
public class ReportR2Dao extends ReportDaoBase {

    /**
     * Retrieves weekly picker productivity for the given ISO year and optional ISO week.
     *
     * @param isoYear required ISO year (e.g., 2025)
     * @param isoWeek optional ISO week (1-53); when null, returns the whole year
     * @return immutable DTO list
     * @throws SQLException if database access fails
     */
    public List<R2WeeklyProductivityRow> findWeeklyProductivity(int isoYear, Integer isoWeek) throws SQLException {
        return findWeeklyProductivity(isoYear, isoWeek, null, null);
    }

    /**
     * Retrieves productivity filtered by optional picker and product category.
     */
    public List<R2WeeklyProductivityRow> findWeeklyProductivity(int isoYear, Integer isoWeek,
                                                   Long pickerEmployeeId, String productCategory) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    iso_year,
                    iso_week,
                    iso_week_start,
                    iso_week_end,
                    picker_employee_id,
                    first_name,
                    last_name,
                    category,
                    lines_picked,
                    units_picked,
                    units_per_hour,
                    avg_pick_time_min,
                    short_error_lines
                FROM v_r2_weekly_picker_productivity
                WHERE iso_year = ?
                """);

        if (isoWeek != null) {
            sql.append(" AND iso_week = ?");
        }
        if (pickerEmployeeId != null) {
            sql.append(" AND picker_employee_id = ?");
        }
        if (productCategory != null) {
            sql.append(" AND category = ?");
        }

        sql.append("""
                \nORDER BY 
                    iso_year DESC, 
                    iso_week DESC, 
                    picker_employee_id, 
                    category
                """);

        return executeQuery(sql.toString(), ps -> {
            int idx = 1;
            ps.setInt(idx++, isoYear);
            if (isoWeek != null) {
                ps.setInt(idx++, isoWeek);
            }
            if (pickerEmployeeId != null) {
                ps.setLong(idx++, pickerEmployeeId);
            }
            if (productCategory != null) {
                ps.setString(idx++, productCategory);
            }
        }, this::mapRow);
    }

    /**
     * Alternative method using R2ProductivityFilters for more complex filtering scenarios
     */
    public List<R2WeeklyProductivityRow> findWeeklyProductivity(R2ProductivityFilters filters) throws SQLException {
        return findWeeklyProductivity(
            filters.getIsoYear(),
            filters.getIsoWeek(),
            filters.getPickerEmployeeId(),
            filters.getProductCategory()
        );
    }

    private R2WeeklyProductivityRow mapRow(ResultSet rs) throws SQLException {
        int isoYear = rs.getInt("iso_year");
        int isoWeek = rs.getInt("iso_week");
        LocalDate isoWeekStart = rs.getObject("iso_week_start", LocalDate.class);
        LocalDate isoWeekEnd = rs.getObject("iso_week_end", LocalDate.class);
        Long pickerEmployeeId = rs.getObject("picker_employee_id", Long.class);
        String pickerFirstName = rs.getString("first_name");
        String pickerLastName = rs.getString("last_name");
        String productCategory = rs.getString("category");
        Long linesPicked = rs.getObject("lines_picked", Long.class);
        BigDecimal unitsPicked = rs.getBigDecimal("units_picked");
        BigDecimal unitsPerHour = rs.getBigDecimal("units_per_hour");
        Long avgPickTimeMin = rs.getObject("avg_pick_time_min", Long.class);
        Long shortErrorLines = rs.getObject("short_error_lines", Long.class);

        // Handle potential null values
        if (rs.wasNull()) {
            linesPicked = 0L;
            unitsPicked = BigDecimal.ZERO;
            unitsPerHour = BigDecimal.ZERO;
            avgPickTimeMin = 0L;
            shortErrorLines = 0L;
        }

        return new R2WeeklyProductivityRow(
            isoYear,
            isoWeek,
            isoWeekStart,
            isoWeekEnd,
            pickerEmployeeId,
            pickerFirstName,
            pickerLastName,
            productCategory,
            linesPicked,
            unitsPicked,
            unitsPerHour,
            avgPickTimeMin,
            shortErrorLines
        );
    }
}
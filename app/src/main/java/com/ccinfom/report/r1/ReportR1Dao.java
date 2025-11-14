package com.ccinfom.report.r1;

import com.ccinfom.report.ReportDaoBase;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO wrapper for v_r1_daily_outcomes.
 */
public class ReportR1Dao extends ReportDaoBase {

    /**
     * Retrieves daily outcomes for the given year and optional month.
     *
     * @param year  required calendar year (e.g., 2025)
     * @param month optional calendar month (1-12); when null, returns the whole year
     * @return immutable DTO list
     * @throws SQLException if database access fails
     */
    public List<R1DailyOutcomeRow> findDailyOutcomes(int year, Integer month) throws SQLException {
        return findDailyOutcomes(year, month, null, null);
    }

    /**
     * Retrieves outcomes filtered by optional customer/branch.
     */
    public List<R1DailyOutcomeRow> findDailyOutcomes(int year, Integer month,
                                                     Long customerId, Long branchId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT DATE(ch.created_at) AS calendar_date,
                       YEAR(ch.created_at) AS calendar_year,
                       MONTH(ch.created_at) AS calendar_month,
                       DATE_FORMAT(ch.created_at, '%M') AS month_name_label,
                       DATE_FORMAT(ch.created_at, '%W') AS day_name_label,
                       COUNT(DISTINCT CASE WHEN ch.final_status = 'Delivered' THEN ch.close_id END) AS delivered_ticket_count,
                       COUNT(DISTINCT CASE WHEN ch.final_status = 'Short-Closed' THEN ch.close_id END) AS short_closed_ticket_count,
                       COUNT(DISTINCT ch.close_id) AS tickets_closed,
                       COUNT(CASE WHEN cv.short_qty > 0 THEN cv.variance_id END) AS shortage_line_count,
                       COALESCE(SUM(CASE WHEN cv.short_qty > 0 THEN cv.short_qty ELSE 0 END), 0) AS shortage_units
                  FROM close_hdr ch
                  JOIN close_variance cv ON cv.close_id = ch.close_id
                  JOIN pick_ticket_hdr pth ON ch.pick_ticket_id = pth.pick_ticket_id
                 WHERE YEAR(ch.created_at) = ?
                """);

        if (month != null) {
            sql.append(" AND MONTH(ch.created_at) = ?");
        }
        if (customerId != null) {
            sql.append(" AND pth.customer_id = ?");
        }
        if (branchId != null) {
            sql.append(" AND pth.branch_id = ?");
        }

        sql.append("""
                \nGROUP BY DATE(ch.created_at),
                         YEAR(ch.created_at),
                         MONTH(ch.created_at),
                         DATE_FORMAT(ch.created_at, '%M'),
                         DATE_FORMAT(ch.created_at, '%W')
                ORDER BY calendar_date
                """);

        return executeQuery(sql.toString(), ps -> {
            int idx = 1;
            ps.setInt(idx++, year);
            if (month != null) {
                ps.setInt(idx++, month);
            }
            if (customerId != null) {
                ps.setLong(idx++, customerId);
            }
            if (branchId != null) {
                ps.setLong(idx++, branchId);
            }
        }, this::mapRow);
    }

    private R1DailyOutcomeRow mapRow(ResultSet rs) throws SQLException {
        LocalDate date = rs.getObject("calendar_date", LocalDate.class);
        int year = rs.getInt("calendar_year");
        int month = rs.getInt("calendar_month");
        String monthLabel = rs.getString("month_name_label");
        String dayLabel = rs.getString("day_name_label");
        int delivered = rs.getInt("delivered_ticket_count");
        int shortClosed = rs.getInt("short_closed_ticket_count");
        int closed = rs.getInt("tickets_closed");
        int shortageLines = rs.getInt("shortage_line_count");
        BigDecimal shortageUnits = rs.getBigDecimal("shortage_units");

        return new R1DailyOutcomeRow(
                date,
                year,
                month,
                monthLabel,
                dayLabel,
                delivered,
                shortClosed,
                closed,
                shortageLines,
                shortageUnits
        );
    }
}

package com.ccinfom.report.r3;

import com.ccinfom.report.ReportDaoBase;
import com.ccinfom.report.r4.ReportFilters;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO for R3 - Monthly pack-to-dispatch performance by branch.
 * Computes minutes from box sealed to dispatch departure, SLA adherence, and exceptions.
 */
public class ReportR3Dao extends ReportDaoBase {

    public List<R3MonthlyThroughputRow> findMonthlyThroughput(ReportFilters filters) throws SQLException {
        if (filters == null || filters.getYear() == null || filters.getMonth() == null) {
            throw new SQLException("Year and month are required");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                  be.yr AS year,
                  be.mon AS month,
                  CONCAT(be.yr, '-', LPAD(be.mon, 2, '0')) AS year_month_label,
                  be.branch_id,
                  be.branch_name,
                  COUNT(*) AS shipments_packed,
                  ROUND(AVG(be.minutes_to_dispatch), 2) AS avg_minutes_to_dispatch,
                  ROUND(
                    CAST(
                      SUBSTRING_INDEX(
                        SUBSTRING_INDEX(GROUP_CONCAT(be.minutes_to_dispatch ORDER BY be.minutes_to_dispatch SEPARATOR ','), ',', FLOOR((COUNT(*) + 1) / 2)),
                        ',', -1
                      ) AS DECIMAL(12,2)
                    ), 2
                  ) AS median_minutes_to_dispatch,
                  ROUND(100 * SUM(CASE WHEN be.sla_within_flag = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS sla_within_pct,
                  SUM(be.exception_flag) AS exceptions_count
                FROM (
                  SELECT
                    pb.box_id,
                    pth.branch_id,
                    b.branch_name,
                    YEAR(pb.sealed_at) AS yr,
                    MONTH(pb.sealed_at) AS mon,
                    pb.sealed_at,
                    dh.depart_ts,
                    v.sla_hours,
                    TIMESTAMPDIFF(MINUTE, pb.sealed_at, dh.depart_ts) AS minutes_to_dispatch,
                    CASE
                      WHEN dh.depart_ts IS NULL THEN 1
                      WHEN TIMESTAMPDIFF(MINUTE, pb.sealed_at, dh.depart_ts) > v.sla_hours * 60 THEN 1
                      ELSE 0
                    END AS exception_flag,
                    CASE
                      WHEN dh.depart_ts IS NOT NULL
                       AND TIMESTAMPDIFF(MINUTE, pb.sealed_at, dh.depart_ts) <= v.sla_hours * 60 THEN 1
                      ELSE 0
                    END AS sla_within_flag
                  FROM pack_box_hdr pb
                  JOIN pick_ticket_hdr pth ON pb.pick_ticket_id = pth.pick_ticket_id
                  JOIN branches b ON pth.branch_id = b.branch_id
                  JOIN dispatch_line dl ON dl.box_id = pb.box_id
                  JOIN dispatch_hdr dh ON dh.dispatch_id = dl.dispatch_id
                  JOIN vehicles v ON dh.vehicle_id = v.vehicle_id
                  WHERE pb.sealed_flag = TRUE
                    AND pb.sealed_at IS NOT NULL
                    AND dh.depart_ts IS NOT NULL
                    AND YEAR(pb.sealed_at) = ?
                    AND MONTH(pb.sealed_at) = ?
                """);

        if (filters.getBranchId() != null) {
            sql.append("    AND pth.branch_id = ?\n");
        }

        sql.append("""
                ) AS be
                GROUP BY be.yr, be.mon, be.branch_id, be.branch_name
                ORDER BY be.yr, be.mon, be.branch_name
                """);

        return executeQuery(sql.toString(), ps -> {
            int idx = 1;
            ps.setInt(idx++, filters.getYear());
            ps.setInt(idx++, filters.getMonth());
            if (filters.getBranchId() != null) {
                ps.setLong(idx++, filters.getBranchId());
            }
        }, this::mapRow);
    }

    private R3MonthlyThroughputRow mapRow(ResultSet rs) throws SQLException {
        R3MonthlyThroughputRow row = new R3MonthlyThroughputRow();
        row.setYear(rs.getInt("year"));
        row.setMonth(rs.getInt("month"));
        row.setYearMonth(rs.getString("year_month_label"));
        Object branchIdObj = rs.getObject("branch_id");
        row.setBranchId(branchIdObj == null ? null : ((Number) branchIdObj).longValue());
        row.setBranchName(rs.getString("branch_name"));
        row.setShipmentsPacked(rs.getInt("shipments_packed"));
        row.setAvgMinutesToDispatch(rs.getBigDecimal("avg_minutes_to_dispatch"));
        row.setMedianMinutesToDispatch(rs.getBigDecimal("median_minutes_to_dispatch"));
        row.setSlaWithinPct(rs.getBigDecimal("sla_within_pct"));
        row.setExceptionsCount(rs.getInt("exceptions_count"));
        return row;
    }
}

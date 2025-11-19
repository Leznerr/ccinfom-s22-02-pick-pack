package com.ccinfom.report;

import com.ccinfom.config.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for report DAOs providing shared JDBC helpers.
 *  - Centralizes connection handling and try-with-resources patterns.
 *  - Supplies small functional interfaces for binding parameters and mapping rows.
 *  - Exposes helpers for common date-range binding logic.
 */
public abstract class ReportDaoBase {

    @FunctionalInterface
    protected interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected Connection getConnection() throws SQLException {
        return DbConnection.getConnection();
    }

    /**
     * Executes a query with the provided binder + mapper and returns the mapped list.
     */
    protected <T> List<T> executeQuery(String sql, StatementBinder binder, RowMapper<T> mapper) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        }
    }

    /**
     * Convenience overload for simple queries without custom binding.
     */
    protected <T> List<T> executeQuery(String sql, RowMapper<T> mapper) throws SQLException {
        return executeQuery(sql, null, mapper);
    }

    /**
     * Binds a date range as TIMESTAMP parameters (inclusive start/end day boundaries).
     *
     * @return next parameter index after the bound values.
     */
    protected int bindDateRange(PreparedStatement ps, int startIndex, LocalDate startDate, LocalDate endDate) throws SQLException {
        if (startDate != null) {
            ps.setTimestamp(startIndex++, toTimestamp(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            LocalDateTime endOfDay = endDate.atTime(LocalTime.MAX);
            ps.setTimestamp(startIndex++, toTimestamp(endOfDay));
        }
        return startIndex;
    }

    protected SQLException wrapSqlException(String context, Exception cause) {
        if (cause instanceof SQLException se) {
            return se;
        }
        return new SQLException(context, cause);
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }
}

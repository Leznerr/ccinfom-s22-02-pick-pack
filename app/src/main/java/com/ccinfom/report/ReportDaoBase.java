package com.ccinfom.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * TODO[Phase F]:
 *  - Provide reusable JDBC helpers for report DAOs (connection retrieval, parameter binding, DTO mapping).
 *  - Consider injecting DataSource/DbConnection for testability.
 *  - Expose protected methods that child DAOs (ReportR1Dao, etc.) can call to execute queries safely.
 */
public abstract class ReportDaoBase {

    // TODO: Inject/configure DbConnection helper; placeholder methods below illustrate intent.

    protected Connection getConnection() {
        // TODO: Return a JDBC connection using the existing DbConnection utility.
        return null;
    }

    protected void closeQuietly(ResultSet rs, PreparedStatement ps, Connection conn) {
        // TODO: Implement proper resource cleanup (or switch to try-with-resources in callers).
    }
}

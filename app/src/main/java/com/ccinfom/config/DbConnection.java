/*
 * CCINFOM — Phase D
 * File: DbConnection.java
 * Purpose: Create JDBC connections using MySQL Connector/J.
 *
 * Responsibilities:
 *  - Build JDBC URL: jdbc:mysql://<host>:<port>/<schema>?serverTimezone=UTC
 *  - Provide getConnection() using values from DbConfig.
 *  - Provide closeQuietly(ResultSet/Statement/Connection) helpers.
 *
 * TODOs:
 *  [ ] Implement getConnection() with DriverManager and proper exceptions.
 *  [ ] Add simple ping method (SELECT 1) for manual testing.
 *  [ ] Ensure auto-commit defaults are left to callers (Services).
 *
 * Definition of Done:
 *  - getConnection() succeeds with valid config; fails with clear message on bad config.
 *  - No resource leaks (closeQuietly works in finally blocks).
 *
 * Pitfalls:
 *  - Do not cache a single global Connection (create per operation).
 *  - Ensure MySQL driver jar is on classpath (lib/mysql-connector-j-*.jar).
 */

 package com.ccinfom.config;

 import java.sql.*;

 public class DbConnection {

    public static Connection getConnection() throws SQLException {
        DbConfig config = new DbConfig();

        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=false", config.getHost(), config.getPort(), config.getSchema());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }

        return DriverManager.getConnection(url, config.getUser(), config.getPassword());
    }
    
    /**
     * Simple connectivity test (runs SELECT 1).
     */
    public static boolean ping() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                System.out.println("Database ping successful!");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Database ping failed: " + e.getMessage());
        }

        return false;
    }

    // --- Utility methods for closing resources ---

    /** Quietly closes a ResultSet (no exception thrown). */
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try { 
                rs.close(); 
            } catch (SQLException ignored) {}
        }
    }

    /** Quietly closes a Statement (no exception thrown). */
    public static void closeQuietly(Statement st) {
        if (st != null) {
            try { 
                st.close(); 
            } catch (SQLException ignored) {}
        }
    }

    /** Quietly closes a Connection (no exception thrown). */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try { 
                conn.close(); 
            } catch (SQLException ignored) {}
        }
    }

    // Manual test entry point
    public static void main(String[] args) {
        if (ping()) {
            System.out.println("Connection test passed.");
        } else {
            System.out.println("Connection test failed.");
        }
    }
 }

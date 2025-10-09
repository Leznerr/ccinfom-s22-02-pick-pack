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

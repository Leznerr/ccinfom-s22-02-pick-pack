/*
 * CCINFOM — Phase D
 * File: DbConfig.java
 * Purpose: Load DB settings from classpath resource `dbconfig.properties`
 *          and (optionally) an external path override via system property:
 *          -Dccinfom.dbconfig=/path/to/dbconfig.properties
 *
 * Responsibilities:
 *  - Load and validate: db.host, db.port, db.schema, db.user, db.password.
 *  - Provide getters for other classes (no UI code here).
 *
 * TODOs:
 *  [ ] Implement load order: external override > classpath.
 *  [ ] Validate required keys; throw clear IllegalStateException if missing.
 *  [ ] Never log or print passwords.
 *
 * Definition of Done:
 *  - Calling new DbConfig().getHost()/getUser() returns correct values.
 *  - If a required key is missing → clear error message, no NPEs.
 *  - Unit smoke: can print "DB host: <host>" without exposing secrets.
 *
 * Pitfalls:
 *  - Don’t swallow exceptions (bubble them up with context).
 *  - Keep this class immutable after construction.
 */

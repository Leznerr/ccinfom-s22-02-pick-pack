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

package com.ccinfom.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {

    private final String host;
    private final String port;
    private final String schema;
    private final String user;
    private final String password;

    public DbConfig() {
        Properties props = new Properties();

        // Optional external override
        String externalPath = System.getProperty("ccinfom.dbconfig");

        try (InputStream input = (externalPath != null)
                ? new FileInputStream(externalPath)
                : getClass().getClassLoader().getResourceAsStream("dbconfig.properties")) {

            if (input == null) {
                throw new IllegalStateException("dbconfig.properties not found in classpath or external path.");
            }

            props.load(input);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dbconfig.properties: " + e.getMessage(), e);
        }

        // Validate required properties
        host = require(props, "db.host");
        port = require(props, "db.port");
        schema = require(props, "db.schema");
        user = require(props, "db.user");
        password = require(props, "db.password");
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required DB config key: " + key);
        }
        
        return value.trim();
    }

    // Getters
    public String getHost() 
    { 
        return host; 
    }

    public String getPort() 
    { 
        return port; 
    }

    public String getSchema() 
    { 
        return schema; 
    }

    public String getUser() 
    { 
        return user;
    }

    public String getPassword() 
    { 
        return password; 
    }

    // Quick manual test
    public static void main(String[] args) {
        try {
            DbConfig config = new DbConfig();

            System.out.println("DB host: " + config.getHost());
            System.out.println("DB schema: " + config.getSchema());
            System.out.println("DB user: " + config.getUser());
        } catch (Exception e) {
            System.err.println("Failed to load DB config: " + e.getMessage());
        }
    }
}

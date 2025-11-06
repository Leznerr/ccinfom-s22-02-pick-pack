/*
 * CCINFOM â€" Phase D
 * File: DbConfig.java
 * Purpose: Load DB settings from environment variables or a properties file.
 *          The load order is: 
 *          1. Environment Variables (e.g., DB_HOST, DB_USER)
 *          2. Java System Property (-Dccinfom.dbconfig=/path/to/dbconfig.properties)
 *          3. Classpath resource (dbconfig.properties)
 *
 * Responsibilities:
 *  - Load and validate: db.host, db.port, db.schema, db.user, db.password.
 *  - Provide getters for other classes (no UI code here).
 *
 * TODOs:
 *  [x] Implement load order: env > external override > classpath.
 *  [x] Validate required keys; throw clear IllegalStateException if missing.
 *  [x] Never log or print passwords.
 *
 * Definition of Done:
 *  - Calling new DbConfig().getHost()/getUser() returns correct values.
 *  - If a required key is missing â†' clear error message, no NPEs.
 *  - Unit smoke: can print "DB host: <host>" without exposing secrets.
 *
 * Pitfalls:
 *  - Donâ€™t swallow exceptions (bubble them up with context).
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
                // This is not a fatal error; we can still proceed if environment variables are set
                System.out.println("INFO: dbconfig.properties not found. Relying on environment variables.");
            } else {
                props.load(input);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dbconfig.properties: " + e.getMessage(), e);
        }

        // Validate required properties, prioritizing environment variables
        host = getenvOrElse(props, "db.host");
        port = getenvOrElse(props, "db.port");
        schema = getenvOrElse(props, "db.schema");
        user = getenvOrElse(props, "db.user");
        password = getenvOrElse(props, "db.password");
    }

    private static String getenvOrElse(Properties props, String key) {
        String envVar = getEnvVarForKey(key);
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        // Fallback to properties file
        return require(props, key);
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            // If we are here, it means env var was not set and property is missing.
            String envVar = getEnvVarForKey(key);
            throw new IllegalStateException(
                "Missing required DB config. Set environment variable \"" + envVar + "\" or add '" + key + "' to dbconfig.properties");
        }
        
        return value.trim();
    }

    private static String getEnvVarForKey(String key) {
        return key.replace("db.", "DB_").toUpperCase();
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

    public String getUrl() {
        // Include allowPublicKeyRetrieval=true&useSSL=false for MySQL 8+
        return String.format(
            "jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true",
            host, port, schema
        );
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


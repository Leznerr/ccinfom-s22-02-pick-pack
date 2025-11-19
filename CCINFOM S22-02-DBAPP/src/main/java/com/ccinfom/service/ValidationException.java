package com.ccinfom.service;

/**
 * Custom exception to represent business rule or data validation failures.
 * Optionally carries a short code so tests/UI can react deterministically.
 */
public class ValidationException extends Exception {

    private final String code;

    public ValidationException(String message) {
        super(message);
        this.code = null;
    }

    public ValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}


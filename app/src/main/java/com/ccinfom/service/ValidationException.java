package com.ccinfom.service;

/**
 * Custom exception to represent business rule or data validation failures.
 * This helps in providing clear, user-friendly error messages to the UI layer.
 */
public class ValidationException extends Exception {

    /**
     * Constructs a new ValidationException with the specified detail message.
     * @param message the detail message.
     */
    public ValidationException(String message) {
        super(message);
    }
}
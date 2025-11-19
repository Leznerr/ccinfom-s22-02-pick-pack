package com.ccinfom.util;

import com.ccinfom.service.ValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Shared validation helpers for core record management.
 * Keeps phone, uniqueness, and capacity checks consistent
 * across services and future CRUD modules.
 */
public final class CoreValidationUtil {

    private static final Pattern PH_MOBILE = Pattern.compile("^09\\d{9}$");
    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private CoreValidationUtil() {
    }

    public static void requireNonBlank(String value, String code, String message) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(code, message);
        }
    }

    public static void validatePhoneFormat(String phone, String code, String fieldLabel) throws ValidationException {
        if (phone == null || phone.isBlank()) {
            return;
        }
        if (!PH_MOBILE.matcher(phone).matches()) {
            throw new ValidationException(code,
                    fieldLabel + " must follow the PH 11-digit format (09XXXXXXXXX).");
        }
    }

    public static void validateEmailFormat(String email, String code, String fieldLabel) throws ValidationException {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!SIMPLE_EMAIL.matcher(email).matches()) {
            throw new ValidationException(code,
                    fieldLabel + " must be a valid email address.");
        }
    }

    public static void ensureNotDuplicate(boolean alreadyExists, String code, String message)
            throws ValidationException {
        if (alreadyExists) {
            throw new ValidationException(code, message);
        }
    }

    public static void ensureCapacityAvailable(int capacity, int currentLoad, int requestedBoxes,
                                               String code) throws ValidationException {
        if (capacity < 0) {
            throw new ValidationException(code,
                    "Vehicle capacity cannot be negative (" + capacity + ").");
        }
        if (currentLoad + requestedBoxes > capacity) {
            throw new ValidationException(code,
                    "Vehicle capacity exceeded. Current load=" + currentLoad
                            + ", requested=" + requestedBoxes + ", capacity=" + capacity);
        }
    }

    public static void requireNonNegative(BigDecimal value, String code, String fieldLabel)
            throws ValidationException {
        if (value == null || value.signum() < 0) {
            throw new ValidationException(code, fieldLabel + " cannot be negative.");
        }
    }
}

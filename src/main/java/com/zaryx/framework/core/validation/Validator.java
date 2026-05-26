package com.zaryx.framework.core.validation;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Framework validation system.
 * Provides common, reusable validators.
 */
public class Validator {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern URL_PATTERN =
        Pattern.compile("^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:, .;]*[-a-zA-Z0-9+&@#/%=~_|]$");
    private static final Pattern UUID_PATTERN =
        Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    /**
     * Validates that a string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validates that a string has a specific length range
     */
    public static boolean hasLength(String value, int minLength, int maxLength) {
        if (value == null) return minLength <= 0;
        int length = value.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates that a string is an email
     */
    public static boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a string is a URL
     */
    public static boolean isURL(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a string is a UUID
     */
    public static boolean isUUID(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }

    /**
     * Validates that a number is within a range
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a number is within a range
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a collection is not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Validates that a map is not empty
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * Validates that an array is not empty
     */
    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    /**
     * Validates that a string is numeric
     */
    public static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates that a string is an integer
     */
    public static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates with a custom predicate
     */
    public static <T> boolean validate(T value, Predicate<T> predicate) {
        return predicate.test(value);
    }

    /**
     * Validates multiple conditions (all must be true)
     */
    @SafeVarargs
    public static boolean validateAll(Predicate<?>... predicates) {
        for (Predicate<?> predicate : predicates) {
            if (!predicate.test(null)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that at least one condition is true
     */
    @SafeVarargs
    public static boolean validateAny(Predicate<?>... predicates) {
        for (Predicate<?> predicate : predicates) {
            if (predicate.test(null)) {
                return true;
            }
        }
        return false;
    }
}

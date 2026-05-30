package com.zaryx.okaso.core.validation;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Validator {

    // Reusable patterns for common input checks.
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern URL_PATTERN =
        Pattern.compile("^(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:, .;]*[-a-zA-Z0-9+&@#/%=~_|]$");
    private static final Pattern UUID_PATTERN =
        Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // Validates that a string length stays inside a fixed range.
    public static boolean hasLength(String value, int minLength, int maxLength) {
        if (value == null) return minLength <= 0;
        int length = value.length();
        return length >= minLength && length <= maxLength;
    }

    public static boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    public static boolean isURL(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    public static boolean isUUID(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }

    // Checks inclusive integer bounds.
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    // Checks inclusive decimal bounds.
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static boolean isNotEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    // Validates that text can be parsed as a number.
    public static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Validates that text can be parsed as an integer.
    public static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Runs a custom predicate against a value.
    public static <T> boolean validate(T value, Predicate<T> predicate) {
        return predicate.test(value);
    }

    // Returns true only when every predicate accepts the input.
    @SafeVarargs
    public static boolean validateAll(Predicate<?>... predicates) {
        for (Predicate<?> predicate : predicates) {
            if (!predicate.test(null)) {
                return false;
            }
        }
        return true;
    }

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

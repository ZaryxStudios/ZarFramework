package com.zaryx.okaso.utility;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * General-purpose utilities for the okaso.
 */
public class OkasoUtils {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Validates that a name is allowed
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return VALID_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Capitalizes the first letter of a string
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Converts a string to PascalCase
     */
    public static String toPascalCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Converts a string to camelCase
     */
    public static String toCamelCase(String str) {
        String pascal = toPascalCase(str);
        if (pascal.isEmpty()) {
            return pascal;
        }
        return pascal.substring(0, 1).toLowerCase() + pascal.substring(1);
    }

    /**
     * Repeats a string n times
     */
    public static String repeat(String str, int times) {
        if (times <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Truncates a string to a maximum length
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns the current timestamp as a formatted string
     */
    public static String getCurrentTimestamp() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Returns the time difference between two timestamps
     */
    public static String getTimeDifference(long startTime, long endTime) {
        long diffMillis = endTime - startTime;

        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Converts bytes into a readable format
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Checks whether a string contains only numbers
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Splits a string into a list of strings
     */
    public static List<String> toList(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(delimiter));
    }

    /**
     * Joins a list into a string
     */
    public static String fromList(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(delimiter, list);
    }

    /**
     * Returns the simple class name
     */
    public static String getSimpleClassName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * Checks if two objects are equal (null-safe)
     */
    public static boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * Returns the first non-null element
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Creates a visual progress bar representation
     */
    public static String progressBar(int current, int total, int length) {
        if (total <= 0) {
            return "[" + repeat("=", length) + "]";
        }

        int filled = (int) ((double) current / total * length);
        int empty = length - filled;

        return "[" + repeat("=", filled) + repeat("-", empty) + "] " +
               current + "/" + total;
    }
}

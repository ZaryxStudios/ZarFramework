package com.zaryx.okaso.bukkit.utility;

import java.util.ArrayList;
import java.util.List;

public final class VersionSupport {

    // Version families used by the helper methods.
    public static final int FAMILY_LEGACY = 0;
    public static final int FAMILY_MODERN = 1;
    public static final int FAMILY_FUTURE = 2;

    private VersionSupport() {}

    // Lowest supported server version.
    public static final int[] MIN_VERSION = {1, 7, 10};

    // Highest supported server version.
    public static final int[] MAX_VERSION = {1, 21, 0};

    // Split a version token into numeric parts.
    public static int[] parseVersionToken(String token) {
        if (token == null) return new int[]{0};

        List<Integer> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char c : token.toCharArray()) {
            if (Character.isDigit(c)) {
                cur.append(c);
            } else {
                if (cur.length() > 0) {
                    parts.add(Integer.parseInt(cur.toString()));
                    cur.setLength(0);
                }
            }
        }
        if (cur.length() > 0) parts.add(Integer.parseInt(cur.toString()));

        if (parts.isEmpty()) return new int[]{0};
        int[] out = new int[parts.size()];
        for (int i = 0; i < parts.size(); i++) out[i] = parts.get(i);
        return out;
    }

    // Check whether the token is at least the required version.
    public static boolean isAtLeast(String token, int... required) {
        int[] actual = parseVersionToken(token);
        int len = Math.max(actual.length, required.length);
        for (int i = 0; i < len; i++) {
            int a = i < actual.length ? actual[i] : 0;
            int r = i < required.length ? required[i] : 0;
            if (a > r) return true;
            if (a < r) return false;
        }
        return true;
    }

    // Check whether the token is strictly below the required version.
    public static boolean isBefore(String token, int... required) {
        return !isAtLeast(token, required);
    }

    // Check whether the token is inside the provided version range.
    public static boolean isBetween(String token, int[] min, int[] max) {
        return isAtLeast(token, min) && isBefore(token, incrementVersion(max));
    }

    // Move the upper bound forward by one patch step.
    private static int[] incrementVersion(int[] version) {
        int[] result = version.clone();
        result[result.length - 1]++;
        return result;
    }

    // Legacy means versions before 1.17.
    public static boolean isLegacy(String token) {
        return isBetween(token, new int[]{1, 8, 0}, new int[]{1, 17, 0});
    }

    // Modern means 1.17 and newer.
    public static boolean isModern(String token) {
        return isAtLeast(token, 1, 17);
    }

    // Map the token to one of the supported families.
    public static int getVersionFamily(String token) {
        if (isAtLeast(token, 1, 21)) return FAMILY_FUTURE;
        if (isAtLeast(token, 1, 17)) return FAMILY_MODERN;
        return FAMILY_LEGACY;
    }

    // Return a human-readable family name.
    public static String getVersionFamilyName(String token) {
        switch (getVersionFamily(token)) {
            case FAMILY_FUTURE: return "FUTURE";
            case FAMILY_MODERN: return "MODERN";
            default: return "LEGACY";
        }
    }

    // Return the major version as major.minor.
    public static String getMajorVersion(String token) {
        int[] parts = parseVersionToken(token);
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return token;
    }

    // Return the minor version number.
    public static int getMinorVersion(String token) {
        int[] parts = parseVersionToken(token);
        return parts.length >= 2 ? parts[1] : 0;
    }

    // Return the patch version number.
    public static int getPatchVersion(String token) {
        int[] parts = parseVersionToken(token);
        return parts.length >= 3 ? parts[2] : 0;
    }

    // Compare two version tokens numerically.
    public static int compareVersions(String token1, String token2) {
        int[] v1 = parseVersionToken(token1);
        int[] v2 = parseVersionToken(token2);
        int len = Math.max(v1.length, v2.length);
        for (int i = 0; i < len; i++) {
            int a = i < v1.length ? v1[i] : 0;
            int b = i < v2.length ? v2[i] : 0;
            if (a != b) return a - b;
        }
        return 0;
    }

    // Check whether the token is inside the supported range.
    public static boolean isSupported(String token) {
        return isAtLeast(token, MIN_VERSION) && isBefore(token, incrementVersion(MAX_VERSION));
    }

    // Build a short debug string for logging and diagnostics.
    public static String getVersionInfo(String token) {
        return String.format("Version: %s (Family: %s, Major: %s, Supported: %b)",
                token, getVersionFamilyName(token), getMajorVersion(token), isSupported(token));
    }
}

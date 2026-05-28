package com.zaryx.okaso.bukkit.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to interpret NMS / OBC version tokens across Bukkit/Spigot naming schemes.
 * Provides coarse-grained comparisons to allow feature gating between wide ranges
 * such as 1.7.10 up to modern year-based releases.
 */
public final class VersionSupport {

    private VersionSupport() {}

    public static int[] parseVersionToken(String token) {
        if (token == null) return new int[]{0};
        // Extract contiguous groups of digits and return as ints
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

    public static boolean isBefore(String token, int... required) {
        return !isAtLeast(token, required);
    }
}

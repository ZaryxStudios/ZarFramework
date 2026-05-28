package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A command argument that parses time durations (e.g. "10s", "5m", "1h30m").
 * Returns the value in milliseconds.
 */
public final class TimeCommandArgument extends CommandArgument<Long> {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*([smhd])", Pattern.CASE_INSENSITIVE);
    private static final long SECOND = 1_000L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    private final long minMillis;
    private final long maxMillis;

    /**
     * Creates a required time argument with no range constraint.
     * @param name the argument name
     */
    public TimeCommandArgument(String name) {
        super(name);
        this.minMillis = 0;
        this.maxMillis = 0;
    }

    /**
     * Creates an optional time argument with a default value and no range constraint.
     * @param name         the argument name
     * @param optional     whether the argument is optional
     * @param defaultValue the default time string (e.g. "30s")
     */
    public TimeCommandArgument(String name, boolean optional, String defaultValue) {
        super(name, optional, defaultValue != null ? parseInternal(defaultValue) : null);
        this.minMillis = 0;
        this.maxMillis = 0;
    }

    /**
     * Creates an optional time argument with a default value and range constraints.
     * @param name         the argument name
     * @param optional     whether the argument is optional
     * @param defaultValue the default time string (e.g. "30s")
     * @param minMillis    the minimum allowed time string (e.g. "5s"), or null for no minimum
     * @param maxMillis    the maximum allowed time string (e.g. "1h"), or null for no maximum
     */
    public TimeCommandArgument(String name, boolean optional, String defaultValue, String minMillis, String maxMillis) {
        super(name, optional, defaultValue != null ? parseInternal(defaultValue) : null);
        this.minMillis = minMillis != null ? parseInternal(minMillis) : 0;
        this.maxMillis = maxMillis != null ? parseInternal(maxMillis) : 0;
    }

    /** @return the minimum allowed time in milliseconds, or 0 if unbounded */
    public long getMinMillis() { return minMillis; }
    /** @return the maximum allowed time in milliseconds, or 0 if unbounded */
    public long getMaxMillis() { return maxMillis; }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        long total = parseInternal(input.trim());
        if (total <= 0) return false;
        if (minMillis > 0 && total < minMillis) return false;
        if (maxMillis > 0 && total > maxMillis) return false;
        return true;
    }

    @Override
    public Long parse(String input) {
        return input != null ? parseInternal(input.trim()) : null;
    }

    /**
     * Parse a time string like "1h30m" into milliseconds.
     * @return milliseconds, or -1 if parsing fails
     */
    public static long parseInternal(String input) {
        if (input == null) return -1;
        Matcher m = TIME_PATTERN.matcher(input.toLowerCase());
        long total = 0;
        boolean matched = false;
        while (m.find()) {
            matched = true;
            long amount = Long.parseLong(m.group(1));
            char unit = m.group(2).charAt(0);
            switch (unit) {
                case 's': total += amount * SECOND; break;
                case 'm': total += amount * MINUTE; break;
                case 'h': total += amount * HOUR; break;
                case 'd': total += amount * DAY; break;
                default: return -1;
            }
        }
        return matched ? total : -1;
    }

    /**
     * Format milliseconds into a human-readable time string (e.g. "1h30m").
     */
    public static String formatMillis(long millis) {
        if (millis <= 0) return "0s";
        StringBuilder sb = new StringBuilder();
        long d = millis / DAY; millis %= DAY;
        long h = millis / HOUR; millis %= HOUR;
        long min = millis / MINUTE; millis %= MINUTE;
        long s = millis / SECOND;
        if (d > 0) sb.append(d).append('d');
        if (h > 0) sb.append(h).append('h');
        if (min > 0) sb.append(min).append('m');
        if (s > 0) sb.append(s).append('s');
        return sb.length() > 0 ? sb.toString() : "0s";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.asList("10s", "5m", "1h", "1h30m");
    }
}

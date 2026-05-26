package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeCommandArgument extends CommandArgument<Long> {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);

    private final long minMillis;
    private final long maxMillis;

    public TimeCommandArgument(String name, String minMillis, String maxMillis) {
        super(name);
        this.minMillis = minMillis != null ? parseInternal(minMillis) : 0;
        this.maxMillis = maxMillis != null ? parseInternal(maxMillis) : 0;

        if (this.minMillis < 0 || this.maxMillis < 0) {
            throw new IllegalArgumentException("Tiempo mínimo o máximo inválido");
        }
    }

    public TimeCommandArgument(String name, boolean optional, String defaultValue,
                               String minMillis, String maxMillis) {
        super(name, optional, parseInternal(defaultValue));
        this.minMillis = minMillis != null ? parseInternal(minMillis) : 0;
        this.maxMillis = maxMillis != null ? parseInternal(maxMillis) : 0;
    }

    @Override
    public boolean validate(String input) {
        long totalMillis = parseInternal(input);

        if (totalMillis <= 0) {
            return false;
        }

        if (this.minMillis > 0 && totalMillis < this.minMillis) {
            return false;
        }

        return this.maxMillis <= 0 || totalMillis <= this.maxMillis;
    }

    @Override
    public Long parse(String input) {
        return parseInternal(input);
    }

    private static long parseInternal(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        long totalMillis = 0;
        int matches = 0;

        while (matcher.find()) {
            matches++;

            long amount = Long.parseLong(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 's':
                    totalMillis += amount * 1_000L;
                    break;
                case 'm':
                    totalMillis += amount * 60_000L;
                    break;
                case 'h':
                    totalMillis += amount * 3_600_000L;
                    break;
                case 'd':
                    totalMillis += amount * 86_400_000L;
                    break;
                default:
                    return -1;
            }
        }

        if (matches == 0) {
            return -1;
        }

        return totalMillis;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.asList("10s", "5m", "1h", "1h30m");
    }
}
package com.zaryx.okaso.bukkit.command.argument;

import com.zaryx.okaso.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class IntegerCommandArgument extends CommandArgument<Integer> {

    private final int min;
    private final int max;
    private static final int UNBOUNDED = Integer.MIN_VALUE;

    public IntegerCommandArgument(String name, int min, int max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public IntegerCommandArgument(String name, boolean optional, Integer defaultValue, int min, int max) {
        super(name, optional, defaultValue);
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }

    public int getMax() { return max; }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        try {
            int value = Integer.parseInt(input.trim());
            if (value < min) return false;
            return max == UNBOUNDED || value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public Integer parse(String input) {
        return Integer.parseInt(input.trim());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        if (min > 1000) return Collections.emptyList();
        return Arrays.asList(
                String.valueOf(Math.max(0, min)),
                String.valueOf(Math.max(0, min * 10)),
                String.valueOf(Math.max(0, min * 100))
        );
    }
}

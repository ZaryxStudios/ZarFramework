package com.zaryx.okaso.bukkit.command.argument;

import com.zaryx.okaso.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DoubleCommandArgument extends CommandArgument<Double> {

    private final double min;
    private final double max;
    private static final double UNBOUNDED = Double.NEGATIVE_INFINITY;

    public DoubleCommandArgument(String name, double min, double max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public DoubleCommandArgument(String name, boolean optional, Double defaultValue, double min, double max) {
        super(name, optional, defaultValue);
        this.min = min;
        this.max = max;
    }

    public double getMin() { return min; }

    public double getMax() { return max; }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        try {
            double value = Double.parseDouble(input.trim());
            if (value < min) return false;
            return max == UNBOUNDED || value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public Double parse(String input) {
        return Double.parseDouble(input.trim());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        if (min > 1000.0) return Collections.emptyList();
        return Arrays.asList(
                String.valueOf(Math.max(0.0, min)),
                String.valueOf(Math.max(0.0, min * 10)),
                String.valueOf(Math.max(0.0, min * 100))
        );
    }
}

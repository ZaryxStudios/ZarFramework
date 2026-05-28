package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A command argument that parses a double within an optional range [min, max].
 */
public final class DoubleCommandArgument extends CommandArgument<Double> {

    private final double min;
    private final double max;
    private static final double UNBOUNDED = Double.NEGATIVE_INFINITY;

    /**
     * Creates a required double argument with a range constraint.
     * @param name the argument name
     * @param min  the minimum value (inclusive)
     * @param max  the maximum value (inclusive), or {@link #UNBOUNDED} for no upper bound
     */
    public DoubleCommandArgument(String name, double min, double max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    /**
     * Creates an optional double argument with a range constraint.
     * @param name         the argument name
     * @param optional     whether the argument is optional
     * @param defaultValue the default value if omitted
     * @param min          the minimum value (inclusive)
     * @param max          the maximum value (inclusive), or {@link #UNBOUNDED} for no upper bound
     */
    public DoubleCommandArgument(String name, boolean optional, Double defaultValue, double min, double max) {
        super(name, optional, defaultValue);
        this.min = min;
        this.max = max;
    }

    /** @return the minimum value (inclusive) */
    public double getMin() { return min; }
    /** @return the maximum value (inclusive), or {@link #UNBOUNDED} if unbounded */
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

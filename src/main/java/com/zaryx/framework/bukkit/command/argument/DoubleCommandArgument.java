package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class DoubleCommandArgument extends CommandArgument<Double> {

    private final double min;
    private final double max;

    public DoubleCommandArgument(String name, double min, double max) {
        super(name);

        this.min = min;
        this.max = max;
    }

    public DoubleCommandArgument(String name, boolean optional, Double defaultValue,
                                 int min, int max) {
        super(name, optional, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(String input) {
        try {
            double value = Double.parseDouble(input);
            if (value < this.min) return false;
            return !(this.max >= 0) || !(value > this.max);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Double parse(String input) {
        return Double.parseDouble(input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.asList(
                String.valueOf(min),
                String.valueOf(min * 10),
                String.valueOf(min * 100)
        );
    }
}

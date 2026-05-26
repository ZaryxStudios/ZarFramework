package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class IntegerCommandArgument extends CommandArgument<Integer> {

    private final int min;
    private final int max;

    public IntegerCommandArgument(String name, int min, int max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public IntegerCommandArgument(String name, boolean optional, int defaultValue,
                                  int min, int max) {
        super(name, optional, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean validate(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value < this.min) return false;
            return this.max < 0 || value <= this.max;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Integer parse(String input) {
        return Integer.parseInt(input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.asList(
                String.valueOf(this.min),
                String.valueOf(this.min * 10),
                String.valueOf(this.min * 100)
        );
    }
}

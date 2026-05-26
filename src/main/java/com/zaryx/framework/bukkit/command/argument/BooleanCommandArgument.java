package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class BooleanCommandArgument extends CommandArgument<Boolean> {

    public BooleanCommandArgument(String name) {
        super(name);
    }

    public BooleanCommandArgument(String name, boolean optional, Boolean defaultValue) {
        super(name, optional, defaultValue);
    }

    @Override
    public boolean validate(String input) {
        return input.equalsIgnoreCase("true")
                || input.equalsIgnoreCase("false");
    }

    @Override
    public Boolean parse(String input) {
        return Boolean.parseBoolean(input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.asList("true", "false");
    }
}

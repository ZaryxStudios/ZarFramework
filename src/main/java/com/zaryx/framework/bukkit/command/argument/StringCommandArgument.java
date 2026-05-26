package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;

public class StringCommandArgument extends CommandArgument<String> {

    public StringCommandArgument(String name) {
        super(name);
    }

    public StringCommandArgument(String name, boolean optional, String defaultValue) {
        super(name, optional, defaultValue);
    }

    @Override
    public boolean validate(String input) {
        return true;
    }

    @Override
    public String parse(String input) {
        return input;
    }
}

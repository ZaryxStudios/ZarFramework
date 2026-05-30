package com.zaryx.okaso.bukkit.command.argument;

import com.zaryx.okaso.bukkit.command.extra.CommandArgument;

public final class StringCommandArgument extends CommandArgument<String> {

    public StringCommandArgument(String name) {
        super(name);
    }

    public StringCommandArgument(String name, boolean optional, String defaultValue) {
        super(name, optional, defaultValue);
    }

    @Override
    public boolean validate(String input) {
        return input != null;
    }

    @Override
    public String parse(String input) {
        return input;
    }
}

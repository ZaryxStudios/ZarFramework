package com.zaryx.okaso.bukkit.command.argument;

import com.zaryx.okaso.bukkit.command.extra.CommandArgument;

/**
 * A command argument that accepts any non-null string.
 */
public final class StringCommandArgument extends CommandArgument<String> {

    /**
     * Creates a required string argument.
     * @param name the argument name
     */
    public StringCommandArgument(String name) {
        super(name);
    }

    /**
     * Creates an optional string argument with a default value.
     * @param name         the argument name
     * @param optional     whether the argument is optional
     * @param defaultValue the default value if omitted
     */
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

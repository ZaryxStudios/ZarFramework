package com.zaryx.okaso.bukkit.command.extra;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public abstract class CommandArgument<T> {

    private final String name;
    private final boolean optional;
    private final T defaultValue;
    private final int priority;

    protected CommandArgument(String name) {
        this(name, false, null, 0);
    }

    protected CommandArgument(String name, boolean optional, int priority) {
        this(name, optional, null, priority);
    }

    protected CommandArgument(String name, boolean optional, T defaultValue) {
        this(name, optional, defaultValue, 0);
    }

    protected CommandArgument(String name, boolean optional, T defaultValue, int priority) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name must not be null or empty");
        }
        this.name = name.trim().toLowerCase();
        this.optional = optional;
        this.defaultValue = defaultValue;
        this.priority = priority;
    }

    public String getName() { return this.name; }

    public boolean isOptional() { return this.optional; }

    public T getDefaultValue() { return this.defaultValue; }

    public int getPriority() { return this.priority; }

    public abstract boolean validate(String input);

    public abstract T parse(String input);

    public List<String> tabComplete(CommandSender sender, String input) {
        return Collections.emptyList();
    }

    public boolean shouldParse(CommandContext commandContext) {
        return true;
    }

    public boolean isRequired(CommandContext commandContext) {
        return !this.optional;
    }
}

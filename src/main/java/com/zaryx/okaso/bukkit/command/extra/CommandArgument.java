package com.zaryx.okaso.bukkit.command.extra;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for command arguments with validation, parsing, and tab completion.
 *
 * <p>Arguments are sorted by priority (lower = earlier) and then by required/optional status
 * (required arguments come before optional ones).</p>
 *
 * @param <T> the type this argument parses into
 */
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

    /** The argument name (lowercase, trimmed). */
    public String getName() { return this.name; }

    /** Whether this argument is optional. */
    public boolean isOptional() { return this.optional; }

    /** Default value used when the argument is missing and optional. */
    public T getDefaultValue() { return this.defaultValue; }

    /** Priority for argument ordering (lower = earlier in the argument list). */
    public int getPriority() { return this.priority; }

    /** Validate the raw input string before parsing. */
    public abstract boolean validate(String input);

    /** Parse the validated input string into the target type. */
    public abstract T parse(String input);

    /** Provide tab-completion suggestions for this argument. */
    public List<String> tabComplete(CommandSender sender, String input) {
        return Collections.emptyList();
    }

    /**
     * Whether this argument should be parsed in the given context.
     * Override to implement conditional parsing (e.g. based on sender permissions).
     */
    public boolean shouldParse(CommandContext commandContext) {
        return true;
    }

    /**
     * Whether this argument is required in the given context.
     * By default, mirrors {@link #isOptional()}.
     */
    public boolean isRequired(CommandContext commandContext) {
        return !this.optional;
    }
}

package com.zaryx.okaso.bukkit.command.argument;

import com.zaryx.okaso.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BooleanCommandArgument extends CommandArgument<Boolean> {

    public BooleanCommandArgument(String name) {
        super(name);
    }

    public BooleanCommandArgument(String name, boolean optional, Boolean defaultValue) {
        super(name, optional, defaultValue);
    }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        String lower = input.trim().toLowerCase();
        return "true".equals(lower) || "false".equals(lower)
            || "yes".equals(lower) || "no".equals(lower);
    }

    @Override
    public Boolean parse(String input) {
        String lower = input.trim().toLowerCase();
        if ("yes".equals(lower)) return true;
        if ("no".equals(lower)) return false;
        return Boolean.parseBoolean(lower);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        String prefix = input != null ? input.trim().toLowerCase() : "";
        if (prefix.isEmpty() || "true".startsWith(prefix) || "t".startsWith(prefix)) {
            return Collections.singletonList("true");
        }
        if ("false".startsWith(prefix) || "f".startsWith(prefix)) {
            return Collections.singletonList("false");
        }
        return Arrays.asList("true", "false");
    }
}

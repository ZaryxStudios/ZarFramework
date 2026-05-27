package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A command argument that parses an enum constant by name (case-insensitive).
 *
 * @param <T> the enum type
 */
public final class EnumCommandArgument<T extends Enum<T>> extends CommandArgument<T> {

    private final Class<T> enumClass;

    public EnumCommandArgument(String name, Class<T> enumClass) {
        super(name);
        this.enumClass = enumClass;
    }

    public Class<T> getEnumClass() { return enumClass; }

    @Override
    public boolean validate(String input) {
        if (input == null) return false;
        try {
            Enum.valueOf(enumClass, input.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public T parse(String input) {
        return Enum.valueOf(enumClass, input.trim().toUpperCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        String prefix = input != null ? input.trim().toUpperCase() : "";
        List<String> results = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            String name = constant.name();
            if (name.startsWith(prefix)) results.add(name);
        }
        return results;
    }
}

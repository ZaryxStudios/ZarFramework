package com.zaryx.framework.bukkit.command.argument;

import com.zaryx.framework.bukkit.command.extra.CommandArgument;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EnumCommandArgument<T extends Enum<T>> extends CommandArgument<T> {

    private final Class<T> enumClass;

    public EnumCommandArgument(String name, Class<T> enumClass) {
        super(name);
        this.enumClass = enumClass;
    }

    @Override
    public boolean validate(String input) {
        try {
            Enum.valueOf(this.enumClass, input.toUpperCase());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public T parse(String input) {
        return Enum.valueOf(this.enumClass, input.toUpperCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
        return Arrays.stream(this.enumClass.getEnumConstants())
                .map(Enum::name)
                .filter(s -> s.toLowerCase().startsWith(input.toUpperCase()))
                .collect(Collectors.toList());
    }
}

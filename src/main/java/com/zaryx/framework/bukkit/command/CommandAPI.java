package com.zaryx.framework.bukkit.command;

import com.zaryx.framework.bukkit.command.core.CommandManager;
import org.bukkit.command.Command;

public final class CommandAPI {

    private CommandAPI() {}

    public static void register(Command command) {
        CommandManager.getInstance().register(command);
    }

    public static void unregister(Command command) {
        CommandManager.getInstance().unregister(command.getName());
    }

    public static void enable(Command command) {
        CommandManager.getInstance().enable(command);
    }

    public static void disable(Command command) {
        CommandManager.getInstance().disable(command);
    }
}

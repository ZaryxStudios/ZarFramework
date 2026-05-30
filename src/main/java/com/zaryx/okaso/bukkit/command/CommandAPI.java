package com.zaryx.okaso.bukkit.command;

import com.zaryx.okaso.bukkit.command.core.CommandManager;
import org.bukkit.command.Command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CommandAPI {

    private CommandAPI() {}

    public static boolean register(Command command) {
        if (command == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.register(command);
    }

    public static boolean unregister(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.unregister(name);
    }

    public static void enable(String name) {
        if (name == null) return;
        CommandManager mgr = CommandManager.getInstance();
        if (mgr != null) mgr.enable(name);
    }

    public static void disable(String name) {
        if (name == null) return;
        CommandManager mgr = CommandManager.getInstance();
        if (mgr != null) mgr.disable(name);
    }

    public static boolean isRegistered(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.isRegistered(name);
    }

    public static boolean isEnabled(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.isCommandEnabled(name);
    }

    public static Command get(String name) {
        if (name == null) return null;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getCommand(name) : null;
    }

    public static Collection<Command> all() {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getAllCommands() : Collections.<Command>emptyList();
    }

    public static Set<String> names() {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getAllCommandNames() : Collections.<String>emptySet();
    }

    public static List<Command> findByPrefix(String prefix) {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.findCommandsByPrefix(prefix) : Collections.<Command>emptyList();
    }

    public static String info(String name) {
        if (name == null) return null;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getCommandInfo(name) : null;
    }
}

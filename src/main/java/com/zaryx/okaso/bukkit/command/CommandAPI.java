package com.zaryx.okaso.bukkit.command;

import com.zaryx.okaso.bukkit.command.core.CommandManager;
import org.bukkit.command.Command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Public facade for the command subsystem.
 * Provides registration, lookup, enable/disable, and query operations.
 */
public final class CommandAPI {

    private CommandAPI() {}

    /**
     * Register a command with the okaso.
     * @return true if registration succeeded
     */
    public static boolean register(Command command) {
        if (command == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.register(command);
    }

    /**
     * Unregister a command by name or alias.
     * @return true if the command was found and unregistered
     */
    public static boolean unregister(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.unregister(name);
    }

    /**
     * Enable a previously disabled command.
     */
    public static void enable(String name) {
        if (name == null) return;
        CommandManager mgr = CommandManager.getInstance();
        if (mgr != null) mgr.enable(name);
    }

    /**
     * Disable a command so it cannot be executed.
     */
    public static void disable(String name) {
        if (name == null) return;
        CommandManager mgr = CommandManager.getInstance();
        if (mgr != null) mgr.disable(name);
    }

    /**
     * Check if a command is registered.
     */
    public static boolean isRegistered(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.isRegistered(name);
    }

    /**
     * Check if a registered command is currently enabled.
     */
    public static boolean isEnabled(String name) {
        if (name == null) return false;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null && mgr.isCommandEnabled(name);
    }

    /**
     * Get a registered command by name or alias.
     */
    public static Command get(String name) {
        if (name == null) return null;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getCommand(name) : null;
    }

    /**
     * Get all registered commands.
     */
    public static Collection<Command> all() {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getAllCommands() : Collections.<Command>emptyList();
    }

    /**
     * Get all registered command names.
     */
    public static Set<String> names() {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getAllCommandNames() : Collections.<String>emptySet();
    }

    /**
     * Find commands whose name or alias starts with the given prefix.
     */
    public static List<Command> findByPrefix(String prefix) {
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.findCommandsByPrefix(prefix) : Collections.<Command>emptyList();
    }

    /**
     * Get a human-readable info string for a command.
     */
    public static String info(String name) {
        if (name == null) return null;
        CommandManager mgr = CommandManager.getInstance();
        return mgr != null ? mgr.getCommandInfo(name) : null;
    }
}

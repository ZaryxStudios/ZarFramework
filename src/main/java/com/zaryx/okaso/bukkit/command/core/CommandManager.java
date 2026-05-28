package com.zaryx.okaso.bukkit.command.core;

import com.zaryx.okaso.core.ManagedComponent;
import com.zaryx.okaso.core.event.EventBus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized okaso command manager.
 * Provides registration, unregistration, enable/disable, and advanced command management
 * with alias indexing and event bus integration.
 */
public final class CommandManager implements ManagedComponent {

    private static volatile CommandManager instance;

    private final JavaPlugin plugin;
    private final CommandMap commandMap;
    private final Logger logger;
    private final String fallbackPrefix;
    private final Map<String, RegisteredCommand> registry;
    private final Map<String, String> aliasIndex;
    private final EventBus eventBus;
    private volatile boolean initialized;

    public CommandManager(JavaPlugin plugin, String fallbackPrefix) {
        instance = this;
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.commandMap = initCommandMap();
        this.fallbackPrefix = fallbackPrefix != null ? fallbackPrefix : "Okaso";
        this.registry = new ConcurrentHashMap<>();
        this.aliasIndex = new ConcurrentHashMap<>();
        this.eventBus = new EventBus(logger);
        this.initialized = false;
    }

    /**
     * Returns the singleton instance of the CommandManager.
     * @return the current instance, or null if not yet constructed
     */
    public static CommandManager getInstance() {
        return instance;
    }

    /**
     * Returns the logger used by this manager.
     * @return the plugin logger
     */
    public Logger getLogger() {
        return logger;
    }

    // ---- ManagedComponent ----

    @Override
    public String getName() { return "command-manager"; }

    @Override
    public void initialize() {
        this.initialized = true;
        logger.info("CommandManager initialized");
    }

    @Override
    public void disable() {
        cleanup();
    }

    @Override
    public boolean isEnabled() { return initialized; }

    @Override
    public boolean isInitialized() { return initialized; }

    @Override
    public void cleanup() {
        List<String> names = new ArrayList<>(registry.keySet());
        for (String name : names) {
            unregister(name);
        }
        eventBus.clear();
        logger.info("CommandManager cleaned up: " + names.size() + " commands removed");
    }

    @Override
    public String getStats() {
        long enabled = getEnabledCommandCount();
        return "CommandManager | Commands: " + registry.size()
                + " | Enabled: " + enabled
                + " | Aliases: " + aliasIndex.size()
                + " | Status: " + (isEnabled() ? "ACTIVE" : "INACTIVE");
    }

    // ---- Registration ----

    /**
     * Register a command with the okaso and Bukkit's command map.
     * @return true if registration succeeded, false if the name/alias conflicts
     */
    public boolean register(Command command) {
        return register(command, null);
    }

    /**
     * Register a command with an optional description.
     * @return true if registration succeeded
     */
    public boolean register(Command command, String description) {
        if (command == null) {
            logger.warning("Cannot register null command");
            return false;
        }

        String raw = command.getName();
        if (raw == null || raw.trim().isEmpty()) {
            logger.warning("Command has no name");
            return false;
        }

        String name = raw.trim().toLowerCase(Locale.ROOT);
        Set<String> aliases = normalizeAliases(command, name);

        // Collision check
        if (registry.containsKey(name) || aliasIndex.containsKey(name)) {
            logger.warning("Command '" + name + "' is already registered");
            return false;
        }
        for (String a : aliases) {
            if (registry.containsKey(a) || aliasIndex.containsKey(a)) {
                logger.warning("Alias '" + a + "' is already registered");
                return false;
            }
        }

        String desc = description != null ? description : command.getDescription();
        RegisteredCommand entry = new RegisteredCommand(name, command, desc, aliases);

        try {
            commandMap.register(fallbackPrefix, command);
            registry.put(name, entry);
            for (String a : aliases) {
                aliasIndex.put(a, name);
            }
            logger.fine("Command registered: " + name);
            return true;
        } catch (Exception e) {
            registry.remove(name);
            for (String a : aliases) {
                aliasIndex.remove(a);
            }
            logger.log(Level.SEVERE, "Error registering command: " + name, e);
            return false;
        }
    }

    /**
     * Unregister a command by name or alias, removing it from Bukkit's command map.
     */
    public boolean unregister(String name) {
        if (name == null) return false;

        String resolved = resolveCommandName(name.trim());
        if (resolved == null) return false;

        RegisteredCommand entry = registry.remove(resolved);
        removeAliasesFor(resolved);

        if (entry == null) return false;

        try {
            entry.command.unregister(commandMap);
            logger.fine("Command unregistered: " + resolved);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error unregistering: " + resolved, e);
            return false;
        }
    }

    // ---- Enable / Disable ----

    /** Enables a command by name or alias. */
    public void enable(String name) { setEnabled(name, true); }
    /** Disables a command by name or alias. */
    public void disable(String name) { setEnabled(name, false); }
    /** Enables a command instance. */
    public void enable(Command command) { setEnabledFlag(command, true); }
    /** Disables a command instance. */
    public void disable(Command command) { setEnabledFlag(command, false); }

    // ---- Queries ----

    /**
     * Gets a registered command by name or alias.
     * @param name the command name or alias
     * @return the command, or null if not found
     */
    public Command getCommand(String name) {
        String resolved = resolveCommandName(name);
        RegisteredCommand entry = resolved != null ? registry.get(resolved) : null;
        return entry != null ? entry.command : null;
    }

    /**
     * Checks whether a command is registered.
     * @param name the command name or alias
     * @return true if registered
     */
    public boolean isRegistered(String name) {
        return resolveCommandName(name) != null;
    }

    /**
     * Checks whether a registered command is currently enabled.
     * @param name the command name or alias
     * @return true if the command exists and is enabled
     */
    public boolean isCommandEnabled(String name) {
        String resolved = resolveCommandName(name);
        RegisteredCommand entry = resolved != null ? registry.get(resolved) : null;
        return entry != null && entry.enabled;
    }

    /**
     * Gets the description of a registered command.
     * @param name the command name or alias
     * @return the description, or null if not found
     */
    public String getDescription(String name) {
        String resolved = resolveCommandName(name);
        RegisteredCommand entry = resolved != null ? registry.get(resolved) : null;
        return entry != null ? entry.description : null;
    }

    /**
     * Gets a human-readable info string for a command.
     * @param name the command name or alias
     * @return formatted info, or null if not found
     */
    public String getCommandInfo(String name) {
        String resolved = resolveCommandName(name);
        if (resolved == null) return null;
        RegisteredCommand entry = registry.get(resolved);
        if (entry == null) return null;
        return "Command: " + entry.command.getName()
                + " | Enabled: " + entry.enabled
                + " | Description: " + (entry.description != null ? entry.description : "N/A")
                + " | Usage: " + entry.command.getUsage();
    }

    /**
     * Returns an unmodifiable collection of all registered commands.
     * @return snapshot of all commands
     */
    public Collection<Command> getAllCommands() {
        List<Command> snapshot = new ArrayList<>();
        for (RegisteredCommand entry : registry.values()) {
            snapshot.add(entry.command);
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Returns an unmodifiable set of all registered command names.
     * @return snapshot of all command names
     */
    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(registry.keySet()));
    }

    /** @return the total number of registered commands */
    public int getCommandCount() { return registry.size(); }

    /** @return the number of registered commands that are currently enabled */
    public long getEnabledCommandCount() {
        long count = 0;
        for (RegisteredCommand entry : registry.values()) {
            if (entry.enabled) count++;
        }
        return count;
    }

    /**
     * Finds commands whose name or alias starts with the given prefix.
     * @param prefix the prefix to match (case-insensitive)
     * @return list of matching commands (may be empty)
     */
    public List<Command> findCommandsByPrefix(String prefix) {
        LinkedHashSet<Command> result = new LinkedHashSet<>();
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>(result);

        String lower = prefix.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, RegisteredCommand> e : registry.entrySet()) {
            if (e.getKey().startsWith(lower)) {
                result.add(e.getValue().command);
            }
        }
        for (Map.Entry<String, String> e : aliasIndex.entrySet()) {
            if (e.getKey().startsWith(lower)) {
                RegisteredCommand rc = registry.get(e.getValue());
                if (rc != null) result.add(rc.command);
            }
        }
        return new ArrayList<>(result);
    }

    // ---- Events ----

    /** @return the event bus used for command-related events */
    public EventBus getEventBus() { return eventBus; }

    /**
     * Registers a listener for a specific okaso event type.
     * @param type     the event class to listen for
     * @param listener the listener to invoke when the event is published
     * @param <E>      the event type
     */
    public <E extends EventBus.FrameworkEvent> void addEventListener(Class<E> type, EventBus.EventListener<E> listener) {
        eventBus.subscribe(type, listener);
    }

    /**
     * Publishes a okaso event to all registered listeners.
     * @param event the event to publish
     */
    public void publishEvent(EventBus.FrameworkEvent event) {
        eventBus.publish(event);
    }

    // ---- Internal helpers ----

    /**
     * Normalizes and deduplicates aliases from a command.
     * Filters out null, empty, and self-referencing aliases.
     */
    private Set<String> normalizeAliases(Command command, String primaryName) {
        Set<String> result = new LinkedHashSet<>();
        List<String> raw = command.getAliases();
        if (raw == null) return result;
        for (String a : raw) {
            if (a == null) continue;
            String normalized = a.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || normalized.equals(primaryName)) continue;
            result.add(normalized);
        }
        return result;
    }

    /**
     * Resolves a command name or alias to its primary registry key.
     * @param name the command name or alias
     * @return the primary registry key, or null if not found
     */
    private String resolveCommandName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (registry.containsKey(key)) return key;
        return aliasIndex.get(key);
    }

    /** Removes all alias entries that point to the given command name. */
    private void removeAliasesFor(String commandName) {
        Iterator<Map.Entry<String, String>> it = aliasIndex.entrySet().iterator();
        while (it.hasNext()) {
            if (commandName.equals(it.next().getValue())) {
                it.remove();
            }
        }
    }

    /** Sets the enabled state of a command by name or alias. */
    private void setEnabled(String name, boolean value) {
        if (name == null) return;
        Command cmd = getCommand(name);
        if (cmd != null) setEnabledFlag(cmd, value);
    }

    /**
     * Sets the enabled state of a command instance, updating both the registry
     * entry and the underlying {@link BaseCommand} if applicable.
     */
    private void setEnabledFlag(Command command, boolean value) {
        if (command == null) return;
        String resolved = resolveCommandName(command.getName());
        if (resolved == null) return;
        RegisteredCommand entry = registry.get(resolved);
        if (entry == null) return;
        entry.enabled = value;
        if (command instanceof BaseCommand) {
            ((BaseCommand) command).setEnabled(value);
        }
    }

    /**
     * Initializes the Bukkit CommandMap via reflection.
     * @return the server's CommandMap
     * @throws RuntimeException if the CommandMap cannot be obtained
     */
    private static CommandMap initCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain CommandMap", e);
        }
    }

    // ---- RegisteredCommand ----

    /** Internal holder for a registered command and its metadata. */
    private static final class RegisteredCommand {
        final String name;
        final Command command;
        final String description;
        final Set<String> aliases;
        volatile boolean enabled;

        RegisteredCommand(String name, Command command, String description, Set<String> aliases) {
            this.name = name;
            this.command = command;
            this.description = description;
            this.aliases = Collections.unmodifiableSet(new LinkedHashSet<>(aliases));
            this.enabled = true;
        }
    }
}

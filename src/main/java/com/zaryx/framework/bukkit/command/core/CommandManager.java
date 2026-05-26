package com.zaryx.framework.bukkit.command.core;

import com.zaryx.framework.core.ManagedComponent;
import com.zaryx.framework.core.event.EventBus;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized framework command manager.
 * Provides registration, unregistration, and advanced command management.
 */
@Getter
public class CommandManager implements ManagedComponent {

    private static CommandManager instance;

    private final JavaPlugin plugin;
    private final CommandMap commandMap;
    private final Logger logger;
    private final String fallbackPrefix;

    private final Map<String, RegisteredCommand> registry;
    private final Map<String, String> aliasIndex;
    private final EventBus eventBus;

    private boolean initialized;

    public CommandManager(JavaPlugin plugin, String fallbackPrefix) {
        instance = this;

        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.commandMap = this.initCommandMap();
        this.fallbackPrefix = fallbackPrefix;
        this.registry = new java.util.concurrent.ConcurrentHashMap<>();
        this.aliasIndex = new java.util.concurrent.ConcurrentHashMap<>();
        this.eventBus = new EventBus(logger);
        this.initialized = false;
    }

    @Override
    public String getName() {
        return "command-manager";
    }

    @Override
    public void initialize() {
        this.initialized = true;
        logger.info("CommandManager initialized successfully");
    }

    @Override
    public void disable() {
        cleanup();
    }

    @Override
    public boolean isEnabled() {
        return initialized && !registry.isEmpty();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void cleanup() {
        List<String> commandNames = new ArrayList<>(registry.keySet());
        for (String name : commandNames) {
            unregister(name);
        }
        eventBus.clear();
        logger.info("CommandManager cleaned up: " + commandNames.size() + " commands removed");
    }

    @Override
    public String getStats() {
        long enabledCount = registry.values().stream().filter(RegisteredCommand::isEnabled).count();
        return "CommandManager | Commands: " + registry.size() +
               " | Enabled: " + enabledCount +
               " | Status: " + (isEnabled() ? "ACTIVE" : "INACTIVE");
    }

    /**
     * Registers a command
     */
    public boolean register(Command command) {
        return register(command, null);
    }

    /**
     * Registers a command with description
     */
    public boolean register(Command command, String description) {
        if (command == null) {
            logger.warning("Attempt to register null command");
            return false;
        }

        String rawName = command.getName();
        if (rawName == null || rawName.trim().isEmpty()) {
            logger.warning("Command has no name");
            return false;
        }

        String name = rawName.trim().toLowerCase(Locale.ROOT);

        // Collect aliases and validate collisions first
        Set<String> normalizedAliases = new LinkedHashSet<>();
        for (String alias : command.getAliases()) {
            if (alias == null) continue;
            String normalizedAlias = alias.trim();
            if (normalizedAlias.isEmpty()) continue;
            normalizedAlias = normalizedAlias.toLowerCase(Locale.ROOT);
            if (normalizedAlias.equals(name)) continue;
            normalizedAliases.add(normalizedAlias);
        }

        // Check for collisions
        if (registry.containsKey(name) || aliasIndex.containsKey(name)) {
            logger.warning("Command '" + name + "' is already registered");
            return false;
        }
        for (String alias : normalizedAliases) {
            if (registry.containsKey(alias) || aliasIndex.containsKey(alias)) {
                logger.warning("Command alias '" + alias + "' is already registered");
                return false;
            }
        }

        RegisteredCommand entry = new RegisteredCommand(
                name,
                command,
                description != null ? description : command.getDescription(),
                normalizedAliases
        );

        try {
            // Register in the server command map and internal registries
            this.commandMap.register(this.fallbackPrefix, command);
            this.registry.put(name, entry);
            for (String alias : normalizedAliases) {
                this.aliasIndex.put(alias, name);
            }
            logger.fine("Command registered: " + name);
            return true;
        } catch (Exception e) {
            // rollback partial state on error
            this.registry.remove(name);
            for (String alias : normalizedAliases) {
                this.aliasIndex.remove(alias);
            }
            logger.log(Level.SEVERE, "Error registering command: " + name, e);
            return false;
        }
    }

    /**
     * Unregisters a command
     */
    public boolean unregister(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        name = resolveCommandName(name);
        if (name == null) {
            return false;
        }

        RegisteredCommand entry = this.registry.remove(name);
        removeAliasesFor(name);

        if (entry == null) {
            return false;
        }

        try {
            entry.command.unregister(this.commandMap);
            logger.fine("Command unregistered: " + name);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error unregistering command: " + name, e);
            return false;
        }
    }

    /**
     * Disables a command without unregistering it
     */
    public void disable(Command command) {
        if (command == null) return;

        String name = resolveCommandName(command.getName());
        if (name == null) {
            return;
        }

        RegisteredCommand entry = this.registry.get(name);
        if (entry == null) {
            return;
        }

        entry.enabled = false;
        if (command instanceof BaseCommand) {
            ((BaseCommand) command).setEnabled(false);
        }
    }

    /**
     * Disables a command by name
     */
    public void disable(String name) {
        if (name == null) return;
        Command cmd = getCommand(name);
        if (cmd != null) {
            disable(cmd);
        }
    }

    /**
     * Enables a command
     */
    public void enable(Command command) {
        if (command == null) return;

        String name = resolveCommandName(command.getName());
        if (name == null) {
            return;
        }

        RegisteredCommand entry = this.registry.get(name);
        if (entry == null) {
            return;
        }

        entry.enabled = true;
        if (command instanceof BaseCommand) {
            ((BaseCommand) command).setEnabled(true);
        }
    }

    /**
     * Enables a command by name
     */
    public void enable(String name) {
        if (name == null) return;
        Command cmd = getCommand(name);
        if (cmd != null) {
            enable(cmd);
        }
    }

    /**
     * Gets a registered command
     */
    public Command getCommand(String name) {
        String primaryName = resolveCommandName(name);
        RegisteredCommand entry = primaryName != null ? registry.get(primaryName) : null;
        return entry != null ? entry.command : null;
    }

    /**
     * Checks if a command is registered
     */
    public boolean isRegistered(String name) {
        String primaryName = resolveCommandName(name);
        return primaryName != null && registry.containsKey(primaryName);
    }

    /**
     * Checks if a command is enabled
     */
    public boolean isCommandEnabled(String name) {
        String primaryName = resolveCommandName(name);
        RegisteredCommand entry = primaryName != null ? registry.get(primaryName) : null;
        return entry != null && entry.enabled;
    }

    /**
     * Gets the description of a command
     */
    public String getDescription(String name) {
        String primaryName = resolveCommandName(name);
        RegisteredCommand entry = primaryName != null ? registry.get(primaryName) : null;
        return entry != null ? entry.description : null;
    }

    /**
     * Gets all registered commands
     */
    public Collection<Command> getAllCommands() {
        List<Command> snapshot = new ArrayList<>();
        for (RegisteredCommand entry : this.registry.values()) {
            snapshot.add(entry.command);
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Gets all command names
     */
    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(registry.keySet()));
    }

    /**
     * Gets the number of registered commands
     */
    public int getCommandCount() {
        return registry.size();
    }

    /**
     * Gets the number of enabled commands
     */
    public long getEnabledCommandCount() {
        return registry.values().stream().filter(RegisteredCommand::isEnabled).count();
    }

    /**
     * Searches for commands by name prefix
     */
    public List<Command> findCommandsByPrefix(String prefix) {
        LinkedHashSet<Command> result = new LinkedHashSet<>();
        if (prefix != null) {
            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, RegisteredCommand> entry : registry.entrySet()) {
                if (entry.getKey().startsWith(lowerPrefix)) {
                    result.add(entry.getValue().command);
                }
            }

            for (Map.Entry<String, String> entry : aliasIndex.entrySet()) {
                if (entry.getKey().startsWith(lowerPrefix)) {
                    RegisteredCommand command = registry.get(entry.getValue());
                    if (command != null) {
                        result.add(command.command);
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Registers an event listener
     */
    public <E extends EventBus.FrameworkEvent> void addEventListener(
            Class<E> eventType, EventBus.EventListener<E> listener) {
        eventBus.subscribe(eventType, listener);
    }

    /**
     * Publishes an event
     */
    public void publishEvent(EventBus.FrameworkEvent event) {
        eventBus.publish(event);
    }

    /**
     * Gets the EventBus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Gets detailed information about a command
     */
    public String getCommandInfo(String name) {
        String primaryName = resolveCommandName(name);
        if (primaryName == null) {
            return null;
        }

        RegisteredCommand entry = registry.get(primaryName);
        if (entry == null) {
            return null;
        }

        return "Command: " + entry.command.getName() +
               " | Enabled: " + entry.enabled +
               " | Description: " + (entry.description != null ? entry.description : "N/A") +
               " | Usage: " + entry.command.getUsage();
    }

    public static CommandManager getInstance() {
        return instance;
    }

    private CommandMap initCommandMap() {
        try {
            Field field = Bukkit.getServer()
                    .getClass()
                    .getDeclaredField("commandMap");

            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());

        } catch (Exception e) {
            throw new RuntimeException("Could not obtain CommandMap", e);
        }
    }

    private String resolveCommandName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (registry.containsKey(normalized)) {
            return normalized;
        }

        return aliasIndex.get(normalized);
    }

    private void removeAliasesFor(String commandName) {
        Iterator<Map.Entry<String, String>> iterator = aliasIndex.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (commandName.equals(entry.getValue())) {
                iterator.remove();
            }
        }
    }

    private static final class RegisteredCommand {
        private final String name;
        private final Command command;
        private final String description;
        private final Set<String> aliases;
        private boolean enabled;

        private RegisteredCommand(String name, Command command, String description, Set<String> aliases) {
            this.name = name;
            this.command = command;
            this.description = description;
            this.aliases = new LinkedHashSet<>(aliases);
            this.enabled = true;
        }

        private boolean isEnabled() {
            return this.enabled;
        }
    }

}

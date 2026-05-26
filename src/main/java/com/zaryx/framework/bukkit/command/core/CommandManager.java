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
    
    private final Map<String, Command> commands;
    private final Map<String, String> aliases;
    private final Map<String, Boolean> enabled;
    private final Map<String, String> descriptions;
    private final EventBus eventBus;
    
    private boolean initialized;

    public CommandManager(JavaPlugin plugin, String fallbackPrefix) {
        instance = this;

        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.commandMap = this.initCommandMap();
        this.fallbackPrefix = fallbackPrefix;
        this.commands = new ConcurrentHashMap<>();
        this.aliases = new ConcurrentHashMap<>();
        this.enabled = new ConcurrentHashMap<>();
        this.descriptions = new ConcurrentHashMap<>();
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
        return !commands.isEmpty() && initialized;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void cleanup() {
        List<String> commandNames = new ArrayList<>(commands.keySet());
        for (String name : commandNames) {
            unregister(name);
        }
        eventBus.clear();
        logger.info("CommandManager cleaned up: " + commandNames.size() + " commands removed");
    }

    @Override
    public String getStats() {
        long enabledCount = enabled.values().stream().filter(b -> b).count();
        return "CommandManager | Commands: " + commands.size() + 
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

        String name = command.getName().toLowerCase(Locale.ROOT);

        if (commands.containsKey(name) || aliases.containsKey(name)) {
            logger.warning("Command '" + name + "' is already registered");
            return false;
        }

        Set<String> normalizedAliases = new LinkedHashSet<>();
        for (String alias : command.getAliases()) {
            if (alias == null || alias.trim().isEmpty()) {
                continue;
            }

            String normalizedAlias = alias.trim().toLowerCase(Locale.ROOT);
            if (normalizedAlias.equals(name)) {
                continue;
            }

            if (commands.containsKey(normalizedAlias) || aliases.containsKey(normalizedAlias)) {
                logger.warning("Command alias '" + normalizedAlias + "' is already registered");
                return false;
            }

            normalizedAliases.add(normalizedAlias);
        }

        try {
            this.commandMap.register(this.fallbackPrefix, command);
            this.commands.put(name, command);
            this.enabled.put(name, true);
            this.descriptions.put(name, description != null ? description : command.getDescription());
            for (String alias : normalizedAliases) {
                this.aliases.put(alias, name);
            }
            logger.fine("Command registered: " + name);
            return true;
        } catch (Exception e) {
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

        Command command = this.commands.remove(name);
        this.enabled.remove(name);
        this.descriptions.remove(name);
        removeAliasesFor(name);

        if (command == null) {
            return false;
        }

        try {
            command.unregister(this.commandMap);
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

        this.enabled.put(name, false);
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

        this.enabled.put(name, true);
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
        return primaryName != null ? commands.get(primaryName) : null;
    }

    /**
     * Checks if a command is registered
     */
    public boolean isRegistered(String name) {
        return resolveCommandName(name) != null;
    }

    /**
     * Checks if a command is enabled
     */
    public boolean isCommandEnabled(String name) {
        String primaryName = resolveCommandName(name);
        return primaryName != null && enabled.getOrDefault(primaryName, false);
    }

    /**
     * Gets the description of a command
     */
    public String getDescription(String name) {
        String primaryName = resolveCommandName(name);
        return primaryName != null ? descriptions.get(primaryName) : null;
    }

    /**
     * Gets all registered commands
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    /**
     * Gets all command names
     */
    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }

    /**
     * Gets the number of registered commands
     */
    public int getCommandCount() {
        return commands.size();
    }

    /**
     * Gets the number of enabled commands
     */
    public long getEnabledCommandCount() {
        return enabled.values().stream().filter(b -> b).count();
    }

    /**
     * Searches for commands by name prefix
     */
    public List<Command> findCommandsByPrefix(String prefix) {
        LinkedHashSet<Command> result = new LinkedHashSet<>();
        if (prefix != null) {
            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, Command> entry : commands.entrySet()) {
                if (entry.getKey().startsWith(lowerPrefix)) {
                    result.add(entry.getValue());
                }
            }

            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                if (entry.getKey().startsWith(lowerPrefix)) {
                    Command command = commands.get(entry.getValue());
                    if (command != null) {
                        result.add(command);
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
        
        Command cmd = commands.get(primaryName);
        boolean isEnabled = enabled.getOrDefault(primaryName, false);
        String desc = descriptions.get(primaryName);
        
        return "Command: " + cmd.getName() + 
               " | Enabled: " + isEnabled +
               " | Description: " + (desc != null ? desc : "N/A") +
               " | Usage: " + cmd.getUsage();
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
        if (commands.containsKey(normalized)) {
            return normalized;
        }

        return aliases.get(normalized);
    }

    private void removeAliasesFor(String commandName) {
        Iterator<Map.Entry<String, String>> iterator = aliases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (commandName.equals(entry.getValue())) {
                iterator.remove();
            }
        }
    }

}

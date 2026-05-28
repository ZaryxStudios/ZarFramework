package com.zaryx.okaso.api;

import com.zaryx.okaso.core.*;
import com.zaryx.okaso.core.cache.CacheManager;
import com.zaryx.okaso.core.event.EventBus;
import com.zaryx.okaso.core.registry.Registry;
import com.zaryx.okaso.core.security.HashingService;
import com.zaryx.okaso.core.serialization.OkasoSerializer;
import com.zaryx.okaso.core.text.MessageService;
import com.zaryx.okaso.core.web.WebRequestService;
import com.zaryx.okaso.bukkit.nms.NmsService;
import com.zaryx.okaso.bukkit.simulation.PacketSimulationService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Main API for Okaso.
 * Provides unified access to okaso components.
 *
 * Example usage as a plugin dependency:
 * <pre>
 * OkasoAPI okaso = OkasoAPI.create(logger, plugin);
 * okaso.initialize();
 *
 * CacheManager cache = okaso.getCacheManager();
 * EventBus eventBus = okaso.getEventBus();
 * </pre>
 */
public interface OkasoAPI {

    /**
     * Okaso initialization state
     */
    enum State {
        CREATED, INITIALIZED, RUNNING, DISABLED, ERROR
    }

    // ============ Lifecycle ============

    /**
     * Initializes all okaso components
     * @return true if initialization was successful
     */
    boolean initialize();

    /**
     * Starts the okaso (after `initialize`)
     * @return true if startup succeeded
     */
    boolean start();

    /**
     * Stops all okaso components
     */
    void shutdown();

    /**
     * Returns the current okaso state
     */
    State getState();

    /**
     * Checks if the okaso is running
     */
    boolean isRunning();

    /**
     * Returns the okaso logger
     */
    Logger getLogger();

    // ============ Component Access ============

    /**
     * Returns the module manager
     */
    ModuleManager getModuleManager();

    /**
     * Checks whether a module exists
     */
    boolean hasModule(String name);

    /**
     * Returns the registered module names
     */
    java.util.Set<String> getModuleNames();

    /**
     * Returns the event bus
     */
    EventBus getEventBus();

    /**
     * Returns the serializer service for JSON/object conversion.
     */
    OkasoSerializer getSerializer();

    /**
     * Returns the hashing service for digests and HMAC.
     */
    HashingService getHasher();

    /**
     * Returns the HTTP/web request service.
     */
    WebRequestService getWebRequests();

    /**
     * Returns the high-level messaging service.
     */
    MessageService getMessages();

    /**
     * Returns the NMS abstraction for version-aware packet access.
     */
    NmsService getNms();

    /**
     * Returns the client-side packet simulation service.
     */
    PacketSimulationService getPacketSimulation();

    /**
     * Returns the cache manager
     */
    CacheManager getCacheManager();

    /**
     * Returns a module by name and type
     */
    <T extends Module> T getModule(String name, Class<T> type);

    /**
     * Registers a module
     */
    boolean registerModule(Module module);

    /**
     * Registers a custom component
     */
    <T> void registerComponent(String name, T component, String metadata);

    /**
     * Registers a custom component without metadata.
     */
    default <T> void registerComponent(String name, T component) {
        registerComponent(name, component, null);
    }

    /**
     * Checks whether a component exists
     */
    boolean hasComponent(String name);

    /**
     * Returns the registered component names
     */
    java.util.Set<String> getComponentNames();

    /**
     * Retrieves a registered component
     */
    <T> T getComponent(String name, Class<T> type);

    /**
     * Returns a generic registry by name
     */
    <T> Registry<T> getRegistry(String name);

    /**
     * Returns the registered registry names
     */
    java.util.Set<String> getRegistryNames();

    /**
     * Creates a new generic registry
     */
    <T> Registry<T> createRegistry(String name);

    /**
     * Registers all modules from a package
     */
    boolean registerModulesFromPackage(String packageName);

    /**
     * Registers all modules from a package with a filter
     */
    boolean registerModulesFromPackage(String packageName, java.util.function.Predicate<Module> filter);

    // ============ Utilities ============

    /**
     * Returns the executor service for async tasks
     */
    ExecutorService getExecutorService();

    /**
     * Runs a task asynchronously with the okaso executor.
     */
    default CompletableFuture<Void> runAsyncTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return CompletableFuture.runAsync(task, getExecutorService());
    }

    /**
     * Runs a supplier asynchronously with the okaso executor.
     */
    default <T> CompletableFuture<T> supplyAsyncTask(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("supplier must not be null");
        }
        return CompletableFuture.supplyAsync(supplier, getExecutorService());
    }

    /**
     * Resolves a component or throws if not present.
     */
    default <T> T requireComponent(String name, Class<T> type) {
        T component = getComponent(name, type);
        if (component == null) {
            throw new IllegalStateException("Required component not found: " + name);
        }
        return component;
    }

    /**
     * Returns okaso statistics
     */
    OkasoStats getStats();

    /**
     * Returns debug information for the okaso
     */
    OkasoDebugInfo getDebugInfo();

    /**
     * Returns the current okaso configuration
     */
    OkasoConfig getConfig();

    /**
     * Reloads in-memory okaso configuration state
     */
    boolean reloadConfig();

    /**
     * Restarts the okaso using the current configuration
     */
    boolean restart();

    /**
     * Resets the okaso fully and clears registered modules, components, and registries.
     */
    boolean reset();

    /**
     * Checks whether a named registry exists
     */
    boolean hasRegistry(String name);

    /**
     * Retrieves a registered component by type
     */
    <T> T getComponent(Class<T> type);

    /**
     * Registers a component by its runtime type.
     */
    default <T> void registerComponent(T component) {
        registerComponent(null, component, null);
    }

    /**
     * Registers a component by its runtime type and metadata.
     */
    default <T> void registerComponent(T component, String metadata) {
        registerComponent(null, component, metadata);
    }

    /**
     * Returns the okaso version
     */
    String getVersion();

    // ============ Factory Method ============

    /**
     * Creates a new OkasoAPI instance for plugin use
     *
     * @param logger plugin logger
     * @param owner plugin instance or name
     * @return a new OkasoAPI instance
     */
    static OkasoAPI create(Logger logger, Object owner) {
        return new OkasoAPIImpl(logger, owner);
    }

    /**
     * Creates a new OkasoAPI instance using the plugin logger automatically.
     * This is the simplest integration path for Bukkit/Spigot plugins.
     */
    static OkasoAPI create(JavaPlugin plugin) {
        return new OkasoAPIImpl(plugin.getLogger(), plugin);
    }

    /**
     * Creates a new OkasoAPI instance with a custom configuration
     *
     * @param logger plugin logger
     * @param owner plugin instance or name
     * @param config okaso configuration
     * @return a new OkasoAPI instance
     */
    static OkasoAPI create(Logger logger, Object owner, OkasoConfig config) {
        return new OkasoAPIImpl(logger, owner, config);
    }

    /**
     * Creates a new OkasoAPI instance using the plugin logger automatically.
     * This is the simplest integration path for Bukkit/Spigot plugins.
     */
    static OkasoAPI create(JavaPlugin plugin, OkasoConfig config) {
        return new OkasoAPIImpl(plugin.getLogger(), plugin, config);
    }
}


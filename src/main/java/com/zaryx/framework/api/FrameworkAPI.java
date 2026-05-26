package com.zaryx.framework.api;

import com.zaryx.framework.core.*;
import com.zaryx.framework.core.cache.CacheManager;
import com.zaryx.framework.core.event.EventBus;
import com.zaryx.framework.core.registry.Registry;
import com.zaryx.framework.core.security.HashingService;
import com.zaryx.framework.core.serialization.FrameworkSerializer;
import com.zaryx.framework.core.text.MessageService;
import com.zaryx.framework.core.web.WebRequestService;
import com.zaryx.framework.bukkit.nms.NmsService;
import com.zaryx.framework.bukkit.simulation.PacketSimulationService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Main API for ZarFramework.
 * Provides unified access to framework components.
 *
 * Example usage as a plugin dependency:
 * <pre>
 * FrameworkAPI framework = FrameworkAPI.create(logger, plugin);
 * framework.initialize();
 *
 * CacheManager cache = framework.getCacheManager();
 * EventBus eventBus = framework.getEventBus();
 * </pre>
 */
public interface FrameworkAPI {

    /**
     * Framework initialization state
     */
    enum State {
        CREATED,
        INITIALIZED,
        RUNNING,
        DISABLED,
        ERROR
    }

    // ============ Lifecycle ============

    /**
     * Initializes all framework components
     * @return true if initialization was successful
     */
    boolean initialize();

    /**
     * Starts the framework (after `initialize`)
     * @return true if startup succeeded
     */
    boolean start();

    /**
     * Stops all framework components
     */
    void shutdown();

    /**
     * Returns the current framework state
     */
    State getState();

    /**
     * Checks if the framework is running
     */
    boolean isRunning();

    /**
     * Returns the framework logger
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
    FrameworkSerializer getSerializer();

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
     * Runs a task asynchronously with the framework executor.
     */
    default CompletableFuture<Void> runAsyncTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return CompletableFuture.runAsync(task, getExecutorService());
    }

    /**
     * Runs a supplier asynchronously with the framework executor.
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
     * Returns framework statistics
     */
    FrameworkStats getStats();

    /**
     * Returns debug information for the framework
     */
    FrameworkDebugInfo getDebugInfo();

    /**
     * Returns the current framework configuration
     */
    FrameworkConfig getConfig();

    /**
     * Reloads in-memory framework configuration state
     */
    boolean reloadConfig();

    /**
     * Restarts the framework using the current configuration
     */
    boolean restart();

    /**
     * Resets the framework fully and clears registered modules, components, and registries.
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
     * Returns the framework version
     */
    String getVersion();

    // ============ Factory Method ============

    /**
     * Creates a new FrameworkAPI instance for plugin use
     *
     * @param logger plugin logger
     * @param owner plugin instance or name
     * @return a new FrameworkAPI instance
     */
    static FrameworkAPI create(Logger logger, Object owner) {
        return new FrameworkAPIImpl(logger, owner);
    }

    /**
     * Creates a new FrameworkAPI instance using the plugin logger automatically.
     * This is the simplest integration path for Bukkit/Spigot plugins.
     */
    static FrameworkAPI create(JavaPlugin plugin) {
        return new FrameworkAPIImpl(plugin.getLogger(), plugin);
    }

    /**
     * Creates a new FrameworkAPI instance with a custom configuration
     *
     * @param logger plugin logger
     * @param owner plugin instance or name
     * @param config framework configuration
     * @return a new FrameworkAPI instance
     */
    static FrameworkAPI create(Logger logger, Object owner, FrameworkConfig config) {
        return new FrameworkAPIImpl(logger, owner, config);
    }

    /**
     * Creates a new FrameworkAPI instance using the plugin logger automatically.
     * This is the simplest integration path for Bukkit/Spigot plugins.
     */
    static FrameworkAPI create(JavaPlugin plugin, FrameworkConfig config) {
        return new FrameworkAPIImpl(plugin.getLogger(), plugin, config);
    }
}


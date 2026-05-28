package com.zaryx.okaso.bukkit.adapter;

import com.zaryx.okaso.api.OkasoAPI;
import com.zaryx.okaso.api.OkasoConfig;
import com.zaryx.okaso.core.*;
import com.zaryx.okaso.core.cache.CacheManager;
import com.zaryx.okaso.core.event.EventBus;
import com.zaryx.okaso.core.security.HashingService;
import com.zaryx.okaso.core.serialization.OkasoSerializer;
import com.zaryx.okaso.core.text.MessageService;
import com.zaryx.okaso.core.web.WebRequestService;
import com.zaryx.okaso.bukkit.nms.NmsService;
import com.zaryx.okaso.bukkit.simulation.PacketSimulationService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for integrating Okaso into Spigot plugins.
 *
 * Usage:
 * <pre>
 * public class MyPlugin extends JavaPlugin {
 *     private PluginOkasoAdapter okaso;
 *
 *     @Override
 *     public void onEnable() {
 *         okaso = new PluginOkasoAdapter(this)
 *             .withConfig(OkasoConfig.production())
 *             .initialize();
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         okaso.shutdown();
 *     }
 * }
 * </pre>
 */
public class PluginOkasoAdapter {

    private final JavaPlugin plugin;
    private OkasoAPI okaso;
    private OkasoConfig config;
    private Logger logger;
    private final List<Module> pendingModules;

    // ============ Constructor ============

    /**
     * Creates a new adapter for the plugin
     * @param plugin Spigot plugin
     */
    public PluginOkasoAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new OkasoConfig();
        this.pendingModules = new ArrayList<>();
    }

    // ============ Configuration ============

    /**
     * Sets the okaso configuration
     */
    public PluginOkasoAdapter withConfig(OkasoConfig config) {
        if (config != null) {
            this.config = new OkasoConfig(config);
        }
        return this;
    }

    /**
     * Enables development mode
     */
    public PluginOkasoAdapter developmentMode() {
        this.config = OkasoConfig.development();
        return this;
    }

    /**
     * Enables production mode
     */
    public PluginOkasoAdapter productionMode() {
        this.config = OkasoConfig.production();
        return this;
    }

    /**
     * Enables low-performance mode
     */
    public PluginOkasoAdapter lowPerformanceMode() {
        this.config = OkasoConfig.lowPerformance();
        return this;
    }

    /**
     * Enables high-performance mode
     */
    public PluginOkasoAdapter highPerformanceMode() {
        this.config = OkasoConfig.highPerformance();
        return this;
    }

    // ============ Initialization ============

    /**
     * Initializes the okaso
     */
    public PluginOkasoAdapter initialize() {
        try {
            logger.info("╔════════════════════════════════════════╗");
            logger.info("║  Initializing Okaso             ║");
            logger.info("║  Plugin: " + plugin.getName() + "                      ║");
            logger.info("╚════════════════════════════════════════╝");

            // Create okaso instance with plugin-scoped logger and config
            this.okaso = OkasoAPI.create(plugin, config);

            for (Module module : pendingModules) {
                okaso.registerModule(module);
            }
            pendingModules.clear();

            // Initialize
            if (!okaso.initialize()) {
                logger.severe("Okaso initialization failed");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return this;
            }

            // Start
            if (!okaso.start()) {
                logger.severe("Okaso startup failed");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return this;
            }

            logger.info("✓ Okaso initialized successfully");
            if (config.isDebugMode()) {
                logger.info("Debug info: " + okaso.getDebugInfo());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error during initialization", e);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

        return this;
    }

    /**
     * Stops the okaso
     */
    public void shutdown() {
        try {
            if (okaso != null && okaso.isRunning()) {
                logger.info("Stopping Okaso...");
                okaso.shutdown();
                logger.info("✓ Okaso stopped");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    // ============ Component Access ============

    /**
     * Returns the okaso instance
     */
    public OkasoAPI getOkaso() {
        if (okaso == null) {
            throw new IllegalStateException("Okaso has not been initialized");
        }
        return okaso;
    }

    /**
     * Returns the ModuleManager
     */
    public ModuleManager getModuleManager() {
        return getOkaso().getModuleManager();
    }

    /**
     * Checks whether a module exists
     */
    public boolean hasModule(String name) {
        return okaso != null && okaso.hasModule(name);
    }

    /**
     * Returns the module names
     */
    public java.util.Set<String> getModuleNames() {
        return okaso != null ? okaso.getModuleNames() : java.util.Collections.emptySet();
    }

    /**
     * Returns the EventBus
     */
    public EventBus getEventBus() {
        return getOkaso().getEventBus();
    }

    /**
     * Returns the CacheManager
     */
    public CacheManager getCacheManager() {
        return getOkaso().getCacheManager();
    }

    /**
     * Returns the okaso serializer.
     */
    public OkasoSerializer getSerializer() {
        return getOkaso().getSerializer();
    }

    /**
     * Returns the hashing service.
     */
    public HashingService getHasher() {
        return getOkaso().getHasher();
    }

    /**
     * Returns the HTTP/web request service.
     */
    public WebRequestService getWebRequests() {
        return getOkaso().getWebRequests();
    }

    /**
     * Returns the high-level messaging service.
     */
    public MessageService getMessages() {
        return getOkaso().getMessages();
    }

    /**
     * Returns the NMS service.
     */
    public NmsService getNms() {
        return getOkaso().getNms();
    }

    /**
     * Returns the client-side packet simulation service.
     */
    public PacketSimulationService getPacketSimulation() {
        return getOkaso().getPacketSimulation();
    }

    /**
     * Registers a custom module.
     * Modules may be registered before okaso initialization and will be initialized during startup.
     */
    public boolean registerModule(Module module) {
        if (okaso == null) {
            if (module == null) {
                logger.warning("Attempted to register a null module");
                return false;
            }
            pendingModules.add(module);
            return true;
        }
        return okaso.registerModule(module);
    }

    /**
     * Registers a custom component
     */
    public <T> void registerComponent(String name, T component) {
        if (okaso != null) {
            okaso.registerComponent(name, component);
        }
    }

    /**
     * Registers a custom component with automatic type detection
     */
    public <T> void registerComponent(T component) {
        if (okaso != null) {
            okaso.registerComponent(component);
        }
    }

    /**
     * Registers a custom component with automatic type detection and metadata
     */
    public <T> void registerComponent(T component, String metadata) {
        if (okaso != null) {
            okaso.registerComponent(component, metadata);
        }
    }

    /**
     * Checks whether a component exists
     */
    public boolean hasComponent(String name) {
        return okaso != null && okaso.hasComponent(name);
    }

    /**
     * Returns the component names
     */
    public java.util.Set<String> getComponentNames() {
        return okaso != null ? okaso.getComponentNames() : java.util.Collections.emptySet();
    }

    /**
     * Returns a component
     */
    public <T> T getComponent(String name, Class<T> type) {
        if (okaso == null) {
            return null;
        }
        return okaso.getComponent(name, type);
    }

    /**
     * Returns a required component or throws if it is not registered.
     */
    public <T> T requireComponent(String name, Class<T> type) {
        return getOkaso().requireComponent(name, type);
    }

    /**
     * Runs a task on the okaso async executor.
     */
    public CompletableFuture<Void> runAsyncTask(Runnable task) {
        return getOkaso().runAsyncTask(task);
    }

    /**
     * Runs a supplier on the okaso async executor.
     */
    public <T> CompletableFuture<T> supplyAsyncTask(Supplier<T> supplier) {
        return getOkaso().supplyAsyncTask(supplier);
    }

    // ============ Utilities ============

    /**
     * Returns the current okaso state
     */
    public OkasoAPI.State getState() {
        if (okaso == null) {
            return OkasoAPI.State.CREATED;
        }
        return okaso.getState();
    }

    /**
     * Checks if the okaso is running
     */
    public boolean isRunning() {
        return okaso != null && okaso.isRunning();
    }

    /**
     * Returns the okaso logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns okaso statistics
     */
    public String getStatsString() {
        if (okaso == null) {
            return "Okaso not initialized";
        }
        return okaso.getStats().toString();
    }

    /**
     * Returns debug information
     */
    public String getDebugString() {
        if (okaso == null) {
            return "Okaso not initialized";
        }
        return okaso.getDebugInfo().toString();
    }

    /**
     * Reloads the configuration
     */
    public boolean reload() {
        if (okaso == null) {
            return false;
        }
        return okaso.reloadConfig();
    }

    /**
     * Restarts the okaso
     */
    public boolean restart() {
        if (okaso == null) {
            return false;
        }
        return okaso.restart();
    }

    @Override
    public String toString() {
        return "PluginOkasoAdapter{" +
                "plugin=" + plugin.getName() +
                ", state=" + getState() +
                ", running=" + isRunning() +
                '}';
    }
}

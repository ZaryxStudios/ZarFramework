package com.zaryx.framework.bukkit.adapter;

import com.zaryx.framework.api.FrameworkAPI;
import com.zaryx.framework.api.FrameworkConfig;
import com.zaryx.framework.core.*;
import com.zaryx.framework.core.cache.CacheManager;
import com.zaryx.framework.core.event.EventBus;
import com.zaryx.framework.core.security.HashingService;
import com.zaryx.framework.core.serialization.FrameworkSerializer;
import com.zaryx.framework.core.text.MessageService;
import com.zaryx.framework.core.web.WebRequestService;
import com.zaryx.framework.bukkit.nms.NmsService;
import com.zaryx.framework.bukkit.simulation.PacketSimulationService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for integrating ZarFramework into Spigot plugins.
 *
 * Usage:
 * <pre>
 * public class MyPlugin extends JavaPlugin {
 *     private PluginFrameworkAdapter framework;
 *
 *     @Override
 *     public void onEnable() {
 *         framework = new PluginFrameworkAdapter(this)
 *             .withConfig(FrameworkConfig.production())
 *             .initialize();
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         framework.shutdown();
 *     }
 * }
 * </pre>
 */
public class PluginFrameworkAdapter {

    private final JavaPlugin plugin;
    private FrameworkAPI framework;
    private FrameworkConfig config;
    private Logger logger;
    private final List<Module> pendingModules;

    // ============ Constructor ============

    /**
     * Creates a new adapter for the plugin
     * @param plugin Spigot plugin
     */
    public PluginFrameworkAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new FrameworkConfig();
        this.pendingModules = new ArrayList<>();
    }

    // ============ Configuration ============

    /**
     * Sets the framework configuration
     */
    public PluginFrameworkAdapter withConfig(FrameworkConfig config) {
        if (config != null) {
            this.config = new FrameworkConfig(config);
        }
        return this;
    }

    /**
     * Enables development mode
     */
    public PluginFrameworkAdapter developmentMode() {
        this.config = FrameworkConfig.development();
        return this;
    }

    /**
     * Enables production mode
     */
    public PluginFrameworkAdapter productionMode() {
        this.config = FrameworkConfig.production();
        return this;
    }

    /**
     * Enables low-performance mode
     */
    public PluginFrameworkAdapter lowPerformanceMode() {
        this.config = FrameworkConfig.lowPerformance();
        return this;
    }

    /**
     * Enables high-performance mode
     */
    public PluginFrameworkAdapter highPerformanceMode() {
        this.config = FrameworkConfig.highPerformance();
        return this;
    }

    // ============ Initialization ============

    /**
     * Initializes the framework
     */
    public PluginFrameworkAdapter initialize() {
        try {
            logger.info("╔════════════════════════════════════════╗");
            logger.info("║  Initializing ZarFramework             ║");
            logger.info("║  Plugin: " + plugin.getName() + "                      ║");
            logger.info("╚════════════════════════════════════════╝");

            // Create framework instance with plugin-scoped logger and config
            this.framework = FrameworkAPI.create(plugin, config);

            for (Module module : pendingModules) {
                framework.registerModule(module);
            }
            pendingModules.clear();

            // Initialize
            if (!framework.initialize()) {
                logger.severe("Framework initialization failed");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return this;
            }

            // Start
            if (!framework.start()) {
                logger.severe("Framework startup failed");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return this;
            }

            logger.info("✓ ZarFramework initialized successfully");
            if (config.isDebugMode()) {
                logger.info("Debug info: " + framework.getDebugInfo());
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error during initialization", e);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

        return this;
    }

    /**
     * Stops the framework
     */
    public void shutdown() {
        try {
            if (framework != null && framework.isRunning()) {
                logger.info("Stopping ZarFramework...");
                framework.shutdown();
                logger.info("✓ ZarFramework stopped");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    // ============ Component Access ============

    /**
     * Returns the framework instance
     */
    public FrameworkAPI getFramework() {
        if (framework == null) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return framework;
    }

    /**
     * Returns the ModuleManager
     */
    public ModuleManager getModuleManager() {
        return getFramework().getModuleManager();
    }

    /**
     * Checks whether a module exists
     */
    public boolean hasModule(String name) {
        return framework != null && framework.hasModule(name);
    }

    /**
     * Returns the module names
     */
    public java.util.Set<String> getModuleNames() {
        return framework != null ? framework.getModuleNames() : java.util.Collections.emptySet();
    }

    /**
     * Returns the EventBus
     */
    public EventBus getEventBus() {
        return getFramework().getEventBus();
    }

    /**
     * Returns the CacheManager
     */
    public CacheManager getCacheManager() {
        return getFramework().getCacheManager();
    }

    /**
     * Returns the framework serializer.
     */
    public FrameworkSerializer getSerializer() {
        return getFramework().getSerializer();
    }

    /**
     * Returns the hashing service.
     */
    public HashingService getHasher() {
        return getFramework().getHasher();
    }

    /**
     * Returns the HTTP/web request service.
     */
    public WebRequestService getWebRequests() {
        return getFramework().getWebRequests();
    }

    /**
     * Returns the high-level messaging service.
     */
    public MessageService getMessages() {
        return getFramework().getMessages();
    }

    /**
     * Returns the NMS service.
     */
    public NmsService getNms() {
        return getFramework().getNms();
    }

    /**
     * Returns the client-side packet simulation service.
     */
    public PacketSimulationService getPacketSimulation() {
        return getFramework().getPacketSimulation();
    }

    /**
     * Registers a custom module.
     * Modules may be registered before framework initialization and will be initialized during startup.
     */
    public boolean registerModule(Module module) {
        if (framework == null) {
            if (module == null) {
                logger.warning("Attempted to register a null module");
                return false;
            }
            pendingModules.add(module);
            return true;
        }
        return framework.registerModule(module);
    }

    /**
     * Registers a custom component
     */
    public <T> void registerComponent(String name, T component) {
        if (framework != null) {
            framework.registerComponent(name, component);
        }
    }

    /**
     * Registers a custom component with automatic type detection
     */
    public <T> void registerComponent(T component) {
        if (framework != null) {
            framework.registerComponent(component);
        }
    }

    /**
     * Registers a custom component with automatic type detection and metadata
     */
    public <T> void registerComponent(T component, String metadata) {
        if (framework != null) {
            framework.registerComponent(component, metadata);
        }
    }

    /**
     * Checks whether a component exists
     */
    public boolean hasComponent(String name) {
        return framework != null && framework.hasComponent(name);
    }

    /**
     * Returns the component names
     */
    public java.util.Set<String> getComponentNames() {
        return framework != null ? framework.getComponentNames() : java.util.Collections.emptySet();
    }

    /**
     * Returns a component
     */
    public <T> T getComponent(String name, Class<T> type) {
        if (framework == null) {
            return null;
        }
        return framework.getComponent(name, type);
    }

    /**
     * Returns a required component or throws if it is not registered.
     */
    public <T> T requireComponent(String name, Class<T> type) {
        return getFramework().requireComponent(name, type);
    }

    /**
     * Runs a task on the framework async executor.
     */
    public CompletableFuture<Void> runAsyncTask(Runnable task) {
        return getFramework().runAsyncTask(task);
    }

    /**
     * Runs a supplier on the framework async executor.
     */
    public <T> CompletableFuture<T> supplyAsyncTask(Supplier<T> supplier) {
        return getFramework().supplyAsyncTask(supplier);
    }

    // ============ Utilities ============

    /**
     * Returns the current framework state
     */
    public FrameworkAPI.State getState() {
        if (framework == null) {
            return FrameworkAPI.State.CREATED;
        }
        return framework.getState();
    }

    /**
     * Checks if the framework is running
     */
    public boolean isRunning() {
        return framework != null && framework.isRunning();
    }

    /**
     * Returns the framework logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns framework statistics
     */
    public String getStatsString() {
        if (framework == null) {
            return "Framework not initialized";
        }
        return framework.getStats().toString();
    }

    /**
     * Returns debug information
     */
    public String getDebugString() {
        if (framework == null) {
            return "Framework not initialized";
        }
        return framework.getDebugInfo().toString();
    }

    /**
     * Reloads the configuration
     */
    public boolean reload() {
        if (framework == null) {
            return false;
        }
        return framework.reloadConfig();
    }

    /**
     * Restarts the framework
     */
    public boolean restart() {
        if (framework == null) {
            return false;
        }
        return framework.restart();
    }

    @Override
    public String toString() {
        return "PluginFrameworkAdapter{" +
                "plugin=" + plugin.getName() +
                ", state=" + getState() +
                ", running=" + isRunning() +
                '}';
    }
}

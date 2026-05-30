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

public class PluginOkasoAdapter {

    private final JavaPlugin plugin;
    private OkasoAPI okaso;
    private OkasoConfig config;
    private Logger logger;
    private final List<Module> pendingModules;

    public PluginOkasoAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = new OkasoConfig();
        this.pendingModules = new ArrayList<>();
    }

    public PluginOkasoAdapter withConfig(OkasoConfig config) {
        if (config != null) {
            this.config = new OkasoConfig(config);
        }
        return this;
    }

    public PluginOkasoAdapter developmentMode() {
        this.config = OkasoConfig.development();
        return this;
    }

    public PluginOkasoAdapter productionMode() {
        this.config = OkasoConfig.production();
        return this;
    }

    public PluginOkasoAdapter lowPerformanceMode() {
        this.config = OkasoConfig.lowPerformance();
        return this;
    }

    public PluginOkasoAdapter highPerformanceMode() {
        this.config = OkasoConfig.highPerformance();
        return this;
    }

    public PluginOkasoAdapter initialize() {
        try {
            logger.info("╔════════════════════════════════════════╗");
            logger.info("║  Initializing Okaso             ║");
            logger.info("║  Plugin: " + plugin.getName() + "                      ║");
            logger.info("╚════════════════════════════════════════╝");

            this.okaso = OkasoAPI.create(plugin, config);

            for (Module module : pendingModules) {
                okaso.registerModule(module);
            }
            pendingModules.clear();

            if (!okaso.initialize()) {
                logger.severe("Okaso initialization failed");
                Bukkit.getPluginManager().disablePlugin(plugin);
                return this;
            }

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

    public OkasoAPI getOkaso() {
        if (okaso == null) {
            throw new IllegalStateException("Okaso has not been initialized");
        }
        return okaso;
    }

    public ModuleManager getModuleManager() {
        return getOkaso().getModuleManager();
    }

    public boolean hasModule(String name) {
        return okaso != null && okaso.hasModule(name);
    }

    public java.util.Set<String> getModuleNames() {
        return okaso != null ? okaso.getModuleNames() : java.util.Collections.emptySet();
    }

    public EventBus getEventBus() {
        return getOkaso().getEventBus();
    }

    public CacheManager getCacheManager() {
        return getOkaso().getCacheManager();
    }

    public OkasoSerializer getSerializer() {
        return getOkaso().getSerializer();
    }

    public HashingService getHasher() {
        return getOkaso().getHasher();
    }

    public WebRequestService getWebRequests() {
        return getOkaso().getWebRequests();
    }

    public MessageService getMessages() {
        return getOkaso().getMessages();
    }

    public NmsService getNms() {
        return getOkaso().getNms();
    }

    public PacketSimulationService getPacketSimulation() {
        return getOkaso().getPacketSimulation();
    }

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

    public <T> void registerComponent(String name, T component) {
        if (okaso != null) {
            okaso.registerComponent(name, component);
        }
    }

    public <T> void registerComponent(T component) {
        if (okaso != null) {
            okaso.registerComponent(component);
        }
    }

    public <T> void registerComponent(T component, String metadata) {
        if (okaso != null) {
            okaso.registerComponent(component, metadata);
        }
    }

    public boolean hasComponent(String name) {
        return okaso != null && okaso.hasComponent(name);
    }

    public java.util.Set<String> getComponentNames() {
        return okaso != null ? okaso.getComponentNames() : java.util.Collections.emptySet();
    }

    public <T> T getComponent(String name, Class<T> type) {
        if (okaso == null) {
            return null;
        }
        return okaso.getComponent(name, type);
    }

    public <T> T requireComponent(String name, Class<T> type) {
        return getOkaso().requireComponent(name, type);
    }

    public CompletableFuture<Void> runAsyncTask(Runnable task) {
        return getOkaso().runAsyncTask(task);
    }

    public <T> CompletableFuture<T> supplyAsyncTask(Supplier<T> supplier) {
        return getOkaso().supplyAsyncTask(supplier);
    }

    public OkasoAPI.State getState() {
        if (okaso == null) {
            return OkasoAPI.State.CREATED;
        }
        return okaso.getState();
    }

    public boolean isRunning() {
        return okaso != null && okaso.isRunning();
    }

    public Logger getLogger() {
        return logger;
    }

    public String getStatsString() {
        if (okaso == null) {
            return "Okaso not initialized";
        }
        return okaso.getStats().toString();
    }

    public String getDebugString() {
        if (okaso == null) {
            return "Okaso not initialized";
        }
        return okaso.getDebugInfo().toString();
    }

    public boolean reload() {
        if (okaso == null) {
            return false;
        }
        return okaso.reloadConfig();
    }

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

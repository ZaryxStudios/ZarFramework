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

public interface OkasoAPI {

    enum State {
        CREATED, INITIALIZED, RUNNING, DISABLED, ERROR
    }

    boolean initialize();

    boolean start();

    void shutdown();

    State getState();

    boolean isRunning();

    Logger getLogger();

    ModuleManager getModuleManager();

    boolean hasModule(String name);

    java.util.Set<String> getModuleNames();

    EventBus getEventBus();

    OkasoSerializer getSerializer();

    HashingService getHasher();

    WebRequestService getWebRequests();

    MessageService getMessages();

    NmsService getNms();

    PacketSimulationService getPacketSimulation();

    CacheManager getCacheManager();

    <T extends Module> T getModule(String name, Class<T> type);

    boolean registerModule(Module module);

    <T> void registerComponent(String name, T component, String metadata);

    default <T> void registerComponent(String name, T component) {
        registerComponent(name, component, null);
    }

    boolean hasComponent(String name);

    java.util.Set<String> getComponentNames();

    <T> T getComponent(String name, Class<T> type);

    <T> Registry<T> getRegistry(String name);

    java.util.Set<String> getRegistryNames();

    <T> Registry<T> createRegistry(String name);

    boolean registerModulesFromPackage(String packageName);

    boolean registerModulesFromPackage(String packageName, java.util.function.Predicate<Module> filter);

    ExecutorService getExecutorService();

    default CompletableFuture<Void> runAsyncTask(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return CompletableFuture.runAsync(task, getExecutorService());
    }

    default <T> CompletableFuture<T> supplyAsyncTask(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("supplier must not be null");
        }
        return CompletableFuture.supplyAsync(supplier, getExecutorService());
    }

    default <T> T requireComponent(String name, Class<T> type) {
        T component = getComponent(name, type);
        if (component == null) {
            throw new IllegalStateException("Required component not found: " + name);
        }
        return component;
    }

    OkasoStats getStats();

    OkasoDebugInfo getDebugInfo();

    OkasoConfig getConfig();

    boolean reloadConfig();

    boolean restart();

    boolean reset();

    boolean hasRegistry(String name);

    <T> T getComponent(Class<T> type);

    default <T> void registerComponent(T component) {
        registerComponent(null, component, null);
    }

    default <T> void registerComponent(T component, String metadata) {
        registerComponent(null, component, metadata);
    }

    String getVersion();

    static OkasoAPI create(Logger logger, Object owner) {
        return new OkasoAPIImpl(logger, owner);
    }

    static OkasoAPI create(JavaPlugin plugin) {
        return new OkasoAPIImpl(plugin.getLogger(), plugin);
    }

    static OkasoAPI create(Logger logger, Object owner, OkasoConfig config) {
        return new OkasoAPIImpl(logger, owner, config);
    }

    static OkasoAPI create(JavaPlugin plugin, OkasoConfig config) {
        return new OkasoAPIImpl(plugin.getLogger(), plugin, config);
    }
}

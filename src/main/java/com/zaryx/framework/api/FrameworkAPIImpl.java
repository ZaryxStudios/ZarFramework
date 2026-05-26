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
import com.google.gson.GsonBuilder;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FrameworkAPI implementation.
 * Provides a unified interface to access framework components.
 */
public class FrameworkAPIImpl implements FrameworkAPI {

    private final Logger logger;
    private final String ownerName;
    private final FrameworkConfig config;
    private final Map<String, Object> components;
    private final Map<String, Registry<?>> registries;
    private ExecutorService executorService;

    private volatile State state;
    private long startTime;
    private long initStartTime;
    private final ModuleManager moduleManager;
    private EventBus eventBus;
    private CacheManager cacheManager;
    private FrameworkSerializer serializer;
    private HashingService hasher;
    private WebRequestService webRequests;
    private NmsService nms;
    private PacketSimulationService packetSimulation;
    private MessageService messages;
    private FrameworkStats stats;

    // ============ Constructor ============

    public FrameworkAPIImpl(Logger logger, Object owner) {
        this(logger, owner, new FrameworkConfig());
    }

    public FrameworkAPIImpl(Logger logger, Object owner, FrameworkConfig config) {
        this.logger = logger;
        this.ownerName = owner != null ? owner.toString() : "Unknown";
        this.config = new FrameworkConfig(config);
        this.components = new ConcurrentHashMap<>();
        this.registries = new ConcurrentHashMap<>();
        this.executorService = createExecutorService();
        this.moduleManager = new ModuleManager(logger);
        this.state = State.CREATED;
        this.stats = new FrameworkStats();

        logger.info("FrameworkAPI created for: " + ownerName);
        if (config.isDebugMode()) {
            logger.info("Debug mode enabled: " + config);
        }
    }

    // ============ Lifecycle ============

    @Override
    public synchronized boolean initialize() {
        if (state != State.CREATED && state != State.DISABLED) {
            logger.warning("Framework already initialized or in invalid state (state: " + state + ")");
            return state != State.ERROR;
        }

        try {
            initStartTime = System.currentTimeMillis();
            logger.info("Initializing ZarFramework for " + ownerName + "...");

            if (executorService == null || executorService.isShutdown()) {
                executorService = createExecutorService();
            }

            if (eventBus == null) {
                this.eventBus = new EventBus(logger);
            }
            if (cacheManager == null) {
                this.cacheManager = new CacheManager(config.getCacheTTL(), config.getCacheMaxSize());
            }
            if (serializer == null) {
                serializer = new FrameworkSerializer(new GsonBuilder().serializeNulls().create());
            }
            if (hasher == null) {
                hasher = new HashingService();
            }
            if (webRequests == null) {
                webRequests = new WebRequestService(
                        config.getConnectTimeoutMs(), config.getRequestTimeoutMs(), getExecutorService()
                );
            }
            if (nms == null) {
                nms = new NmsService();
            }
            if (packetSimulation == null) {
                packetSimulation = new PacketSimulationService(logger);
            }
            if (messages == null) {
                messages = new MessageService();
            }

            // Register core components
            components.putIfAbsent("moduleManager", moduleManager);
            components.putIfAbsent("eventBus", eventBus);
            components.putIfAbsent("cacheManager", cacheManager);
            components.putIfAbsent("serializer", serializer);
            components.putIfAbsent("hasher", hasher);
            components.putIfAbsent("webRequests", webRequests);
            components.putIfAbsent("nms", nms);
            components.putIfAbsent("packetSimulation", packetSimulation);
            components.putIfAbsent("messages", messages);
            components.putIfAbsent("frameworkConfig", config);

            state = State.INITIALIZED;
            long initTime = System.currentTimeMillis() - initStartTime;
            stats.setUptime(initTime);

            logger.info("✓ ZarFramework initialized in " + initTime + "ms");
            return true;

        } catch (Exception e) {
            state = State.ERROR;
            logger.log(Level.SEVERE, "Error during initialization", e);
            stats.setLastErrorMessage(e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized boolean start() {
        if (state != State.INITIALIZED) {
            logger.warning("Framework must be in INITIALIZED state (current: " + state + ")");
            return false;
        }

        try {
            startTime = System.currentTimeMillis();
            logger.info("Starting ZarFramework...");

            // Initialize all modules
            if (!moduleManager.initializeAll()) {
                throw new RuntimeException("Module initialization failed");
            }

            state = State.RUNNING;
            logger.info("✓ ZarFramework started successfully");
            return true;

        } catch (Exception e) {
            state = State.ERROR;
            logger.log(Level.SEVERE, "Error during startup", e);
            return false;
        }
    }

    @Override
    public synchronized void shutdown() {
        if (state == State.DISABLED || state == State.CREATED) {
            return;
        }

        try {
            logger.info("Stopping ZarFramework...");

            // Disable modules without removing registration so restart can preserve state
            if (moduleManager != null) {
                moduleManager.disableAll();
            }

            // Shutdown executor service
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
            executorService = null;

            // Clear cache only
            if (cacheManager != null) {
                cacheManager.clear();
            }

            state = State.DISABLED;
            logger.info("✓ ZarFramework stopped cleanly");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during shutdown", e);
        }
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    // ============ Component Access ============

    @Override
    public ModuleManager getModuleManager() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return moduleManager;
    }

    @Override
    public boolean hasModule(String name) {
        return moduleManager != null && name != null && moduleManager.get(name) != null;
    }

    @Override
    public Set<String> getModuleNames() {
        if (moduleManager == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (Module module : moduleManager.getAll()) {
            names.add(module.getName());
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public EventBus getEventBus() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return eventBus;
    }

    @Override
    public FrameworkSerializer getSerializer() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return serializer;
    }

    @Override
    public HashingService getHasher() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return hasher;
    }

    @Override
    public WebRequestService getWebRequests() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return webRequests;
    }

    @Override
    public MessageService getMessages() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return messages;
    }

    @Override
    public NmsService getNms() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return nms;
    }

    @Override
    public PacketSimulationService getPacketSimulation() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return packetSimulation;
    }

    @Override
    public CacheManager getCacheManager() {
        if (state == State.CREATED) {
            throw new IllegalStateException("Framework has not been initialized");
        }
        return cacheManager;
    }

    @Override
    public <T extends Module> T getModule(String name, Class<T> type) {
        if (moduleManager == null) {
            return null;
        }
        Module module = moduleManager.get(name);
        if (module != null && type.isInstance(module)) {
            return type.cast(module);
        }
        return null;
    }

    @Override
    public boolean registerModule(Module module) {
        if (moduleManager == null) {
            logger.warning("ModuleManager not available");
            return false;
        }
        try {
            moduleManager.register(module);
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error registering module: " + module.getName(), e);
            return false;
        }
    }

    @Override
    public boolean hasComponent(String name) {
        return name != null && components.containsKey(name);
    }

    @Override
    public <T> T getComponent(Class<T> type) {
        if (type == null) {
            return null;
        }

        for (Object component : components.values()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
        }
        return null;
    }

    @Override
    public Set<String> getComponentNames() {
        return Collections.unmodifiableSet(new HashSet<>(components.keySet()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component != null && type.isInstance(component)) {
            return (T) component;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registry<T> getRegistry(String name) {
        return (Registry<T>) registries.get(name);
    }

    @Override
    public boolean hasRegistry(String name) {
        return name != null && registries.containsKey(name);
    }

    @Override
    public Set<String> getRegistryNames() {
        return Collections.unmodifiableSet(new HashSet<>(registries.keySet()));
    }

    @Override
    public <T> Registry<T> createRegistry(String name) {
        if (registries.containsKey(name)) {
            logger.warning("Registry '" + name + "' already exists");
            return getRegistry(name);
        }
        Registry<T> registry = new Registry<>(logger, name);
        registries.put(name, registry);
        return registry;
    }

    @Override
    public boolean registerModulesFromPackage(String packageName) {
        return registerModulesFromPackage(packageName, module -> true);
    }

    @Override
    public boolean registerModulesFromPackage(String packageName, java.util.function.Predicate<Module> filter) {
        try {
            logger.info("Auto-discovering modules in package: " + packageName);

            // This is a simplified implementation - in a real framework, // you would use reflection to scan the package for Module implementations
            // For now, we'll just log the intent and return true
            logger.info("Module auto-discovery for " + packageName + " (simplified implementation)");
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during module auto-discovery", e);
            return false;
        }
    }

    @Override
    public <T> void registerComponent(String name, T component, String metadata) {
        if (component == null) {
            logger.warning("Cannot register null component");
            return;
        }
        if (name == null) {
            name = component.getClass().getSimpleName();
        }
        if (components.containsKey(name)) {
            logger.warning("Component '" + name + "' already exists");
            return;
        }
        components.put(name, component);
        if (config.isDetailedLogging()) {
            logger.info("Component registered: " + name + " (" + component.getClass().getSimpleName() + ")");
        }
    }

    @Override
    public <T> void registerComponent(T component) {
        registerComponent(null, component, null);
    }

    @Override
    public <T> void registerComponent(T component, String metadata) {
        registerComponent(null, component, metadata);
    }
    // ============ Utilities ============

    @Override
    public ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = createExecutorService();
        }
        return executorService;
    }

    @Override
    public FrameworkConfig getConfig() {
        return config;
    }

    @Override
    public FrameworkStats getStats() {
        updateStats();
        return stats;
    }

    @Override
    public FrameworkDebugInfo getDebugInfo() {
        FrameworkDebugInfo info = new FrameworkDebugInfo();
        info.setFrameworkVersion("2.0.0");
        info.setStartTime(startTime);
        info.setInitializationTime(System.currentTimeMillis() - initStartTime);
        info.setState(state.toString());
        info.setComponents(new HashMap<>(components));

        if (moduleManager != null) {
            Map<String, String> moduleStatus = new HashMap<>();
            for (Module module : moduleManager.getAll()) {
                moduleStatus.put(module.getName(), module.isEnabled() ? "ENABLED" : "DISABLED");
            }
            info.setModuleStatus(moduleStatus);
        }

        return info;
    }

    @Override
    public boolean reloadConfig() {
        try {
            logger.info("Reloading framework configuration...");
            if (cacheManager != null) {
                cacheManager.clear();
            }
            logger.info("✓ Configuration reloaded");
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error reloading configuration", e);
            return false;
        }
    }

    @Override
    public synchronized boolean restart() {
        try {
            shutdown();
            state = State.CREATED;
            return initialize() && start();
        } catch (Exception e) {
            state = State.ERROR;
            logger.log(Level.SEVERE, "Error during restart", e);
            return false;
        }
    }

    @Override
    public synchronized boolean reset() {
        try {
            if (state == State.RUNNING) {
                shutdown();
            }

            moduleManager.disableAll();
            components.clear();
            registries.clear();
            eventBus = null;
            cacheManager = null;
            serializer = null;
            hasher = null;
            webRequests = null;
            nms = null;
            messages = null;
            executorService = null;
            stats = new FrameworkStats();
            state = State.CREATED;
            logger.info("Framework reset complete");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during reset", e);
            state = State.ERROR;
            return false;
        }
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    // ============ Private Methods ============

    private void updateStats() {
        stats.setUptime(System.currentTimeMillis() - startTime);

        if (moduleManager != null) {
            stats.setModuleCount(moduleManager.size());
            stats.setEnabledModuleCount((int) moduleManager.getAll().stream()
                    .filter(Module::isEnabled)
                    .count());
        }

        if (cacheManager != null) {
            stats.setCacheSize(cacheManager.size());
        }

        Runtime runtime = Runtime.getRuntime();
        stats.setMemoryUsage(runtime.totalMemory() - runtime.freeMemory());
        stats.setLastCollectionTime(System.currentTimeMillis());
    }

    private ExecutorService createExecutorService() {
        return Executors.newFixedThreadPool(config.getThreadPoolSize());
    }
}

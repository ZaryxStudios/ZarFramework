package com.zaryx.okaso.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Legacy global okaso singleton.
 * Prefer the per-plugin {@link com.zaryx.okaso.api.OkasoAPI} instance instead.
 */
@Deprecated
public class OkasoCore {

    private static OkasoCore instance;

    private final Logger logger;
    private final ModuleManager moduleManager;
    private final Map<String, Object> components;
    private volatile boolean running;

    private OkasoCore(Logger logger) {
        this.logger = logger;
        this.moduleManager = new ModuleManager(logger);
        this.components = new ConcurrentHashMap<>();
        this.running = false;
    }

    /**
     * Initializes the core okaso.
     * @deprecated Use {@link com.zaryx.okaso.api.OkasoAPI} instead.
     */
    @Deprecated
    public static synchronized void initialize(Logger logger) {
        if (instance == null) {
            instance = new OkasoCore(logger);
        }
    }

    /**
     * Returns the singleton instance.
     * @deprecated Use {@link com.zaryx.okaso.api.OkasoAPI} instead.
     */
    @Deprecated
    public static OkasoCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("OkasoCore has not been initialized");
        }
        return instance;
    }

    /**
     * Returns the ModuleManager
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * Registers a component
     */
    public <T> void registerComponent(String name, T component) {
        if (name == null || component == null) {
            logger.warning("Cannot register null component");
            return;
        }

        if (components.containsKey(name)) {
            logger.warning("Component '" + name + "' is already registered");
            return;
        }

        components.put(name, component);
        logger.fine("Component registered: " + name);
    }

    /**
     * Returns a component
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component != null && type.isInstance(component)) {
            return (T) component;
        }
        return null;
    }

    /**
     * Returns a component without type validation
     */
    public Object getComponent(String name) {
        return components.get(name);
    }

    /**
     * Checks whether a component exists
     */
    public boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    /**
     * Returns all registered components
     */
    public Collection<Object> getAllComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * Starts the okaso
     */
    public synchronized boolean start() {
        if (running) {
            logger.warning("Okaso is already running");
            return false;
        }

        try {
            logger.info("Starting OkasoCore...");

            if (!moduleManager.initializeAll()) {
                logger.severe("Error initializing modules");
                return false;
            }

            running = true;
            logger.info("OkasoCore started successfully");
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting OkasoCore", e);
            return false;
        }
    }

    /**
     * Stops the okaso
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        try {
            logger.info("Stopping OkasoCore...");
            moduleManager.disableAll();
            running = false;
            logger.info("OkasoCore stopped");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error stopping OkasoCore", e);
        }
    }

    /**
     * Checks whether the okaso is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns a short status summary
     */
    public String getStatus() {
        return "OkasoCore | Status: " + (running ? "ACTIVE" : "INACTIVE") +
               " | Modules: " + moduleManager.size() +
               " | Components: " + components.size();
    }

    /**
     * Returns the okaso logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Clears everything and resets the core
     */
    public synchronized void reset() {
        stop();
        components.clear();
        logger.info("OkasoCore reset");
    }
}

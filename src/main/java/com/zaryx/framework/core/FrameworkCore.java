package com.zaryx.framework.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Legacy global framework singleton.
 * Prefer the per-plugin {@link com.zaryx.framework.api.FrameworkAPI} instance instead.
 */
@Deprecated
public class FrameworkCore {

    private static FrameworkCore instance;

    private final Logger logger;
    private final ModuleManager moduleManager;
    private final Map<String, Object> components;
    private volatile boolean running;

    private FrameworkCore(Logger logger) {
        this.logger = logger;
        this.moduleManager = new ModuleManager(logger);
        this.components = new ConcurrentHashMap<>();
        this.running = false;
    }

    /**
     * Initializes the core framework.
     * @deprecated Use {@link com.zaryx.framework.api.FrameworkAPI} instead.
     */
    @Deprecated
    public static synchronized void initialize(Logger logger) {
        if (instance == null) {
            instance = new FrameworkCore(logger);
        }
    }

    /**
     * Returns the singleton instance.
     * @deprecated Use {@link com.zaryx.framework.api.FrameworkAPI} instead.
     */
    @Deprecated
    public static FrameworkCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FrameworkCore has not been initialized");
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
     * Starts the framework
     */
    public synchronized boolean start() {
        if (running) {
            logger.warning("Framework is already running");
            return false;
        }

        try {
            logger.info("Starting FrameworkCore...");
            
            if (!moduleManager.initializeAll()) {
                logger.severe("Error initializing modules");
                return false;
            }

            running = true;
            logger.info("FrameworkCore started successfully");
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting FrameworkCore", e);
            return false;
        }
    }

    /**
     * Stops the framework
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }

        try {
            logger.info("Stopping FrameworkCore...");
            moduleManager.disableAll();
            running = false;
            logger.info("FrameworkCore stopped");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error stopping FrameworkCore", e);
        }
    }

    /**
     * Checks whether the framework is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns a short status summary
     */
    public String getStatus() {
        return "FrameworkCore | Status: " + (running ? "ACTIVE" : "INACTIVE") +
               " | Modules: " + moduleManager.size() +
               " | Components: " + components.size();
    }

    /**
     * Returns the framework logger
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
        logger.info("FrameworkCore reset");
    }
}

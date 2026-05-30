package com.zaryx.okaso.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
public class OkasoCore {

    // Legacy bootstrap container kept for older integrations.
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

    @Deprecated
    // Creates the singleton instance once.
    public static synchronized void initialize(Logger logger) {
        if (instance == null) {
            instance = new OkasoCore(logger);
        }
    }

    @Deprecated
    // Returns the singleton instance or fails fast if it was not initialized.
    public static OkasoCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("OkasoCore has not been initialized");
        }
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

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

    // Reads a typed component from the shared registry.
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component != null && type.isInstance(component)) {
            return (T) component;
        }
        return null;
    }

    public Object getComponent(String name) {
        return components.get(name);
    }

    public boolean hasComponent(String name) {
        return components.containsKey(name);
    }

    // Exposes the registered components as a read-only view.
    public Collection<Object> getAllComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    // Starts the core only after modules are ready.
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

    // Stops modules first and then flips the running flag off.
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

    public boolean isRunning() {
        return running;
    }

    public String getStatus() {
        return "OkasoCore | Status: " + (running ? "ACTIVE" : "INACTIVE") +
               " | Modules: " + moduleManager.size() +
               " | Components: " + components.size();
    }

    public Logger getLogger() {
        return logger;
    }

    // Resets the legacy core state for tests or manual restarts.
    public synchronized void reset() {
        stop();
        components.clear();
        logger.info("OkasoCore reset");
    }
}

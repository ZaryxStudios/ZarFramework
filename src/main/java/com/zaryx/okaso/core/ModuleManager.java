package com.zaryx.okaso.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleManager {

    // Keeps module registration and lifecycle state in one place.
    private final Logger logger;
    private final Map<String, Module> modules;
    private final List<Module> loadOrder;
    private volatile boolean initialized;

    public ModuleManager(Logger logger) {
        this.logger = logger;
        this.modules = new ConcurrentHashMap<>();
        this.loadOrder = Collections.synchronizedList(new ArrayList<>());
        this.initialized = false;
    }

    public boolean register(Module module) {
        if (module == null) {
            logger.warning("Attempted to register a null module");
            return false;
        }

        String name = module.getName();
        if (modules.containsKey(name)) {
            logger.warning("Module '" + name + "' is already registered");
            return false;
        }

        modules.put(name, module);
        loadOrder.add(module);
        logger.info("Module registered: " + name);
        return true;
    }

    // Returns a module by its registered name.
    public Module get(String name) {
        return modules.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(String name, Class<T> type) {
        Module module = modules.get(name);
        if (module != null && type.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    public Collection<Module> getAll() {
        return modules.values();
    }

    // Initializes modules in the same order they were registered.
    public boolean initializeAll() {
        if (initialized) {
            logger.warning("Modules have already been initialized");
            return false;
        }

        logger.info("Initializing " + loadOrder.size() + " module(s)...");
        boolean success = true;

        for (Module module : loadOrder) {
            try {
                module.initialize();
                logger.info("Module initialized: " + module.getName());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error initializing module: " + module.getName(), e);
                if (module.isCritical()) {
                    logger.severe("Critical error in module " + module.getName() + ". Aborting load.");
                    success = false;
                    break;
                }
            }
        }

        if (success) {
            initialized = true;
            logger.info("All modules were initialized successfully");
        }
        return success;
    }

    // Disables modules in reverse order to respect dependencies.
    public void disableAll() {
        logger.info("Disabling modules...");

        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            Module module = loadOrder.get(i);
            try {
                module.disable();
                logger.info("Module disabled: " + module.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error disabling module: " + module.getName(), e);
            }
        }

        modules.clear();
        loadOrder.clear();
        initialized = false;
        logger.info("All modules were disabled");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int size() {
        return modules.size();
    }

    public int registerAll(Collection<Module> modules) {
        int count = 0;
        for (Module module : modules) {
            if (register(module)) {
                count++;
            }
        }
        return count;
    }

    public int registerAll(Module... modules) {
        return registerAll(Arrays.asList(modules));
    }

    // Checks whether a module name is already registered.
    public boolean isRegistered(String name) {
        return name != null && modules.containsKey(name);
    }

    public boolean unregister(String name) {
        Module module = modules.remove(name);
        if (module != null) {
            loadOrder.remove(module);
            logger.info("Module unregistered: " + name);
            return true;
        }
        return false;
    }

    // Returns the registration order used during startup and shutdown.
    public List<Module> getLoadOrder() {
        return Collections.unmodifiableList(loadOrder);
    }

    public int getEnabledCount() {
        return (int) modules.values().stream()
                .filter(Module::isEnabled)
                .count();
    }

    public int getDisabledCount() {
        return (int) modules.values().stream()
                .filter(module -> !module.isEnabled())
                .count();
    }

    public String getStatusSummary() {
        return String.format("Modules: %d total, %d enabled, %d disabled", size(), getEnabledCount(), getDisabledCount());
    }

    // Validates basic module metadata before use.
    public boolean validateAll() {
        boolean allValid = true;
        for (Module module : modules.values()) {
            try {

                if (module.getName() == null || module.getName().trim().isEmpty()) {
                    logger.warning("Module has no name");
                    allValid = false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error validating module: " + module.getName(), e);
                allValid = false;
            }
        }
        return allValid;
    }

    // Enables a single module by calling its initialize hook.
    public boolean enableModule(String name) {
        Module module = get(name);
        if (module != null && !module.isEnabled()) {
            try {
                module.initialize();
                logger.info("Module enabled: " + name);
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error enabling module: " + name, e);
                return false;
            }
        }
        return false;
    }

    // Disables a single module by calling its shutdown hook.
    public boolean disableModule(String name) {
        Module module = get(name);
        if (module != null && module.isEnabled()) {
            try {
                module.disable();
                logger.info("Module disabled: " + name);
                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error disabling module: " + name, e);
                return false;
            }
        }
        return false;
    }
}

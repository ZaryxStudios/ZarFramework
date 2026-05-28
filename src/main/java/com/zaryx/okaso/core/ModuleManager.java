package com.zaryx.okaso.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central module manager for the okaso.
 * Responsible for registering, initializing, and disabling modules.
 */
public class ModuleManager {

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

    /**
     * Registers a module
     * @param module module to register
     * @return true if registration succeeded
     */
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

    /**
     * Retrieves a registered module
     * @param name module name
     * @return the module or null if missing
     */
    public Module get(String name) {
        return modules.get(name);
    }

    /**
     * Retrieves a module with type casting
     * @param name module name
     * @param type expected type
     * @return the cast module or null
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T get(String name, Class<T> type) {
        Module module = modules.get(name);
        if (module != null && type.isInstance(module)) {
            return (T) module;
        }
        return null;
    }

    /**
     * Retrieves all registered modules
     * @return collection of modules
     */
    public Collection<Module> getAll() {
        return modules.values();
    }

    /**
     * Initializes all registered modules
     * @return true if all modules initialized successfully
     */
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

    /**
     * Disables all modules in reverse order
     */
    public void disableAll() {
        logger.info("Disabling modules...");

        // Disable in reverse order
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

    /**
     * Checks whether modules are initialized
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the number of registered modules
     * @return module count
     */
    public int size() {
        return modules.size();
    }

    /**
     * Registers all modules from a collection
     * @param modules collection of modules to register
     * @return number of modules successfully registered
     */
    public int registerAll(Collection<Module> modules) {
        int count = 0;
        for (Module module : modules) {
            if (register(module)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Registers all modules from an array
     * @param modules array of modules to register
     * @return number of modules successfully registered
     */
    public int registerAll(Module... modules) {
        return registerAll(Arrays.asList(modules));
    }

    /**
     * Checks if a module is registered
     * @param name module name
     * @return true if module is registered
     */
    public boolean isRegistered(String name) {
        return name != null && modules.containsKey(name);
    }

    /**
     * Unregisters a module
     * @param name module name
     * @return true if module was unregistered
     */
    public boolean unregister(String name) {
        Module module = modules.remove(name);
        if (module != null) {
            loadOrder.remove(module);
            logger.info("Module unregistered: " + name);
            return true;
        }
        return false;
    }

    /**
     * Gets the load order of modules
     * @return list of modules in load order
     */
    public List<Module> getLoadOrder() {
        return Collections.unmodifiableList(loadOrder);
    }

    /**
     * Gets the number of enabled modules
     * @return number of enabled modules
     */
    public int getEnabledCount() {
        return (int) modules.values().stream()
                .filter(Module::isEnabled)
                .count();
    }

    /**
     * Gets the number of disabled modules
     * @return number of disabled modules
     */
    public int getDisabledCount() {
        return (int) modules.values().stream()
                .filter(module -> !module.isEnabled())
                .count();
    }

    /**
     * Gets a summary of module status
     * @return status summary
     */
    public String getStatusSummary() {
        return String.format("Modules: %d total, %d enabled, %d disabled", size(), getEnabledCount(), getDisabledCount());
    }

    /**
     * Validates all modules
     * @return true if all modules are valid
     */
    public boolean validateAll() {
        boolean allValid = true;
        for (Module module : modules.values()) {
            try {
                // Basic validation - could be extended
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

    /**
     * Enables a specific module
     * @param name module name
     * @return true if module was enabled
     */
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

    /**
     * Disables a specific module
     * @param name module name
     * @return true if module was disabled
     */
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

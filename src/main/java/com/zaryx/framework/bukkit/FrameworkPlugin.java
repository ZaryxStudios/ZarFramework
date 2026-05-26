package com.zaryx.framework.bukkit;

import com.google.gson.GsonBuilder;
import com.zaryx.framework.api.CustomEnchantmentsAPI;
import com.zaryx.framework.bukkit.command.core.CommandManager;
import com.zaryx.framework.bukkit.config.core.ConfigManager;
import com.zaryx.framework.bukkit.menu.core.MenuManager;
import com.zaryx.framework.bukkit.menu.provider.MenuProviderRegistry;
import com.zaryx.framework.bukkit.serialization.core.SerializationManager;
import com.zaryx.framework.bukkit.utility.Task;
import com.zaryx.framework.core.Module;
import com.zaryx.framework.core.ModuleManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main plugin class for ZarFramework.
 * Manages modular initialization of all framework components.
 */
@Getter
public class FrameworkPlugin extends JavaPlugin {

    private static FrameworkPlugin instance;

    public static FrameworkPlugin getInstance() {
        return instance;
    }

    private ModuleManager moduleManager;
    private Logger frameworkLogger;

    // Managers (modules)
    private CommandManager commandManager;
    private MenuManager menuManager;
    private MenuProviderRegistry providerRegistry;
    private ConfigManager configManager;
    private SerializationManager serializationManager;

    @Override
    public void onLoad() {
        instance = this;
        Task.setPlugin(this);
        this.frameworkLogger = this.getLogger();
        
        frameworkLogger.info("╔════════════════════════════════════════╗");
        frameworkLogger.info("║   ZarFramework loading...              ║");
        frameworkLogger.info("║   Version: " + getDescription().getVersion() + "                    ║");
        frameworkLogger.info("╚════════════════════════════════════════╝");
    }

    @Override
    public void onEnable() {
        try {
            // Initialize module system
            this.moduleManager = new ModuleManager(frameworkLogger);

            // Register modules
            if (!this.registerModules()) {
                frameworkLogger.severe("Failed to register critical modules. Plugin disabled.");
                this.setEnabled(false);
                return;
            }

            // Initialize all modules
            if (!this.moduleManager.initializeAll()) {
                frameworkLogger.severe("Module initialization failed. Plugin disabled.");
                this.setEnabled(false);
                return;
            }

            CustomEnchantmentsAPI.registerRuntimeListener(this);

            frameworkLogger.info("╔════════════════════════════════════════╗");
            frameworkLogger.info("║   ZarFramework enabled                 ║");
            frameworkLogger.info("║   Active modules: " + moduleManager.size() + "               ║");
            frameworkLogger.info("╚════════════════════════════════════════╝");

        } catch (Exception e) {
            frameworkLogger.log(Level.SEVERE, "Fatal error during plugin initialization", e);
            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (this.moduleManager != null) {
                this.moduleManager.disableAll();
            }
            CustomEnchantmentsAPI.unregisterRuntimeListener();
            frameworkLogger.info("ZarFramework disabled cleanly");
        } catch (Exception e) {
            frameworkLogger.log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }

    /**
    * Registers all framework modules
    * @return true if critical modules were registered successfully
     */
    private boolean registerModules() {
        // Configuration module
        this.configManager = new ConfigManager();
        ModuleWrapper configModule = new ModuleWrapper("config", configManager) {
            @Override
            public void initialize() {
                // ConfigManager instance created; perform any required startup here if needed
                frameworkLogger.info("ConfigManager ready");
            }

            @Override
            public boolean isCritical() {
                return true;
            }
        };
        moduleManager.register(configModule);

        // Serialization module
        this.serializationManager = new SerializationManager(GsonBuilder::setPrettyPrinting);
        ModuleWrapper serializationModule = new ModuleWrapper("serialization", serializationManager) {
            @Override
            public void initialize() {
                // Serialization is typically initialized in the constructor
            }
        };
        moduleManager.register(serializationModule);

        // Command module
        this.commandManager = new CommandManager(this, this.getName().toUpperCase());
        ModuleWrapper commandModule = new ModuleWrapper("command", commandManager) {
            @Override
            public void initialize() {
                frameworkLogger.info("Command system initialized");
            }

            @Override
            public boolean isCritical() {
                return false;
            }
        };
        moduleManager.register(commandModule);

        // Menu module
        this.menuManager = new MenuManager(this);
        ModuleWrapper menuModule = new ModuleWrapper("menu", menuManager) {
            @Override
            public void initialize() {
                frameworkLogger.info("Menu system initialized");
            }

            @Override
            public boolean isCritical() {
                return false;
            }
        };
        moduleManager.register(menuModule);

        // Menu provider module
        this.providerRegistry = new MenuProviderRegistry(this);
        ModuleWrapper providerModule = new ModuleWrapper("provider", providerRegistry) {
            @Override
            public void initialize() {
                frameworkLogger.info("Provider registry initialized");
            }

            @Override
            public boolean isCritical() {
                return false;
            }
        };
        moduleManager.register(providerModule);

        return true;
    }

    /**
     * Helper class for wrapping managers as modules
     */
    private static abstract class ModuleWrapper implements Module {
        protected final String name;
        protected final Object manager;
        protected volatile boolean enabled;

        public ModuleWrapper(String name, Object manager) {
            this.name = name;
            this.manager = manager;
            this.enabled = true;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public abstract void initialize();

        @Override
        public void disable() {
            this.enabled = false;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}

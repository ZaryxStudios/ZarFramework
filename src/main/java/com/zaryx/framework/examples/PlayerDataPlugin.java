package com.zaryx.framework.examples;

import com.zaryx.framework.bukkit.adapter.PluginFrameworkAdapter;
import com.zaryx.framework.core.cache.CacheManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Example: Player Data plugin
 * Demonstrates:
 * - Framework initialization
 * - Using cache for player data
 * - Event listeners
 * - Basic statistics
 */
public class PlayerDataPlugin extends JavaPlugin implements Listener {

    private PluginFrameworkAdapter framework;
    private CacheManager cache;
    private Map<UUID, PlayerData> playerDataMap;

    @Override
    public void onEnable() {
        try {
            // Initialize framework
            framework = new PluginFrameworkAdapter(this)
                    .productionMode()
                    .initialize();

            if (!framework.isRunning()) {
                getLogger().severe("✗ Framework failed to initialize");
                setEnabled(false);
                return;
            }

            // Get components
            cache = framework.getCacheManager();
            playerDataMap = new HashMap<>();

            // Register listener
            getServer().getPluginManager().registerEvents(this, this);

            getLogger().info("✓ PlayerDataPlugin initialized");

        } catch (Exception e) {
            getLogger().severe("✗ Error: " + e.getMessage());
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (framework != null) {
            getLogger().info("Saving player data...");
            playerDataMap.forEach((uuid, data) -> cache.put("player_" + uuid, data));
            framework.shutdown();
            getLogger().info("✓ PlayerDataPlugin disabled");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Try loading from cache
        PlayerData data = cache.get("player_" + uuid, PlayerData.class);
        
        if (data == null) {
            // Create a new entry
            data = new PlayerData(uuid, player.getName());
        }

        data.incrementLogins();
        playerDataMap.put(uuid, data);
        cache.put("player_" + uuid, data);

        player.sendMessage("§a✓ Welcome! Logins: " + data.getLogins());
        getLogger().info("Player: " + player.getName() + " | Logins: " + data.getLogins());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            cache.put("player_" + uuid, data);
            playerDataMap.remove(uuid);
        }
    }

    /**
     * Player data class
     */
    public static class PlayerData {
        private UUID uuid;
        private String name;
        private int logins;
        private long lastLogin;

        public PlayerData(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
            this.logins = 0;
            this.lastLogin = System.currentTimeMillis();
        }

        public void incrementLogins() {
            this.logins++;
            this.lastLogin = System.currentTimeMillis();
        }

        public int getLogins() {
            return logins;
        }
    }
}

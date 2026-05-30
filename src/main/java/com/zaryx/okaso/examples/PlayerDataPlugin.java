package com.zaryx.okaso.examples;

import com.zaryx.okaso.bukkit.adapter.PluginOkasoAdapter;
import com.zaryx.okaso.core.cache.CacheManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataPlugin extends JavaPlugin implements Listener {

    private PluginOkasoAdapter okaso;
    private CacheManager cache;
    private Map<UUID, PlayerData> playerDataMap;

    @Override
    public void onEnable() {
        try {

            okaso = new PluginOkasoAdapter(this)
                    .productionMode()
                    .initialize();

            if (!okaso.isRunning()) {
                getLogger().severe("✗ Okaso failed to initialize");
                setEnabled(false);
                return;
            }

            cache = okaso.getCacheManager();
            playerDataMap = new HashMap<>();

            getServer().getPluginManager().registerEvents(this, this);

            getLogger().info("✓ PlayerDataPlugin initialized");

        } catch (Exception e) {
            getLogger().severe("✗ Error: " + e.getMessage());
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (okaso != null) {
            getLogger().info("Saving player data...");
            playerDataMap.forEach((uuid, data) -> cache.put("player_" + uuid, data));
            okaso.shutdown();
            getLogger().info("✓ PlayerDataPlugin disabled");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerData data = cache.get("player_" + uuid, PlayerData.class);

        if (data == null) {

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

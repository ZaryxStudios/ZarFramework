package com.zaryx.framework.bukkit.menu.core;

import com.zaryx.framework.bukkit.FrameworkPlugin;
import com.zaryx.framework.bukkit.menu.adapter.MenuContext;
import com.zaryx.framework.bukkit.menu.event.MenuHandler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class MenuManager {

    private static MenuManager instance;
    private final Map<Menu, Set<UUID>> viewers;
    private final Map<Menu, Map<UUID, MenuContext>> contexts;

    public MenuManager(FrameworkPlugin plugin) {
        instance = this;

        this.viewers = new HashMap<>();
        this.contexts = new HashMap<>();

        this.init(plugin);

        Bukkit.getPluginManager().registerEvents(new MenuHandler(), plugin);
    }

    public static MenuManager getInstance() {
        return instance;
    }

    public Map<Menu, Set<UUID>> getViewers() {
        return this.viewers;
    }

    public void register(Menu menu, Player player) {
        this.viewers.computeIfAbsent(menu, k -> new HashSet<>()).add(player.getUniqueId());
    }

    public boolean unregister(Menu menu, Player player) {
        Set<UUID> set = this.viewers.get(menu);
        if (set == null) return true;

        set.remove(player.getUniqueId());

        if (set.isEmpty()) {
            this.viewers.remove(menu);
            this.contexts.remove(menu);
            return true;
        }

        return false;
    }

    public Set<UUID> getViewers(Menu menu) {
        return this.viewers.getOrDefault(menu, Collections.emptySet());
    }

    public void cleanupOfflinePlayer(UUID playerId) {
        this.contexts.values().forEach(contextMap -> contextMap.remove(playerId));
        this.viewers.values().forEach(viewerSet -> viewerSet.remove(playerId));
        this.viewers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        this.contexts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public MenuContext getContext(Menu menu, Player player) {
        return this.contexts
                .computeIfAbsent(menu, m -> new HashMap<>())
                .computeIfAbsent(player.getUniqueId(), u -> new MenuContext());
    }

    public void removeContext(Menu menu, Player player) {
        Map<UUID, MenuContext> map = this.contexts.get(menu);
        if (map != null) {
            map.remove(player.getUniqueId());
            if (map.isEmpty()) this.contexts.remove(menu);
        }
    }

    public void init(FrameworkPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Menu> menus = new ArrayList<>(this.viewers.keySet());

            for (Menu menu : menus) {
                Set<UUID> set = this.viewers.get(menu);
                if (set == null || set.isEmpty()) continue;
                if (!menu.isDirty()) continue;

                menu.update();
            }
        }, 1L, 1L);
    }
}
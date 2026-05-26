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
    private final Map<Menu, MenuSession> sessions;
    private final Map<UUID, Set<Menu>> playerMenus;

    public MenuManager(FrameworkPlugin plugin) {
        instance = this;

        // Use concurrent structures to tolerate async access from handlers
        this.sessions = new java.util.concurrent.ConcurrentHashMap<>();
        this.playerMenus = new java.util.concurrent.ConcurrentHashMap<>();

        this.init(plugin);

        Bukkit.getPluginManager().registerEvents(new MenuHandler(), plugin);
    }

    public static MenuManager getInstance() {
        return instance;
    }

    public Map<Menu, Set<UUID>> getViewers() {
        Map<Menu, Set<UUID>> snapshot = new HashMap<>();
        for (Map.Entry<Menu, MenuSession> entry : this.sessions.entrySet()) {
            MenuSession s = entry.getValue();
            snapshot.put(entry.getKey(), s == null ? java.util.Collections.<UUID>emptySet() : new java.util.HashSet<>(s.viewers));
        }
        return snapshot;
    }

    public void register(Menu menu, Player player) {
        UUID playerId = player.getUniqueId();
        MenuSession session = this.sessions.computeIfAbsent(menu, m -> new MenuSession());
        session.viewers.add(playerId);
        this.playerMenus.computeIfAbsent(playerId, id -> java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Menu, Boolean>())).add(menu);
    }

    public boolean unregister(Menu menu, Player player) {
        MenuSession session = this.sessions.get(menu);
        if (session == null) return true;

        UUID playerId = player.getUniqueId();
        session.viewers.remove(playerId);
        session.contexts.remove(playerId);

        Set<Menu> menus = this.playerMenus.get(playerId);
        if (menus != null) {
            menus.remove(menu);
            if (menus.isEmpty()) {
                this.playerMenus.remove(playerId);
            }
        }

        if (session.isEmpty()) {
            this.sessions.remove(menu);
            return true;
        }

        return false;
    }

    public Set<UUID> getViewers(Menu menu) {
        MenuSession session = this.sessions.get(menu);
        return session != null ? java.util.Collections.unmodifiableSet(session.viewers) : java.util.Collections.<UUID>emptySet();
    }

    public void cleanupOfflinePlayer(UUID playerId) {
        Set<Menu> menus = this.playerMenus.remove(playerId);
        if (menus != null) {
            for (Menu menu : new ArrayList<>(menus)) {
                MenuSession session = this.sessions.get(menu);
                if (session == null) {
                    continue;
                }

                session.viewers.remove(playerId);
                session.contexts.remove(playerId);

                if (session.isEmpty()) {
                    this.sessions.remove(menu);
                }
            }
        }
    }

    public MenuContext getContext(Menu menu, Player player) {
        MenuSession session = this.sessions.computeIfAbsent(menu, m -> new MenuSession());
        return session.contexts.computeIfAbsent(player.getUniqueId(), u -> new MenuContext());
    }

    public void removeContext(Menu menu, Player player) {
        MenuSession session = this.sessions.get(menu);
        if (session != null) {
            session.contexts.remove(player.getUniqueId());
            if (session.isEmpty()) {
                this.sessions.remove(menu);
            }
        }
    }

    public void init(FrameworkPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Menu> menus = new ArrayList<>(this.sessions.keySet());

            for (Menu menu : menus) {
                MenuSession session = this.sessions.get(menu);
                if (session == null || session.viewers.isEmpty()) continue;
                if (!menu.isDirty()) continue;

                try {
                    menu.update();
                } catch (Exception ignored) {
                    // safeguard: do not allow one menu failure to halt the scheduler
                }
            }
        }, 1L, 1L);
    }

    private static final class MenuSession {
        private final Set<UUID> viewers = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>());
        private final Map<UUID, MenuContext> contexts = new java.util.concurrent.ConcurrentHashMap<>();

        private boolean isEmpty() {
            return this.viewers.isEmpty() && this.contexts.isEmpty();
        }
    }
}

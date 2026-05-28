package com.zaryx.okaso.bukkit.menu.core;

import com.zaryx.okaso.bukkit.OkasoPlugin;
import com.zaryx.okaso.bukkit.menu.adapter.MenuContext;
import com.zaryx.okaso.bukkit.menu.event.MenuHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for menu sessions, dirty-queue processing, and stale-session cleanup.
 * Uses a tick-based scheduler for efficient rendering and periodic garbage collection.
 */
public class MenuManager {

    private static final Logger LOGGER = Logger.getLogger(MenuManager.class.getName());
    private static final int MAX_MENUS_PER_TICK = 10;
    private static final long STALE_SESSION_INTERVAL = 20L * 60L * 5L; // 5 minutes in ticks

    private static MenuManager instance;
    private final Map<Menu, MenuSession> sessions;
    private final Map<UUID, Set<Menu>> playerMenus;
    private final java.util.Queue<Menu> dirtyQueue;

    public MenuManager(OkasoPlugin plugin) {
        instance = this;

        // Use concurrent structures to tolerate async access from handlers
        this.sessions = new java.util.concurrent.ConcurrentHashMap<>();
        this.playerMenus = new java.util.concurrent.ConcurrentHashMap<>();
        this.dirtyQueue = new java.util.concurrent.ConcurrentLinkedDeque<>();

        this.init(plugin);

        Bukkit.getPluginManager().registerEvents(new MenuHandler(), plugin);
    }

    public static MenuManager getInstance() {
        return instance;
    }

    public Map<Menu, MenuSession> getSessions() { return sessions; }
    public Map<UUID, Set<Menu>> getPlayerMenus() { return playerMenus; }
    public java.util.Queue<Menu> getDirtyQueue() { return dirtyQueue; }

    public Map<Menu, Set<UUID>> getViewers() {
        Map<Menu, Set<UUID>> snapshot = new HashMap<>();
        for (Map.Entry<Menu, MenuSession> entry : this.sessions.entrySet()) {
            MenuSession s = entry.getValue();
            snapshot.put(entry.getKey(), s == null ? Collections.<UUID>emptySet() : new HashSet<>(s.viewers));
        }
        return snapshot;
    }

    public void register(Menu menu, Player player) {
        UUID playerId = player.getUniqueId();
        MenuSession session = this.sessions.computeIfAbsent(menu, m -> new MenuSession());
        session.viewers.add(playerId);
        this.playerMenus.computeIfAbsent(playerId, id ->
            Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Menu, Boolean>())
        ).add(menu);

        // Enqueue for initial render
        enqueueDirty(menu);
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
            this.dirtyQueue.remove(menu);
            return true;
        }

        return false;
    }

    /**
     * Mark a menu as needing an update and enqueue it for processing.
     */
    public void enqueueDirty(Menu menu) {
        if (menu == null || !menu.isDirty()) return;
        if (!this.dirtyQueue.contains(menu)) {
            this.dirtyQueue.offer(menu);
        }
    }

    public Set<UUID> getViewers(Menu menu) {
        MenuSession session = this.sessions.get(menu);
        return session != null
            ? Collections.unmodifiableSet(session.viewers)
            : Collections.<UUID>emptySet();
    }

    public void cleanupOfflinePlayer(UUID playerId) {
        Set<Menu> menus = this.playerMenus.remove(playerId);
        if (menus == null) return;
        for (Menu menu : new ArrayList<>(menus)) {
            MenuSession session = this.sessions.get(menu);
            if (session == null) continue;

            session.viewers.remove(playerId);
            session.contexts.remove(playerId);

            if (session.isEmpty()) {
                this.sessions.remove(menu);
                this.dirtyQueue.remove(menu);
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
                this.dirtyQueue.remove(menu);
            }
        }
    }

    private void init(OkasoPlugin plugin) {
        // High-frequency render tick: process up to MAX_MENUS_PER_TICK dirty menus per tick
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed = 0;
            while (processed < MAX_MENUS_PER_TICK) {
                Menu menu = this.dirtyQueue.poll();
                if (menu == null) break;

                MenuSession session = this.sessions.get(menu);
                if (session == null || session.viewers.isEmpty()) continue;

                try {
                    menu.update();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to update dirty menu", e);
                }
                processed++;
            }
        }, 1L, 1L);

        // Periodic garbage collection: remove empty/stale sessions every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                List<Menu> orphans = new ArrayList<>();
                for (Map.Entry<Menu, MenuSession> entry : this.sessions.entrySet()) {
                    MenuSession s = entry.getValue();
                    if (s == null || s.isEmpty()) {
                        orphans.add(entry.getKey());
                    }
                }
                for (Menu orphan : orphans) {
                    this.sessions.remove(orphan);
                    this.dirtyQueue.remove(orphan);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during stale session cleanup", e);
            }
        }, STALE_SESSION_INTERVAL, STALE_SESSION_INTERVAL);
    }

    private static final class MenuSession {
        private final Set<UUID> viewers = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<UUID, Boolean>());
        private final Map<UUID, MenuContext> contexts = new java.util.concurrent.ConcurrentHashMap<>();

        private boolean isEmpty() {
            return this.viewers.isEmpty() && this.contexts.isEmpty();
        }
    }
}

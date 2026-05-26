package com.zaryx.framework.bukkit.adapter.tablist;

import com.zaryx.framework.bukkit.adapter.tablist.extra.TabContext;
import com.zaryx.framework.bukkit.adapter.tablist.extra.TabEntry;
import com.zaryx.framework.bukkit.adapter.tablist.extra.TabPacket;
import com.zaryx.framework.bukkit.utility.Task;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class TabManager {

    private static TabManager instance;
    private final TabAdapter adapter;
    private final Map<UUID, Tab> context;

    public TabManager(TabAdapter adapter) {
        instance = this;

        this.adapter = adapter;
        this.context = new HashMap<>();

        Task.syncTimer(this::update, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(new TabHandler(), com.zaryx.framework.bukkit.FrameworkPlugin.getInstance());
    }

    public static TabManager getInstance() {
        return instance;
    }

    private void update() {
        for (UUID uuid : this.context.keySet()) {
            if (Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline()) {
                this.update(Bukkit.getPlayer(uuid));
            }
        }
    }

    public void create(Player player) {
        Tab tab = new Tab(player);
        this.context.put(player.getUniqueId(), tab);
    }

    public void update(Player player) {
        Tab tab = this.context.get(player.getUniqueId());
        if (tab == null) return;

        TabContext context = this.adapter.getLines(player);
        if (context == null) return;

        for (int slot = 0; slot < 80; slot++) {
            TabEntry oldEntry = tab.getLast(slot);
            TabEntry newEntry = context.get(slot);

            if (oldEntry == null && newEntry == null) continue;

            if (oldEntry == null) {
                TabPacket.add(player, slot, newEntry);
                tab.setLast(slot, newEntry);
                continue;
            }

            if (newEntry == null) {
                TabPacket.remove(player, slot);
                tab.setLast(slot, null);
                continue;
            }

            if (!Objects.equals(oldEntry.getValue(), newEntry.getValue()) ||
                !Objects.equals(oldEntry.getSignature(), newEntry.getSignature())) {

                TabPacket.remove(player, slot);
                TabPacket.add(player, slot, newEntry);
                tab.setLast(slot, newEntry);
                continue;
            }

            if (!Objects.equals(oldEntry.getText(), newEntry.getText())) {
                TabPacket.updateText(player, slot, newEntry.getText());
            }

            if (oldEntry.getPing() != newEntry.getPing()) {
                TabPacket.updatePing(player, slot, newEntry.getPing());
            }

            tab.setLast(slot, newEntry);
        }

        TabPacket.headerFooter(player, this.adapter.getHeader(player), this.adapter.getFooter(player));
    }

    public void destroy(Player player) {
        Tab tab = this.context.remove(player.getUniqueId());
        if (tab == null) return;

        for (int slot = 0; slot < 80; slot++) {
            if (tab.getLast(slot) != null) {
                TabPacket.remove(player, slot);
            }
        }
    }
}
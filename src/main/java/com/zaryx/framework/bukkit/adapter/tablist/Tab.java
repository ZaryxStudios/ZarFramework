package com.zaryx.framework.bukkit.adapter.tablist;

import com.zaryx.framework.bukkit.adapter.tablist.extra.TabEntry;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class Tab {

    private final Player player;
    private final TabEntry[] last = new TabEntry[80];

    public Tab(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public TabEntry[] getLastEntries() {
        return this.last;
    }

    public TabEntry getLast(int slot) {
        return this.last[slot];
    }

    public void setLast(int slot, TabEntry entry) {
        this.last[slot] = entry;
    }

    public void clear() {
        Arrays.fill(this.last, null);
    }
}

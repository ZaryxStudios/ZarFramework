package com.zaryx.framework.bukkit.menu.extra;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

@Getter
public class MenuClick {
    private final Player player;
    private final ClickType clickType;

    public MenuClick(Player player, ClickType clickType) {
        this.player = player;
        this.clickType = clickType;
    }

    public Player getPlayer() {
        return this.player;
    }

    public ClickType getClickType() {
        return this.clickType;
    }
}
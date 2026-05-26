package com.zaryx.framework.bukkit.adapter.nametag;

import org.bukkit.entity.Player;

public interface NameTagAdapter {
    String getPrefix(Player viewer, Player target);
    String getSuffix(Player viewer, Player target);
}
package com.zaryx.okaso.bukkit.adapter.tablist;

import com.zaryx.okaso.bukkit.adapter.tablist.extra.TabContext;
import org.bukkit.entity.Player;

import java.util.List;

public interface TabAdapter {
    List<String> getHeader(Player player);
    List<String> getFooter(Player player);
    TabContext getLines(Player player);
}

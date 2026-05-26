package com.zaryx.framework.bukkit.menu.event;

import com.zaryx.framework.bukkit.menu.adapter.MenuContext;
import com.zaryx.framework.bukkit.menu.core.Menu;
import com.zaryx.framework.bukkit.menu.core.MenuManager;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import com.zaryx.framework.bukkit.menu.extra.MenuPolicy;
import com.zaryx.framework.bukkit.utility.Task;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MenuHandler  implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof Menu)) return;

        int slot = event.getRawSlot();

        if (slot >= event.getInventory().getSize()) return;

        Menu menu = (Menu) event.getInventory().getHolder();
        MenuItem[] items = menu.getItems();
        if (items == null) return;

        MenuItem item = items[slot];
        if (item != null && item.isMovable()) return;


        event.setCancelled(true);
        if (item != null) menu.handleClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) return;

        Player player = (Player) event.getPlayer();
        Menu menu = (Menu) event.getInventory().getHolder();
        MenuContext context = menu.context(player);
        boolean navigating = context.has(MenuContext.NAVIGATING);
        boolean becameEmpty = MenuManager.getInstance().unregister(menu, player);

        if (becameEmpty) {
            menu.close(player);
        }

        if(navigating) {
            context.remove(MenuContext.NAVIGATING);
            return;
        }

        if (menu.getPolicy() == MenuPolicy.AUTO) {
            Task.sync(() -> menu.back(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuManager.getInstance().cleanupOfflinePlayer(event.getPlayer().getUniqueId());
    }
}
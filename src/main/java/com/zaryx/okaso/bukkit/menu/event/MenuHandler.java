package com.zaryx.okaso.bukkit.menu.event;

import com.zaryx.okaso.bukkit.menu.adapter.MenuContext;
import com.zaryx.okaso.bukkit.menu.core.Menu;
import com.zaryx.okaso.bukkit.menu.core.MenuManager;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;
import com.zaryx.okaso.bukkit.menu.extra.MenuPolicy;
import com.zaryx.okaso.bukkit.utility.Task;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MenuHandler implements Listener {

    private static final Logger LOGGER = Logger.getLogger(MenuHandler.class.getName());

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Menu menu = resolveMenu(event);
        if (menu == null) return;

        int slot = event.getRawSlot();
        if (!isTopInventorySlot(event, slot)) return;

        MenuItem item = menu.getItem(slot);

        event.setCancelled(item == null || !item.isMovable());

        if (item != null) {
            try {
                menu.handleClick(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception handling menu click at slot " + slot, e);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Menu menu = resolveMenu(event);
        if (menu == null) return;

        MenuContext context = menu.context(player);
        boolean navigating = context != null && Boolean.TRUE.equals(context.get(MenuContext.NAVIGATING));

        menu.close(player);

        if (!navigating && menu.getPolicy() == MenuPolicy.AUTO) {
            Task.sync(() -> {
                try {
                    menu.back(player);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception during auto-back navigation", e);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MenuManager.getInstance().cleanupOfflinePlayer(player.getUniqueId());
    }

    private Menu resolveMenu(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return null;
        if (event.getInventory() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    private Menu resolveMenu(InventoryCloseEvent event) {
        if (event.getInventory() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    private boolean isTopInventorySlot(InventoryClickEvent event, int slot) {
        return slot >= 0 && slot < event.getInventory().getSize();
    }
}

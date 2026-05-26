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
        Menu menu = resolveMenu(event);
        if (menu == null) return;

        int slot = event.getRawSlot();
        if (!isTopInventorySlot(event, slot)) return;

        MenuItem item = menu.getItem(slot);
        if (item != null && item.isMovable()) return;

        event.setCancelled(true);
        if (item != null) {
            try {
                menu.handleClick(event);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Menu menu = resolveMenu(event);
        if (menu == null) return;

        Player player = (Player) event.getPlayer();
        MenuContext context = menu.context(player);
        boolean navigating = context != null && context.has(MenuContext.NAVIGATING);

        // Run per-player close lifecycle and let MenuManager handle session cleanup
        menu.close(player);

        if (navigating) return; // do not trigger auto-back when navigation was intentional

        if (menu.getPolicy() == MenuPolicy.AUTO) {
            Task.sync(() -> {
                try {
                    menu.back(player);
                } catch (Exception ignored) {}
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuManager.getInstance().cleanupOfflinePlayer(event.getPlayer().getUniqueId());
    }

    private Menu resolveMenu(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return null;
        if (event.getInventory() == null || event.getInventory().getHolder() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    private Menu resolveMenu(InventoryCloseEvent event) {
        if (event.getInventory() == null || event.getInventory().getHolder() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    private boolean isTopInventorySlot(InventoryClickEvent event, int slot) {
        return slot >= 0 && slot < event.getInventory().getSize();
    }
}

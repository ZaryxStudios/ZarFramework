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

/**
 * Bukkit {@link Listener} that intercepts inventory events and delegates them
 * to the appropriate {@link Menu} instance.
 *
 * <h3>Event priority design:</h3>
 * <ul>
 *   <li><b>LOWEST</b> for click handling – ensures menus can cancel clicks before
 *       other plugins process them.</li>
 *   <li><b>MONITOR</b> for close/quit – observes the final state without interfering
 *       with other handlers.</li>
 * </ul>
 */
public class MenuHandler implements Listener {

    private static final Logger LOGGER = Logger.getLogger(MenuHandler.class.getName());

    /**
     * Handle inventory clicks within a menu.
     * <p>
     * Cancels the event for non-movable items, then delegates to
     * {@link Menu#handleClick} for action execution.
     *
     * @param event the Bukkit click event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Menu menu = resolveMenu(event);
        if (menu == null) return;

        int slot = event.getRawSlot();
        if (!isTopInventorySlot(event, slot)) return;

        MenuItem item = menu.getItem(slot);
        // Cancel if the item is not movable (or slot is empty but belongs to the menu)
        event.setCancelled(item == null || !item.isMovable());

        if (item != null) {
            try {
                menu.handleClick(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception handling menu click at slot " + slot, e);
            }
        }
    }

    /**
     * Handle inventory close events for menus.
     * <p>
     * Runs the menu close lifecycle and, if the policy is {@link MenuPolicy#AUTO},
     * automatically navigates back to the previous menu (unless currently navigating).
     *
     * @param event the Bukkit close event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Menu menu = resolveMenu(event);
        if (menu == null) return;

        MenuContext context = menu.context(player);
        boolean navigating = context != null && Boolean.TRUE.equals(context.get(MenuContext.NAVIGATING));

        menu.close(player);

        // Auto-back: only when policy is AUTO and the close was not a navigation step
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

    /**
     * Clean up menu sessions when a player disconnects.
     *
     * @param event the Bukkit quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MenuManager.getInstance().cleanupOfflinePlayer(player.getUniqueId());
    }

    /**
     * Resolve a {@link Menu} from a click event's inventory holder.
     *
     * @param event the click event
     * @return the Menu instance, or null if the inventory is not a okaso menu
     */
    private Menu resolveMenu(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return null;
        if (event.getInventory() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    /**
     * Resolve a {@link Menu} from a close event's inventory holder.
     *
     * @param event the close event
     * @return the Menu instance, or null if the inventory is not a okaso menu
     */
    private Menu resolveMenu(InventoryCloseEvent event) {
        if (event.getInventory() == null) return null;
        if (!(event.getInventory().getHolder() instanceof Menu)) return null;
        return (Menu) event.getInventory().getHolder();
    }

    /**
     * Check whether a slot index refers to the top (menu) inventory.
     *
     * @param event the click event
     * @param slot  the raw slot index
     * @return true if the slot is within the top inventory bounds
     */
    private boolean isTopInventorySlot(InventoryClickEvent event, int slot) {
        return slot >= 0 && slot < event.getInventory().getSize();
    }
}

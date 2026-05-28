package com.zaryx.okaso.bukkit.menu.core;

import com.zaryx.okaso.bukkit.menu.adapter.MenuContext;
import com.zaryx.okaso.bukkit.menu.adapter.MenuRenderer;
import com.zaryx.okaso.bukkit.menu.extra.MenuClick;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;
import com.zaryx.okaso.bukkit.menu.extra.MenuPolicy;
import com.zaryx.okaso.bukkit.menu.extra.MenuType;
import com.zaryx.okaso.bukkit.utility.Task;
import com.zaryx.okaso.bukkit.utility.color.ColorParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all okaso menus.
 * Provides lifecycle hooks, dirty-tracking, navigation, and item management.
 */
public abstract class Menu implements InventoryHolder {

    private static final Logger LOGGER = Logger.getLogger(Menu.class.getName());

    private final String title;
    private final int rows;
    private final MenuType type;
    private final MenuItem[] items;

    private Inventory inventory;
    private boolean dirty;
    private final BitSet dirtySlots;

    private MenuPolicy policy;
    private Menu back;

    public Menu(String title, int rows, MenuType type) {
        this.title = title;
        this.rows = rows;
        this.type = type;
        this.items = new MenuItem[rows * 9];
        this.policy = MenuPolicy.NONE;
        this.dirtySlots = new BitSet(rows * 9);

        if (type == null || type == MenuType.CHEST) {
            this.inventory = Bukkit.createInventory(this, rows * 9, ColorParser.parse(title));
        } else {
            this.inventory = Bukkit.createInventory(this, type.getType(), ColorParser.parse(title));
        }
    }

    // ---- Getters / Setters ----

    public String getTitle() { return this.title; }
    public int getRows() { return this.rows; }
    public MenuType getType() { return this.type; }
    public MenuItem[] getItems() { return this.items; }
    public Inventory getInventory() { return this.inventory; }
    public boolean isDirty() { return this.dirty; }
    public MenuPolicy getPolicy() { return this.policy; }
    public void setPolicy(MenuPolicy policy) { this.policy = policy; }
    public Menu getBack() { return this.back; }
    public void setBack(Menu back) { this.back = back; }

    /**
     * Mark the entire menu as dirty (full re-render) and enqueue for update.
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            this.dirtySlots.set(0, this.items.length);
            enqueueForUpdate();
        } else {
            this.dirtySlots.clear();
        }
    }

    /**
     * Internal: mark a single slot dirty (used by setItem) and enqueue.
     */
    private void markSlotDirty(int slot) {
        this.dirty = true;
        this.dirtySlots.set(slot);
        enqueueForUpdate();
    }

    private void enqueueForUpdate() {
        MenuManager mgr = MenuManager.getInstance();
        if (mgr != null) {
            mgr.enqueueDirty(this);
        }
    }

    // ---- Lifecycle ----

    /**
     * Open this menu for the given player, pushing the previous menu onto the back stack.
     */
    public void open(Player player) {
        if (player == null) return;

        MenuContext context = this.context(player);
        boolean navigating = Boolean.TRUE.equals(context.getOrDefault(MenuContext.NAVIGATING, false));

        try {
            // Push the currently open menu onto the back stack (if it's a Menu)
            if (player.getOpenInventory() != null
                    && player.getOpenInventory().getTopInventory() != null
                    && player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                Menu previous = (Menu) player.getOpenInventory().getTopInventory().getHolder();
                if (!navigating) {
                    context.getBackStack().push(previous);
                } else {
                    context.remove(MenuContext.NAVIGATING);
                }
            }

            this.beforeOpen(player);
            this.prepare(player);

            MenuManager.getInstance().register(this, player);
            player.openInventory(this.inventory);

            // Ensure onOpen runs on the main thread
            Task.sync(() -> this.onOpen(player));

            this.afterOpen(player);
            this.setDirty(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open menu '" + this.title + "' for " + player.getName(), e);
            MenuManager.getInstance().unregister(this, player);
        }
    }

    /**
     * Navigate back to the previous menu in the back stack.
     * Closes the inventory if there is no previous menu.
     */
    public void back(Player player) {
        if (player == null) return;

        MenuContext context = this.context(player);
        Deque<Menu> stack = context.getBackStack();

        if (stack == null || stack.isEmpty()) {
            player.closeInventory();
            return;
        }

        Menu previous = stack.pop();
        if (previous == null) {
            player.closeInventory();
            return;
        }

        context.set(MenuContext.NAVIGATING, true);
        previous.open(player);
    }

    /**
     * Close this menu for the given player, running lifecycle hooks and unregistering.
     */
    public void close(Player player) {
        if (player == null) return;

        this.beforeClose(player);

        // Always run per-player close hooks on the main thread
        Task.sync(() -> {
            try { this.onClose(player); } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception in onClose for menu '" + this.title + "'", e);
            }
            try { this.afterClose(player); } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception in afterClose for menu '" + this.title + "'", e);
            }
        });

        // Unregister this player from the menu; MenuManager will cleanup empty sessions
        MenuManager.getInstance().unregister(this, player);
    }

    /**
     * Render only the dirty slots to the inventory.
     * Called by MenuManager's tick scheduler.
     */
    public void update() {
        if (!this.dirty) return;

        try {
            MenuRenderer.render(this, this.dirtySlots);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Rendering failed for menu '" + this.title + "'", e);
        } finally {
            this.dirty = false;
            this.dirtySlots.clear();
        }
    }

    // ---- Click handling ----

    /**
     * Handle an inventory click event. Delegates to the MenuItem at the clicked slot.
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= this.items.length) return;
        MenuItem item = this.items[slot];
        if (item == null) return;

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        try {
            item.handle(new MenuClick(player, event.getClick()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception handling click at slot " + slot + " in menu '" + this.title + "'", e);
        }
    }

    // ---- Item management ----

    public void setItem(int slot, MenuItem item) {
        if (this.items == null) {
            throw new IllegalStateException("Menu not properly initialized");
        }
        int size = this.items.length;
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }

        this.items[slot] = item;

        if (this.inventory != null) {
            this.inventory.setItem(slot, item != null ? item.getItemStack() : null);
            markSlotDirty(slot);
        }
    }

    public MenuItem getItem(int slot) {
        if (slot < 0 || slot >= this.items.length) return null;
        return this.items[slot];
    }

    public void setItems(List<Integer> slots, MenuItem item) {
        if (slots == null || slots.isEmpty()) return;
        for (Integer slot : slots) {
            if (slot != null) setItem(slot, item);
        }
    }

    public void setItems(Map<Integer, MenuItem> itemsBySlot) {
        if (itemsBySlot == null || itemsBySlot.isEmpty()) return;
        for (Map.Entry<Integer, MenuItem> entry : itemsBySlot.entrySet()) {
            if (entry.getKey() != null) setItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Fill all slots with the given item.
     */
    public void fill(MenuItem item) {
        for (int slot = 0; slot < this.items.length; slot++) {
            setItem(slot, item);
        }
    }

    /**
     * Fill a contiguous range of slots [fromSlot, toSlot] with the given item.
     */
    public void fillRange(int fromSlot, int toSlot, MenuItem item) {
        int start = Math.max(0, Math.min(fromSlot, toSlot));
        int end = Math.min(this.items.length - 1, Math.max(fromSlot, toSlot));
        for (int slot = start; slot <= end; slot++) {
            setItem(slot, item);
        }
    }

    /**
     * Clear all items (fill with null).
     */
    public void clearItems() {
        fill(null);
    }

    /**
     * Fill the border (outer ring) of the menu with the given item.
     */
    public void fillBorder(MenuItem item) {
        int width = 9;
        int size = this.rows * width;
        int lastRowStart = size - width;

        for (int slot = 0; slot < size; slot++) {
            boolean top = slot < width;
            boolean bottom = slot >= lastRowStart;
            boolean left = slot % width == 0;
            boolean right = slot % width == width - 1;

            if (top || bottom || left || right) {
                setItem(slot, item);
            }
        }
    }

    // ---- Context ----

    /**
     * Get the per-player context for this menu.
     */
    public MenuContext context(Player player) {
        return MenuManager.getInstance().getContext(this, player);
    }

    // ---- Lifecycle hooks ----

    protected void prepare(Player player) {}
    protected void beforeOpen(Player player) {}
    protected abstract void onOpen(Player player);
    protected void afterOpen(Player player) {}

    protected void beforeClose(Player player) {}
    protected abstract void onClose(Player player);
    protected void afterClose(Player player) {}
}

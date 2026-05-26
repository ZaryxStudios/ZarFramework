package com.zaryx.framework.bukkit.menu.core;

import com.zaryx.framework.bukkit.menu.adapter.MenuContext;
import com.zaryx.framework.bukkit.menu.adapter.MenuRenderer;
import com.zaryx.framework.bukkit.menu.extra.MenuClick;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import com.zaryx.framework.bukkit.menu.extra.MenuPolicy;
import com.zaryx.framework.bukkit.menu.extra.MenuType;
import com.zaryx.framework.bukkit.utility.Task;
import com.zaryx.framework.bukkit.utility.color.ColorParser;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Deque;
import java.util.List;
import java.util.Map;

@Getter @Setter
public abstract class Menu implements InventoryHolder {

    private final String title;
    private final int rows;
    private final MenuType type;
    private final MenuItem[] items;

    private Inventory inventory;
    private boolean dirty;

    private MenuPolicy policy;
    private Menu back;

    public Menu(String title, int rows, MenuType type) {
        this.title = title;
        this.rows = rows;
        this.type = type;
        this.items = new MenuItem[rows * 9];

        this.policy = MenuPolicy.NONE;

        if (type == null || type == MenuType.CHEST) {
            this.inventory = Bukkit.createInventory(this, rows * 9, ColorParser.parse(title));
        } else {
            this.inventory = Bukkit.createInventory(this, type.getType(), ColorParser.parse(title));
        }
    }

    public String getTitle() {
        return this.title;
    }

    public int getRows() {
        return this.rows;
    }

    public MenuType getType() {
        return this.type;
    }

    public MenuItem[] getItems() {
        return this.items;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public MenuPolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(MenuPolicy policy) {
        this.policy = policy;
    }

    public Menu getBack() {
        return this.back;
    }

    public void setBack(Menu back) {
        this.back = back;
    }

    public void open(Player player) {
        if (player == null) return;

        MenuContext context = this.context(player);
        boolean navigating = Boolean.TRUE.equals(context.getOrDefault(MenuContext.NAVIGATING, false));

        try {
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null && player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
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

            // ensure onOpen runs on the main thread
            Task.sync(() -> this.onOpen(player));

            this.afterOpen(player);
            this.setDirty(true);
        } catch (Exception e) {
            // ensure we don't leave the player in an inconsistent state
            MenuManager.getInstance().unregister(this, player);
        }
    }

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

    public void close(Player player) {
        if (player == null) return;

        this.beforeClose(player);

        // Always run per-player close hooks on the main thread
        Task.sync(() -> {
            try {
                this.onClose(player);
            } catch (Exception ignored) {}
            try {
                this.afterClose(player);
            } catch (Exception ignored) {}
        });

        // Unregister this player from the menu; MenuManager will cleanup empty sessions
        MenuManager.getInstance().unregister(this, player);
    }

    public void update() {
        if (!this.dirty) return;
        this.dirty = false;

        try {
            MenuRenderer.render(this);
        } catch (Exception ignored) {
            // rendering failures should not propagate to scheduler
        }
    }

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= this.items.length) return;
        MenuItem item = this.items[slot];
        if (item == null) return;

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        try {
            item.handle(new MenuClick(player, event.getClick()));
        } catch (Exception ignored) {}
    }

    public void setItem(int slot, MenuItem item) {
        if (this.items == null) {
            throw new IllegalStateException("Menu not properly init");
        }

        int size = this.items.length;

        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Invalid slot index");
        }

        this.items[slot] = item;

        if (this.inventory != null) {
            this.inventory.setItem(slot, item != null ? item.getItemStack() : null);
            this.dirty = true;
        }
    }

    public MenuItem getItem(int slot) {
        if (slot < 0 || slot >= this.items.length) {
            return null;
        }
        return this.items[slot];
    }

    public void setItems(List<Integer> slots, MenuItem item) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        for (Integer slot : slots) {
            if (slot != null) {
                setItem(slot, item);
            }
        }
    }

    public void setItems(Map<Integer, MenuItem> itemsBySlot) {
        if (itemsBySlot == null || itemsBySlot.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, MenuItem> entry : itemsBySlot.entrySet()) {
            if (entry.getKey() != null) {
                setItem(entry.getKey(), entry.getValue());
            }
        }
    }

    public void fill(MenuItem item) {
        for (int slot = 0; slot < this.items.length; slot++) {
            setItem(slot, item);
        }
    }

    public void fillRange(int fromSlot, int toSlot, MenuItem item) {
        int start = Math.max(0, Math.min(fromSlot, toSlot));
        int end = Math.min(this.items.length - 1, Math.max(fromSlot, toSlot));
        for (int slot = start; slot <= end; slot++) {
            setItem(slot, item);
        }
    }

    public void clearItems() {
        fill(null);
    }

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

    public MenuContext context(Player player) {
        return MenuManager.getInstance().getContext(this, player);
    }

    protected void prepare(Player player) {}

    protected void beforeOpen(Player player) {}
    protected abstract void onOpen(Player player);
    protected void afterOpen(Player player) {}

    protected void beforeClose(Player player) {}
    protected abstract void onClose(Player player);
    protected void afterClose(Player player) {}
}

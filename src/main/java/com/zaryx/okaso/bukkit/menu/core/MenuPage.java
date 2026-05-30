package com.zaryx.okaso.bukkit.menu.core;

import com.zaryx.okaso.bukkit.menu.adapter.MenuContext;
import com.zaryx.okaso.bukkit.menu.extra.MenuItem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MenuPage extends Menu {

    private static final String PAGE_KEY = "menu:page";
    private static final int INVENTORY_WIDTH = 9;

    private transient Integer pageSizeCache;
    private transient List<Integer> contentSlotsCache;
    private transient List<Integer> navSlotsCache;

    public MenuPage(String title, int rows) {
        super(title, rows, null);
    }

    protected int getPage(Player player) {
        return context(player).getOrDefault(PAGE_KEY, 0);
    }

    protected void setPage(Player player, int page) {
        context(player).set(PAGE_KEY, Math.max(0, page));
        setDirty(true);
    }

    protected boolean hasNextPage(Player player, int total) {
        return (getPage(player) + 1) * getPageSize() < total;
    }

    protected boolean hasPreviousPage(Player player) {
        return getPage(player) > 0;
    }

    protected int getTotalPages(int totalContent) {
        if (totalContent <= 0) return 1;
        int pageSize = Math.max(1, getPageSize());
        return (totalContent + pageSize - 1) / pageSize;
    }

    protected int getPageSize() {
        if (pageSizeCache == null) {
            pageSizeCache = computeContentSlots().size();
        }
        return pageSizeCache;
    }

    protected List<Integer> getNavigationSlots() {
        if (navSlotsCache == null) {
            navSlotsCache = computeNavigationSlots();
        }
        return navSlotsCache;
    }

    protected List<Integer> getContentSlots() {
        if (contentSlotsCache == null) {
            contentSlotsCache = computeContentSlots();
        }
        return contentSlotsCache;
    }

    private List<Integer> computeNavigationSlots() {
        int size = getRows() * INVENTORY_WIDTH;
        if (size < INVENTORY_WIDTH) {
            return Collections.emptyList();
        }
        List<Integer> slots = new ArrayList<>(2);
        slots.add(size - INVENTORY_WIDTH);
        slots.add(size - 1);
        return Collections.unmodifiableList(slots);
    }

    private List<Integer> computeContentSlots() {
        List<Integer> slots = new ArrayList<>();
        List<Integer> nav = computeNavigationSlots();
        int rows = Math.max(1, getRows());
        int lastRow = rows - 1;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < INVENTORY_WIDTH; col++) {
                int slot = row * INVENTORY_WIDTH + col;
                if (row == lastRow && nav.contains(slot)) {
                    continue;
                }
                slots.add(slot);
            }
        }
        return Collections.unmodifiableList(slots);
    }

    protected void renderPage(Player player, List<MenuItem> content) {
        if (content == null) {
            content = Collections.emptyList();
        }

        int page = getPage(player);
        int start = page * Math.max(1, getPageSize());
        List<Integer> slots = getContentSlots();

        for (int i = 0; i < slots.size(); i++) {
            int index = start + i;
            setItem(slots.get(i), index < content.size() ? content.get(index) : null);
        }

        renderNavigation(player, content.size());
    }

    protected void renderNavigation(Player player, int total) {
        List<Integer> nav = getNavigationSlots();
        if (nav.size() < 2) return;

        setItem(nav.get(0), hasPreviousPage(player) ? getPreviousButton(player) : null);
        setItem(nav.get(1), hasNextPage(player, total) ? getNextButton(player) : null);
    }

    protected void nextPage(Player player, List<MenuItem> content) {
        if (content == null || !hasNextPage(player, content.size())) return;
        setPage(player, getPage(player) + 1);
        renderPage(player, content);
    }

    protected void previousPage(Player player, List<MenuItem> content) {
        if (content == null || !hasPreviousPage(player)) return;
        setPage(player, getPage(player) - 1);
        renderPage(player, content);
    }

    protected void openAtPage(Player player, int page, List<MenuItem> content) {
        if (content == null) {
            content = Collections.emptyList();
        }
        int totalPages = getTotalPages(content.size());
        int target = Math.max(0, Math.min(page, totalPages - 1));
        setPage(player, target);
        renderPage(player, content);
    }

    protected void invalidateLayoutCache() {
        this.pageSizeCache = null;
        this.contentSlotsCache = null;
        this.navSlotsCache = null;
    }

    protected abstract MenuItem getPreviousButton(Player player);

    protected abstract MenuItem getNextButton(Player player);
}

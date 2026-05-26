package com.zaryx.framework.bukkit.menu.core;

import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public abstract class MenuPage extends Menu {

    public MenuPage(String title, int rows) {
        super(title, rows, null);
    }

    protected int getPage(Player player) {
        return context(player).get("page");
    }

    protected void setPage(Player player, int page) {
        context(player).set("page", Math.max(0, page));
        setDirty(true);
    }

    protected boolean hasNextPage(Player player, int total) {
        return (getPage(player) + 1) * getPageSize() < total;
    }

    protected boolean hasPreviousPage(Player player) {
        return getPage(player) > 0;
    }

    protected int getTotalPages(int totalContent) {
        if (totalContent <= 0) {
            return 1;
        }
        int pageSize = Math.max(1, getPageSize());
        return (totalContent + pageSize - 1) / pageSize;
    }

    protected int getPageSize() {
        return (getRows() * 9) - getNavigationSlots().size();
    }

    protected List<Integer> getNavigationSlots() {
        int size = getRows() * 9;
        List<Integer> slots = new ArrayList<>();

        slots.add(size - 9);
        slots.add(size - 1);
        return slots;
    }

    protected List<Integer> getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        int lastRow = getRows() - 1;

        for (int i = 0; i < getRows(); i++) {
            for (int j = 0; j < 9; j++) {
                int slot = i * 9 + j;
                if (i == lastRow && getNavigationSlots().contains(slot)) continue;
                slots.add(slot);
            }
        }
        return slots;
    }

    protected void renderPage(Player player, List<MenuItem> content) {
        int page = getPage(player);
        int pageSize = getPageSize();
        int start = page * pageSize;

        List<Integer> slots = getContentSlots();

        for (int i = 0; i < slots.size(); i++) {
            int index = start + i;
            setItem(slots.get(i),
                    index < content.size() ? content.get(index) : null
            );
        }

        renderNavigation(player, content.size());
    }

    protected void renderNavigation(Player player, int total) {
        List<Integer> nav = getNavigationSlots();

        setItem(nav.get(0),
                hasPreviousPage(player) ? getPreviousButton(player) : null
        );

        setItem(nav.get(1),
                hasNextPage(player, total) ? getNextButton(player) : null
        );
    }

    protected void nextPage(Player player, List<MenuItem> content) {
        if (!hasNextPage(player, content.size())) {
            return;
        }
        setPage(player, getPage(player) + 1);
        renderPage(player, content);
    }

    protected void previousPage(Player player, List<MenuItem> content) {
        if (!hasPreviousPage(player)) {
            return;
        }
        setPage(player, getPage(player) - 1);
        renderPage(player, content);
    }

    protected void openAtPage(Player player, int page, List<MenuItem> content) {
        int totalPages = getTotalPages(content.size());
        int target = Math.max(0, Math.min(page, totalPages - 1));
        setPage(player, target);
        renderPage(player, content);
    }

    protected abstract MenuItem getPreviousButton(Player player);
    protected abstract MenuItem getNextButton(Player player);
}

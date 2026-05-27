package com.zaryx.framework.bukkit.menu.core;

import com.zaryx.framework.bukkit.menu.adapter.MenuContext;
import com.zaryx.framework.bukkit.menu.extra.MenuItem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract paginated menu that automatically splits content across pages.
 * <p>
 * The last row is reserved for navigation: slot {@code (lastRow, 0)} for the
 * "previous" button and slot {@code (lastRow, 8)} for the "next" button.
 * All remaining slots are considered content slots.
 *
 * <h3>Layout example (3-row menu):</h3>
 * <pre>
 *   Row 0: [content slots 0–8]
 *   Row 1: [content slots 9–17]
 *   Row 2: [prev] [  ] [  ] [  ] [  ] [  ] [  ] [  ] [next]
 * </pre>
 *
 * <h3>Usage:</h3>
 * <ol>
 *   <li>Subclass and implement {@link #getPreviousButton} and {@link #getNextButton}.</li>
 *   <li>Override {@link #onOpen} and call {@link #renderPage} with your content.</li>
 *   <li>Wire the navigation buttons to call {@link #nextPage}/{@link #previousPage}.</li>
 * </ol>
 *
 * Layout caches ({@link #getPageSize}, {@link #getContentSlots}, {@link #getNavigationSlots})
 * are computed lazily and can be invalidated with {@link #invalidateLayoutCache()}.
 */
public abstract class MenuPage extends Menu {

    private static final String PAGE_KEY = "menu:page";
    private static final int INVENTORY_WIDTH = 9;

    private transient Integer pageSizeCache;
    private transient List<Integer> contentSlotsCache;
    private transient List<Integer> navSlotsCache;

    /**
     * Create a new paginated menu.
     *
     * @param title the inventory title (may contain color codes)
     * @param rows  the number of rows (must be &ge; 1; last row is reserved for navigation)
     */
    public MenuPage(String title, int rows) {
        super(title, rows, null);
    }

    // ---- Page state ----

    /**
     * Get the current page index for the given player (0-based).
     *
     * @param player the viewer
     * @return the current page number, defaulting to 0
     */
    protected int getPage(Player player) {
        return context(player).getOrDefault(PAGE_KEY, 0);
    }

    /**
     * Set the current page index for the given player.
     * <p>
     * The value is clamped to a minimum of 0 and marks the menu as dirty.
     *
     * @param player the viewer
     * @param page   the target page number
     */
    protected void setPage(Player player, int page) {
        context(player).set(PAGE_KEY, Math.max(0, page));
        setDirty(true);
    }

    /**
     * Check whether a next page exists for the given total content size.
     *
     * @param player the viewer
     * @param total  the total number of content items
     * @return {@code true} if there are items beyond the current page
     */
    protected boolean hasNextPage(Player player, int total) {
        return (getPage(player) + 1) * getPageSize() < total;
    }

    /**
     * Check whether a previous page exists for the given player.
     *
     * @param player the viewer
     * @return {@code true} if the current page is greater than 0
     */
    protected boolean hasPreviousPage(Player player) {
        return getPage(player) > 0;
    }

    /**
     * Calculate the total number of pages needed for the given content size.
     *
     * @param totalContent the total number of items
     * @return the number of pages (minimum 1)
     */
    protected int getTotalPages(int totalContent) {
        if (totalContent <= 0) return 1;
        int pageSize = Math.max(1, getPageSize());
        return (totalContent + pageSize - 1) / pageSize;
    }

    // ---- Layout caches ----

    /**
     * Get the number of content slots per page.
     * <p>
     * Cached after the first call; use {@link #invalidateLayoutCache()} to recompute.
     *
     * @return the page size (number of content slots)
     */
    protected int getPageSize() {
        if (pageSizeCache == null) {
            pageSizeCache = computeContentSlots().size();
        }
        return pageSizeCache;
    }

    /**
     * Get the navigation button slot indices.
     * <p>
     * Always returns exactly two slots: the first and last slot of the last row.
     *
     * @return unmodifiable list of navigation slot indices
     */
    protected List<Integer> getNavigationSlots() {
        if (navSlotsCache == null) {
            navSlotsCache = computeNavigationSlots();
        }
        return navSlotsCache;
    }

    /**
     * Get the content slot indices (all slots except navigation slots).
     *
     * @return unmodifiable list of content slot indices
     */
    protected List<Integer> getContentSlots() {
        if (contentSlotsCache == null) {
            contentSlotsCache = computeContentSlots();
        }
        return contentSlotsCache;
    }

    /**
     * Compute navigation slots: first and last slot of the bottom row.
     */
    private List<Integer> computeNavigationSlots() {
        int size = getRows() * INVENTORY_WIDTH;
        if (size < INVENTORY_WIDTH) {
            return Collections.emptyList();
        }
        List<Integer> slots = new ArrayList<>(2);
        slots.add(size - INVENTORY_WIDTH);  // bottom-left
        slots.add(size - 1);                // bottom-right
        return Collections.unmodifiableList(slots);
    }

    /**
     * Compute content slots: all slots except navigation positions on the last row.
     */
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

    // ---- Rendering ----

    /**
     * Render the current page of content into the menu.
     * <p>
     * Clears all content slots first, then fills them with the appropriate
     * slice of the content list for the current page. Also renders navigation
     * buttons via {@link #renderNavigation}.
     *
     * @param player  the viewer
     * @param content the full list of items to paginate (null is treated as empty)
     */
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

    /**
     * Render the previous/next navigation buttons.
     * <p>
     * Only renders a button when the respective page exists.
     *
     * @param player the viewer
     * @param total  the total number of content items
     */
    protected void renderNavigation(Player player, int total) {
        List<Integer> nav = getNavigationSlots();
        if (nav.size() < 2) return;

        setItem(nav.get(0), hasPreviousPage(player) ? getPreviousButton(player) : null);
        setItem(nav.get(1), hasNextPage(player, total) ? getNextButton(player) : null);
    }

    // ---- Navigation ----

    /**
     * Advance to the next page and re-render.
     *
     * @param player  the viewer
     * @param content the full content list
     */
    protected void nextPage(Player player, List<MenuItem> content) {
        if (content == null || !hasNextPage(player, content.size())) return;
        setPage(player, getPage(player) + 1);
        renderPage(player, content);
    }

    /**
     * Go back to the previous page and re-render.
     *
     * @param player  the viewer
     * @param content the full content list
     */
    protected void previousPage(Player player, List<MenuItem> content) {
        if (content == null || !hasPreviousPage(player)) return;
        setPage(player, getPage(player) - 1);
        renderPage(player, content);
    }

    /**
     * Jump to a specific page and re-render.
     * <p>
     * The page is clamped to the valid range [0, totalPages - 1].
     *
     * @param player  the viewer
     * @param page    the target page (0-based)
     * @param content the full content list
     */
    protected void openAtPage(Player player, int page, List<MenuItem> content) {
        if (content == null) {
            content = Collections.emptyList();
        }
        int totalPages = getTotalPages(content.size());
        int target = Math.max(0, Math.min(page, totalPages - 1));
        setPage(player, target);
        renderPage(player, content);
    }

    /**
     * Invalidate layout caches (e.g. if rows change dynamically).
     * <p>
     * Subclasses should call this if they modify the row count after construction.
     */
    protected void invalidateLayoutCache() {
        this.pageSizeCache = null;
        this.contentSlotsCache = null;
        this.navSlotsCache = null;
    }

    // ---- Abstract hooks ----

    /**
     * Create the "previous page" button for the given viewer.
     *
     * @param player the viewer
     * @return the menu item for the previous-page button
     */
    protected abstract MenuItem getPreviousButton(Player player);

    /**
     * Create the "next page" button for the given viewer.
     *
     * @param player the viewer
     * @return the menu item for the next-page button
     */
    protected abstract MenuItem getNextButton(Player player);
}

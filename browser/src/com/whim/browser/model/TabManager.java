package com.whim.browser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the ordered collection of open {@link Tab}s and tracks which one is
 * active.
 *
 * <p>Thread-safety strategy: <b>synchronization</b>. Every public method is
 * {@code synchronized} on the instance monitor, so the tab list, the active
 * reference and the id counter are always mutated and read atomically. This
 * allows, for example, a background thread to open a tab for a popup while the
 * EDT enumerates {@link #tabs()} to rebuild the tab bar, with no risk of a
 * {@code ConcurrentModificationException} or a torn view of the counter.</p>
 */
public final class TabManager {

    private final List<Tab> tabs = new ArrayList<Tab>();
    private int nextId = 1;
    private Tab activeTab;

    /**
     * Creates a new tab, appends it in insertion order, makes it the active tab
     * and returns it.
     *
     * @return the freshly created, now-active tab
     */
    public synchronized Tab newTab() {
        Tab tab = new Tab(nextId++);
        tabs.add(tab);
        activeTab = tab;
        return tab;
    }

    /**
     * Closes the tab with the given id, if present. When the active tab is the
     * one closed, the neighbouring tab (the one that took its slot, or the new
     * last tab) becomes active. The active reference is only {@code null} once
     * every tab has been closed.
     *
     * @param id the id of the tab to close
     */
    public synchronized void closeTab(int id) {
        int pos = indexOf(id);
        if (pos < 0) {
            return;
        }
        Tab removed = tabs.remove(pos);
        if (removed != activeTab) {
            return;
        }
        if (tabs.isEmpty()) {
            activeTab = null;
        } else {
            // Prefer the tab that shifted into this slot, else the previous one.
            int neighbor = pos < tabs.size() ? pos : tabs.size() - 1;
            activeTab = tabs.get(neighbor);
        }
    }

    /** @return the active tab, or {@code null} if no tabs are open. */
    public synchronized Tab active() {
        return activeTab;
    }

    /**
     * Makes the tab with the given id active. Does nothing if no tab has that
     * id.
     *
     * @param id the id of the tab to activate
     */
    public synchronized void setActive(int id) {
        int pos = indexOf(id);
        if (pos >= 0) {
            activeTab = tabs.get(pos);
        }
    }

    /**
     * @return a defensive copy of the open tabs in insertion order. The returned
     *         list is independent of the manager and safe to iterate.
     */
    public synchronized List<Tab> tabs() {
        return new ArrayList<Tab>(tabs);
    }

    /** @return the index of the tab with {@code id}, or {@code -1} if absent. */
    private int indexOf(int id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }
}

package com.whim.browser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Back/forward navigation history for a single {@link Tab}, modelled as a linear
 * list of visited URLs plus a cursor pointing at the current entry.
 *
 * <p>Thread-safety strategy: <b>synchronization</b>. Every public method is
 * {@code synchronized} on the instance monitor, so the internal list and cursor
 * are only ever touched by one thread at a time. This lets a background loading
 * thread record a visit while the EDT queries {@link #canGoBack()} without
 * risking a corrupted view of the history.</p>
 *
 * <p>The cursor {@code index} is {@code -1} when the history is empty and
 * otherwise points at the currently displayed entry. Visiting a new URL after
 * having gone {@link #back()} truncates the forward entries, matching the
 * behaviour users expect from a web browser.</p>
 */
public final class HistoryManager {

    private final List<String> entries = new ArrayList<String>();
    private int index = -1;

    /**
     * Records a visit to {@code url}, making it the current entry. Any entries
     * ahead of the cursor (the "forward" history) are discarded first.
     *
     * @param url the URL that was navigated to; ignored if {@code null}
     */
    public synchronized void visit(String url) {
        if (url == null) {
            return;
        }
        // Drop any forward history before appending the new entry.
        while (entries.size() > index + 1) {
            entries.remove(entries.size() - 1);
        }
        entries.add(url);
        index = entries.size() - 1;
    }

    /** @return {@code true} if there is an entry behind the current one. */
    public synchronized boolean canGoBack() {
        return index > 0;
    }

    /** @return {@code true} if there is an entry ahead of the current one. */
    public synchronized boolean canGoForward() {
        return index >= 0 && index < entries.size() - 1;
    }

    /**
     * Moves the cursor one step back.
     *
     * @return the new current URL, or {@code null} if already at the oldest entry
     */
    public synchronized String back() {
        if (!canGoBack()) {
            return null;
        }
        index--;
        return entries.get(index);
    }

    /**
     * Moves the cursor one step forward.
     *
     * @return the new current URL, or {@code null} if already at the newest entry
     */
    public synchronized String forward() {
        if (!canGoForward()) {
            return null;
        }
        index++;
        return entries.get(index);
    }

    /** @return the URL at the cursor, or {@code null} if the history is empty. */
    public synchronized String current() {
        return index >= 0 ? entries.get(index) : null;
    }

    /**
     * @return a defensive copy of the full ordered history, oldest first. The
     *         returned list is independent of the manager and safe to iterate.
     */
    public synchronized List<String> snapshot() {
        return new ArrayList<String>(entries);
    }
}

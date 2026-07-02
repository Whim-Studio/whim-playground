package com.whim.browser.model;

/**
 * A single browser tab: a stable identifier, a mutable display title, its own
 * {@link HistoryManager}, and the most recent {@link WebResponse} it loaded.
 *
 * <p>Thread-safety strategy: <b>volatile fields</b>. The {@code title} and
 * {@code lastResponse} fields are written by background loading threads (as a
 * page finishes fetching) and read by the Swing Event Dispatch Thread (to
 * repaint tab labels and content). Declaring them {@code volatile} guarantees
 * that a write by one thread is immediately visible to the other, so the EDT
 * never sees a stale title or a half-published response reference. Because
 * {@link WebResponse} is itself immutable, publishing its reference through a
 * volatile field is sufficient to share it safely.</p>
 *
 * <p>The {@code id} is {@code final} and the {@link HistoryManager} reference is
 * {@code final} (the manager provides its own internal synchronization), so
 * neither needs further protection.</p>
 */
public final class Tab {

    private final int id;
    private final HistoryManager history = new HistoryManager();

    private volatile String title = "New Tab";
    private volatile WebResponse lastResponse;

    /**
     * Creates a tab with the given identifier and a default title of
     * {@code "New Tab"}.
     *
     * @param id a unique, stable identifier assigned by the {@link TabManager}
     */
    public Tab(int id) {
        this.id = id;
    }

    /** @return this tab's stable identifier. */
    public int getId() {
        return id;
    }

    /** @return the current display title (never {@code null}). */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the display title. A {@code null} value is coerced to {@code ""} so
     * readers never observe {@code null}.
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    /** @return this tab's own navigation history (never {@code null}). */
    public HistoryManager getHistory() {
        return history;
    }

    /** @return the most recently loaded response, or {@code null} if none yet. */
    public WebResponse getLastResponse() {
        return lastResponse;
    }

    /**
     * Records the most recently loaded response for this tab.
     *
     * @param r the response, or {@code null} to clear
     */
    public void setLastResponse(WebResponse r) {
        this.lastResponse = r;
    }
}

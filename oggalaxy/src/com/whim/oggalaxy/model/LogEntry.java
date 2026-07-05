package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;

/** An event-feed entry. Implements {@link Views.LogEntryView}. */
public final class LogEntry implements Views.LogEntryView, Serializable {

    private static final long serialVersionUID = 1L;

    public int tick;
    public Ids.LogCategory category;
    public String message;

    public LogEntry() {
    }

    public LogEntry(int tick, Ids.LogCategory category, String message) {
        this.tick = tick;
        this.category = category;
        this.message = message;
    }

    @Override public int tick() { return tick; }
    @Override public String timeText() { return "T+" + tick + "h"; }
    @Override public Ids.LogCategory category() { return category; }
    @Override public String message() { return message; }
}

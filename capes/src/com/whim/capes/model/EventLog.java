package com.whim.capes.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared, append-only event log (required for debugging and player review).
 * Notifies registered {@link Listener}s so the UI log panel can update live.
 *
 * <p>{@link #entries} and {@link #nextSequence} persist; {@link #listeners} are
 * transient (they are Swing components, re-registered by the UI after a load).
 */
public final class EventLog implements java.io.Serializable {
    public interface Listener { void onEntry(EventLogEntry entry); }

    private final List<EventLogEntry> entries = new ArrayList<EventLogEntry>();
    private transient List<Listener> listeners = new ArrayList<Listener>();
    private long nextSequence = 1;

    public List<EventLogEntry> entries() { return entries; }

    public void addListener(Listener l) { listeners.add(l); }

    public EventLogEntry log(EventLogEntry.Category category, String message) {
        return log(category, message, null);
    }

    public EventLogEntry log(EventLogEntry.Category category, String message, String narration) {
        EventLogEntry e = new EventLogEntry(nextSequence++, category, message, narration);
        entries.add(e);
        for (Listener l : listeners) l.onEntry(e);
        return e;
    }

    /** Re-create the transient listener list after deserialization. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        listeners = new ArrayList<Listener>();
    }
}

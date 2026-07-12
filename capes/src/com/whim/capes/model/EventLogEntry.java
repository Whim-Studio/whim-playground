package com.whim.capes.model;

/**
 * One immutable line in the shared audit log. Every rules-relevant action
 * (Stake, roll, React, Resolve, token transfer, Gloat) appends an entry so
 * players can review exactly what happened and developers can debug the
 * resource math. A {@link Category} tags the entry for filtering/colouring in
 * the UI, and {@link #narration} optionally carries free-form story text.
 */
public final class EventLogEntry implements java.io.Serializable {
    public enum Category { SCENE, PAGE, CONFLICT, ACTION, REACTION, STAKE, SPLIT, ABILITY, RESOLVE, GLOAT, TOKEN, SYSTEM, NARRATION }

    private final long sequence;
    private final Category category;
    private final String message;
    private final String narration; // may be null

    public EventLogEntry(long sequence, Category category, String message, String narration) {
        this.sequence = sequence;
        this.category = category;
        this.message = message;
        this.narration = narration;
    }

    public long sequence() { return sequence; }
    public Category category() { return category; }
    public String message() { return message; }
    public String narration() { return narration; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(category).append("] ").append(message);
        if (narration != null && !narration.isEmpty()) sb.append("  “").append(narration).append("”");
        return sb.toString();
    }
}

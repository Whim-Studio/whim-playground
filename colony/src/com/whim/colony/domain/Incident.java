package com.whim.colony.domain;

/**
 * A data holder describing a storyteller event: what happened, when, how bad,
 * and a human-readable blurb. This is pure data — the behaviour that mutates
 * the colony lives in an {@code Event} implementation (see the api package).
 */
public final class Incident {
    private final IncidentType type;
    private final long tick;
    private final String description;
    private final int severity; // 1 (mild) .. 10 (catastrophic)

    public Incident(IncidentType type, long tick, String description, int severity) {
        this.type = type;
        this.tick = tick;
        this.description = description;
        this.severity = severity;
    }

    public IncidentType getType() {
        return type;
    }

    public long getTick() {
        return tick;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverity() {
        return severity;
    }
}

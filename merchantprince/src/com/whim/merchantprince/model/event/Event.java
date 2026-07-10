package com.whim.merchantprince.model.event;

import java.io.Serializable;

/**
 * A single event occurrence in the game log: what happened, in which year, and
 * where (a city id, or -1 for world-scale / at-sea events). The event engine
 * creates these; the UI surfaces them via {@code EventDialog} / the status log.
 */
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    public final EventType type;
    public final int year;
    public final int cityId;      // -1 = world / at sea
    public final String message;

    public Event(EventType type, int year, int cityId, String message) {
        this.type = type;
        this.year = year;
        this.cityId = cityId;
        this.message = message;
    }
}

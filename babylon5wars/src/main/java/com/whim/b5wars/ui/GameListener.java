package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;

import java.util.List;

/** Panels implement this to react to state changes and appended engine events. */
public interface GameListener {

    /** State (selection, positions, phase, damage) changed — refresh/repaint. */
    void gameChanged();

    /** Engine produced new log events (may be empty). */
    void logEvents(List<GameEvent> events);
}

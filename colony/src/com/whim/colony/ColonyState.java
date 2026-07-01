package com.whim.colony;

import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * The single shared state aggregate for a running colony. It is read by the UI
 * (Task 3) and mutated by the engine (Task 2). This class holds NO simulation
 * logic — only plain accessors and trivial mutators.
 */
public final class ColonyState {
    /** Maximum number of retained log messages before the oldest are dropped. */
    public static final int MAX_MESSAGES = 200;

    private final ColonyMap map;
    private final List<Colonist> colonists = new ArrayList<Colonist>();
    private final Resources resources;
    private final List<String> messageLog = new ArrayList<String>();
    private long tick = 0L;
    private boolean paused = false;

    public ColonyState(ColonyMap map, Resources resources) {
        this.map = map;
        this.resources = resources;
    }

    public ColonyMap getMap() {
        return map;
    }

    /** @return the live, mutable list of colonists (engine adds/removes here). */
    public List<Colonist> getColonists() {
        return colonists;
    }

    public Resources getResources() {
        return resources;
    }

    /** @return the live message log, oldest first. */
    public List<String> getMessageLog() {
        return messageLog;
    }

    /**
     * Append a message to the log, evicting the oldest entries so the log never
     * exceeds {@link #MAX_MESSAGES}.
     */
    public void addMessage(String message) {
        if (message == null) {
            return;
        }
        messageLog.add(message);
        while (messageLog.size() > MAX_MESSAGES) {
            messageLog.remove(0);
        }
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    /** Advance the tick counter by one and return the new value. */
    public long incrementTick() {
        return ++tick;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }
}

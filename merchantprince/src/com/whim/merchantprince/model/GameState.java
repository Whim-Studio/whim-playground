package com.whim.merchantprince.model;

import com.whim.merchantprince.model.event.Event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete, serialisable snapshot of a game in progress (ARCHITECTURE.md).
 * Every screen reads and mutates this object through the engines; save/load simply
 * serialises the whole tree. The Venice trading calendar runs in years from a start
 * year toward a configurable end year (the original allowed 15..192-year games).
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public int year;
    public int startYear;
    public int endYear;

    public final List<Family> families = new ArrayList<Family>();
    public final List<City> cities = new ArrayList<City>();
    public final List<TransportUnit> units = new ArrayList<TransportUnit>();
    public final List<Event> log = new ArrayList<Event>();

    /** Index into {@link #families} for the human player. */
    public int playerId = 0;

    public boolean gameOver = false;
    public boolean victory = false;
    public String gameOverReason = "";

    // ---- lookup helpers -------------------------------------------------

    public Family player() { return family(playerId); }

    public Family family(int id) {
        for (Family f : families) if (f.id == id) return f;
        return null;
    }

    public City city(int id) {
        for (City c : cities) if (c.id == id) return c;
        return null;
    }

    public TransportUnit unit(int id) {
        for (TransportUnit u : units) if (u.id == id) return u;
        return null;
    }

    /** Units owned by a family. */
    public List<TransportUnit> unitsOf(int familyId) {
        List<TransportUnit> out = new ArrayList<TransportUnit>();
        for (TransportUnit u : units) if (u.ownerId == familyId) out.add(u);
        return out;
    }

    /** Append an event to the running game log. */
    public void logEvent(Event e) { log.add(e); }
}

package com.whim.nobunaga.domain;

import java.util.List;

/**
 * Drives season/year progression. Delegates the rules of a season to the
 * {@link GameEngine}, then advances the calendar. The UI calls {@link #endSeason}
 * for its "End Season" button and {@link #seasonHeader} for the header label.
 */
public final class GameLoopManager {
    private final GameEngine engine;

    public GameLoopManager(GameEngine engine) {
        this.engine = engine;
    }

    /** Resolve the current season via the engine, then advance the clock. */
    public List<String> endSeason(GameState s) {
        List<String> log = engine.endSeason(s);
        s.advanceClock();
        return log;
    }

    /** e.g. "1560 Spring". */
    public String seasonHeader(GameState s) {
        return s.year + " " + s.season.label();
    }
}

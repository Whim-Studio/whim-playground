package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.GameState;

/**
 * World-scale events (GAME_DESIGN_REFERENCE §5): plague, war, papal interdict, and
 * the early Reformation. Rolled once per year; effects feed pricing (crisis goods
 * spike) and family fortunes.
 *
 * <p>Contract frozen for T0. Implementation to be completed by the Economy task (T1).
 */
public final class EventEngine {
    private EventEngine() { }

    /** Roll and apply this year's world events, logging each into {@link GameState#log}. */
    public static void rollYearlyEvents(GameState s, Rng rng) {
        // TODO(T1): roll PLAGUE / WAR / INTERDICT by Constants odds; apply price and
        // stock effects; trigger REFORMATION once past REFORMATION_EARLIEST_YEAR.
    }
}

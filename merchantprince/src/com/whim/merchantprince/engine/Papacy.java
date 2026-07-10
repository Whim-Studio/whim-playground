package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.model.event.Event;
import com.whim.merchantprince.model.event.EventType;

/**
 * Papacy control (GAME_DESIGN_REFERENCE §6). The original let a family influence
 * cardinals toward controlling the papacy, and a controlled papacy could lift or
 * impose interdicts. The exact numbers are unconfirmed, so control is a tunable
 * function of how many cardinals a family has bribed (plus an automatic lock if it
 * already holds the POPE office).
 *
 * <p>Helper file added by the Politics task (T2). There is no persistent
 * {@code interdictActive} flag on {@link GameState} yet (see build report — a model
 * request), so interdict lift/impose is expressed by logging a world event and by
 * the derived {@link #interdictInEffect} scan of the game log.
 */
public final class Papacy {
    private Papacy() { }

    /** Cardinals needed before a family has any grip on the papacy. */
    private static final int CARDINALS_FOR_INFLUENCE = 3;
    /** Cardinals at which control is effectively certain even without the POPE office. */
    private static final int CARDINALS_FOR_CERTAIN_CONTROL = 7;
    /** Marginal control probability contributed per cardinal past the influence floor. */
    private static final double CONTROL_CHANCE_PER_CARDINAL = 0.18;

    /**
     * Probability (0..1) that {@code f} currently controls the College of Cardinals
     * and thus the papacy. Holding the POPE office is outright control.
     */
    public static double controlChance(Family f) {
        if (f.hasOffice(Office.POPE)) return 1.0;
        int over = f.cardinalsBribed - CARDINALS_FOR_INFLUENCE;
        if (over < 0) return 0.0;
        double p = CONTROL_CHANCE_PER_CARDINAL * (over + 1);
        return Math.min(1.0, p);
    }

    /** True if the family plainly dominates the College (used by AI/UI gating). */
    public static boolean dominatesCollege(Family f) {
        return f.hasOffice(Office.POPE) || f.cardinalsBribed >= CARDINALS_FOR_CERTAIN_CONTROL;
    }

    /**
     * Attempt, via influenced cardinals, to seize control of the papacy this year.
     * On success the family is recognised by being granted the POPE office (the only
     * persistent papacy marker in the model), which also counts toward net worth.
     * Requires the ecclesiastical prerequisite of several bribed cardinals.
     */
    public static boolean tryControlPapacy(GameState s, Family f, Rng rng) {
        if (f.hasOffice(Office.POPE)) return true;
        if (f.cardinalsBribed < CARDINALS_FOR_INFLUENCE) return false;
        if (rng.chance(controlChance(f))) {
            f.offices.add(Office.POPE);
            s.logEvent(new Event(EventType.INTERDICT, s.year, -1,
                    "House " + f.surname + " now controls the papacy."));
            return true;
        }
        return false;
    }

    /**
     * A family controlling the papacy imposes an interdict on Venice (a spiritual
     * weapon against rivals). Logged as a world event; there is no persistent flag
     * to toggle in the current model.
     */
    public static String imposeInterdict(GameState s, Family f) {
        if (!dominatesCollege(f)) return "House " + f.surname + " does not control the papacy.";
        s.logEvent(new Event(EventType.INTERDICT, s.year, -1,
                "House " + f.surname + " has the Pope lay an interdict upon their rivals."));
        return "An interdict is proclaimed at House " + f.surname + "'s bidding.";
    }

    /** A controlling family may instead lift an interdict, restoring trade/goodwill. */
    public static String liftInterdict(GameState s, Family f) {
        if (!dominatesCollege(f)) return "House " + f.surname + " does not control the papacy.";
        s.logEvent(new Event(EventType.INTERDICT, s.year, -1,
                "House " + f.surname + " persuades the Pope to lift the interdict."));
        return "The interdict is lifted through House " + f.surname + "'s influence.";
    }

    /**
     * Derived check: was an interdict the most recent papal event still "in effect"
     * this year? Best-effort until a persistent flag exists in the model.
     */
    public static boolean interdictInEffect(GameState s) {
        for (int i = s.log.size() - 1; i >= 0; i--) {
            Event e = s.log.get(i);
            if (e.type != EventType.INTERDICT) continue;
            return e.message.contains("interdict") && !e.message.contains("lift");
        }
        return false;
    }
}

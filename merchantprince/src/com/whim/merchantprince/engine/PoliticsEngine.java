package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Office;

/**
 * Venetian politics, the Church, and dirty tricks (GAME_DESIGN_REFERENCE §6):
 * bribing the Council of Ten for offices, ascending to Doge, influencing cardinals
 * toward control of the papacy, and running a "den of iniquities" for arson, rumour
 * and assassination (with reputation risk if caught).
 *
 * <p>Contract frozen for T0. Full cost tables, papacy control and dirty-trick
 * resolution to be completed by the Politics task (T2).
 */
public final class PoliticsEngine {
    private PoliticsEngine() { }

    /** Attempt to buy a state or Church office by bribery. Returns true on success. */
    public static boolean buyOffice(GameState s, Family f, Office o) {
        if (f.hasOffice(o) || f.florins < o.bribeCost) return false;
        f.florins -= o.bribeCost;
        f.offices.add(o);
        return true;
    }

    /** Bribe one senator (Council of Ten member) into the family's pocket. */
    public static boolean bribeSenator(GameState s, Family f) {
        long cost = 800L + (long) f.senatorsBribed * 400;
        if (f.florins < cost) return false;
        f.florins -= cost;
        f.senatorsBribed++;
        return true;
    }

    /** Influence one cardinal toward the family (papacy track). */
    public static boolean bribeCardinal(GameState s, Family f) {
        long cost = 1500L + (long) f.cardinalsBribed * 700;
        if (f.florins < cost) return false;
        f.florins -= cost;
        f.cardinalsBribed++;
        return true;
    }

    /** Build the family's den of iniquities, unlocking dirty tricks. */
    public static boolean buildDen(GameState s, Family f) {
        if (f.denOfIniquities || f.florins < Constants.DEN_COST) return false;
        f.florins -= Constants.DEN_COST;
        f.denOfIniquities = true;
        return true;
    }

    /**
     * Run a dirty trick (arson / rumour / assassination) against a rival.
     * Returns a human-readable outcome string.
     */
    public static String dirtyTrick(GameState s, Family f, Family target, String kind, Rng rng) {
        // TODO(T2): apply concrete effects per kind (destroy cargo, sink a ship,
        // wound reputation, remove an office/senator) and elimination on assassination.
        if (!f.denOfIniquities) return "You have no den of iniquities.";
        if (rng.chance(Constants.DIRTY_TRICK_CAUGHT_CHANCE)) {
            f.reputation = Math.max(0, f.reputation - Constants.DIRTY_TRICK_REPUTATION_HIT);
            return "Your " + kind + " against House " + target.surname + " was discovered!";
        }
        return "Your " + kind + " against House " + target.surname + " succeeded.";
    }
}

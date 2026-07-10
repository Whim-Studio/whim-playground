package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.model.TransportUnit;

/**
 * Venetian politics, the Church, and dirty tricks (GAME_DESIGN_REFERENCE §6):
 * bribing the Council of Ten for offices, ascending to Doge, influencing cardinals
 * toward control of the papacy, and running a "den of iniquities" for arson, rumour
 * and assassination (with reputation risk if caught).
 *
 * <p>Implemented by the Politics task (T2). Method signatures are frozen from T0;
 * only their bodies are filled in here. Every tunable is a {@code private static
 * final} in this file so the frozen {@link Constants} stays untouched.
 */
public final class PoliticsEngine {
    private PoliticsEngine() { }

    // ---- Bribery cost scaling (ASSUMPTION — exact original tables unconfirmed) ----
    /**
     * Each office/senator/cardinal a family already holds makes the Council of Ten
     * greedier: the marked-up cost grows by this fraction per prior political asset
     * (offices + bribed senators + bribed cardinals). Keeps a runaway leader paying
     * ever more to consolidate power.
     */
    private static final double INFLUENCE_MARKUP_PER_ASSET = 0.20;

    // ---- Office prerequisites (ASSUMPTION) -----------------------------
    /** The Doge is only offered to a family with real standing in the Republic. */
    private static final int DOGE_MIN_STATE_OFFICES = 2;
    /** ...or one that has packed the Council of Ten with enough bribed senators. */
    private static final int DOGE_MIN_SENATORS = 5;
    /** The Pope requires a bloc of cardinals already in the family's pocket. */
    private static final int POPE_MIN_CARDINALS = 4;

    // ---- Dirty-trick effect magnitudes (ASSUMPTION) --------------------
    /** Rumour: reputation points knocked off the victim. */
    private static final int RUMOUR_REPUTATION_HIT = 12;
    /** Assassination: base chance the target family is actually eliminated. */
    private static final double ASSASSINATION_KILL_CHANCE = 0.45;
    /** Fraction of a slain family's florins looted by the perpetrator (rest is voided). */
    private static final double ASSASSINATION_LOOT_FRACTION = 0.25;

    // ---- Base costs for actions the model doesn't already price ---------
    private static final long ARSON_BASE_COST = 1500L;
    private static final long RUMOUR_BASE_COST = 800L;
    private static final long ASSASSINATION_BASE_COST = 6000L;

    /** Count of political assets that inflate future bribery costs. */
    private static int politicalAssets(Family f) {
        return f.offices.size() + f.senatorsBribed + f.cardinalsBribed;
    }

    /** Apply the "you already hold power, pay more" markup to a base cost. */
    private static long markedUp(Family f, long base) {
        double factor = 1.0 + INFLUENCE_MARKUP_PER_ASSET * politicalAssets(f);
        return Math.round(base * factor);
    }

    /**
     * True if a family meets the political prerequisites to even be offered an
     * office. DOGE demands prior state offices or a packed Council; POPE demands a
     * bloc of bribed cardinals. Other offices are open to anyone who can pay.
     */
    public static boolean eligibleFor(Family f, Office o) {
        if (f.hasOffice(o)) return false;
        switch (o) {
            case DOGE:
                return stateOffices(f) >= DOGE_MIN_STATE_OFFICES
                        || f.senatorsBribed >= DOGE_MIN_SENATORS;
            case POPE:
                return f.cardinalsBribed >= POPE_MIN_CARDINALS;
            default:
                return true;
        }
    }

    /** Number of secular Venetian offices held (everything but CARDINAL/POPE). */
    private static int stateOffices(Family f) {
        int n = 0;
        for (Office o : f.offices) {
            if (o != Office.CARDINAL && o != Office.POPE) n++;
        }
        return n;
    }

    /** The current bribe a family must pay to acquire an office (with markup). */
    public static long officeCost(Family f, Office o) {
        return markedUp(f, o.bribeCost);
    }

    /**
     * Attempt to buy a state or Church office by bribery. Fails if already held, if
     * the family is not yet eligible (DOGE/POPE prerequisites), or if it cannot
     * afford the marked-up bribe. Returns true on success.
     */
    public static boolean buyOffice(GameState s, Family f, Office o) {
        if (!eligibleFor(f, o)) return false;
        long cost = officeCost(f, o);
        if (f.florins < cost) return false;
        f.florins -= cost;
        f.offices.add(o);
        return true;
    }

    /** Current cost to bribe the next senator (grows with prior political power). */
    public static long senatorCost(Family f) {
        return markedUp(f, 800L + (long) f.senatorsBribed * 400L);
    }

    /** Bribe one senator (Council of Ten member) into the family's pocket. */
    public static boolean bribeSenator(GameState s, Family f) {
        long cost = senatorCost(f);
        if (f.florins < cost) return false;
        f.florins -= cost;
        f.senatorsBribed++;
        return true;
    }

    /** Current cost to influence the next cardinal (grows with prior power). */
    public static long cardinalCost(Family f) {
        return markedUp(f, 1500L + (long) f.cardinalsBribed * 700L);
    }

    /** Influence one cardinal toward the family (papacy track). */
    public static boolean bribeCardinal(GameState s, Family f) {
        long cost = cardinalCost(f);
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

    // ---- Dirty tricks (§6) ---------------------------------------------

    /** Canonical dirty-trick kinds. Passed as strings to keep the frozen signature. */
    public static final String ARSON = "arson";
    public static final String RUMOUR = "rumour";
    public static final String ASSASSINATION = "assassination";

    /** Florin cost of mounting a given dirty trick (before success/failure). */
    public static long trickCost(String kind) {
        if (ARSON.equals(kind)) return ARSON_BASE_COST;
        if (RUMOUR.equals(kind)) return RUMOUR_BASE_COST;
        if (ASSASSINATION.equals(kind)) return ASSASSINATION_BASE_COST;
        return 0L;
    }

    /**
     * Run a dirty trick (arson / rumour / assassination) against a rival, gated on
     * owning a den of iniquities. The perpetrator pays the operation's cost up front.
     * There is a {@link Constants#DIRTY_TRICK_CAUGHT_CHANCE} chance of being caught,
     * which wounds reputation and aborts the effect; otherwise the concrete effect is
     * applied per kind. Returns a human-readable outcome string.
     *
     * <ul>
     *   <li><b>Arson</b> — burn the rival's cargo sitting in docked (idle) units,
     *       the closest analogue to torching their warehouse stock.</li>
     *   <li><b>Rumour</b> — spread slander, dropping the rival's reputation.</li>
     *   <li><b>Assassination</b> — a chance to wipe out the rival family entirely,
     *       looting a slice of its florins and voiding the rest.</li>
     * </ul>
     */
    public static String dirtyTrick(GameState s, Family f, Family target, String kind, Rng rng) {
        if (!f.denOfIniquities) return "You have no den of iniquities.";
        if (target == null || target.eliminated || target.id == f.id) {
            return "There is no such rival to strike.";
        }
        long cost = trickCost(kind);
        if (f.florins < cost) {
            return "You cannot afford to arrange a " + kind + ".";
        }
        f.florins -= cost;

        // Being caught ruins reputation and aborts the plot (stub behaviour retained).
        if (rng.chance(Constants.DIRTY_TRICK_CAUGHT_CHANCE)) {
            f.reputation = Math.max(0, f.reputation - Constants.DIRTY_TRICK_REPUTATION_HIT);
            return "Your " + kind + " against House " + target.surname + " was discovered! "
                    + "Your reputation suffers.";
        }

        if (ARSON.equals(kind)) {
            return commitArson(s, f, target);
        } else if (RUMOUR.equals(kind)) {
            return commitRumour(target);
        } else if (ASSASSINATION.equals(kind)) {
            return commitAssassination(s, f, target, rng);
        }
        // Unknown kind: treat as a failed, harmless plot but keep the money spent.
        return "Your agents bungle the " + kind + " against House " + target.surname + ".";
    }

    /** Burn the value out of a rival's docked cargo (its exposed "warehouse"). */
    private static String commitArson(GameState s, Family f, Family target) {
        long destroyed = 0;
        int holds = 0;
        for (TransportUnit u : s.unitsOf(target.id)) {
            if (u.inTransit()) continue; // only cargo sitting in port can be torched
            for (Good g : Good.ALL) {
                int q = u.cargo[g.ordinal()];
                if (q > 0) {
                    destroyed += (long) q * g.nominalValue;
                    u.cargo[g.ordinal()] = 0;
                    holds++;
                }
            }
        }
        if (destroyed == 0) {
            return "Your arsonists find nothing of House " + target.surname
                    + "'s worth burning in port.";
        }
        return "Arson! House " + target.surname + " loses cargo worth ~" + destroyed
                + " florins across " + holds + " hold(s).";
    }

    /** Slander a rival, dropping their reputation. */
    private static String commitRumour(Family target) {
        int before = target.reputation;
        target.reputation = Math.max(0, target.reputation - RUMOUR_REPUTATION_HIT);
        return "Rumours ruin House " + target.surname + "'s good name (reputation "
                + before + " -> " + target.reputation + ").";
    }

    /**
     * Attempt to eliminate a rival family. On success the family is flagged
     * eliminated, a slice of its florins is looted by the perpetrator and the rest is
     * voided; its units are cast adrift (ownership left untouched but the family is
     * out of the running). Net worth then excludes an eliminated family from scoring.
     */
    private static String commitAssassination(GameState s, Family f, Family target, Rng rng) {
        if (!rng.chance(ASSASSINATION_KILL_CHANCE)) {
            return "Your assassin fails to reach the head of House " + target.surname + ".";
        }
        long loot = Math.round(target.florins * ASSASSINATION_LOOT_FRACTION);
        f.florins += loot;
        target.florins = 0;
        target.eliminated = true;
        // Sever the family's political assets — dead houses hold no offices.
        target.offices.clear();
        target.senatorsBribed = 0;
        target.cardinalsBribed = 0;
        return "Assassination! House " + target.surname + " is wiped out; House "
                + f.surname + " loots " + loot + " florins.";
    }
}

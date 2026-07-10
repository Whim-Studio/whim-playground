package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.model.UnitType;
import com.whim.merchantprince.model.event.Event;
import com.whim.merchantprince.model.event.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Movement and per-leg hazards (GAME_DESIGN_REFERENCE §2, §4, §5). Travel time is
 * derived from map distance and unit speed; each turn spent on a <em>sea</em> leg
 * risks a storm (galleys more than cogs, per {@link UnitType#stormRisk}) and piracy.
 * Automated routes cycle a unit through a city list.
 *
 * <h3>Land vs sea legs</h3>
 * The model has no explicit route graph, so a leg's medium is inferred from its
 * endpoints (kept deliberately simple and documented): a leg between two
 * {@link City#coastal} cities is a <b>sea leg</b>; any leg touching an inland city is
 * an <b>overland leg</b>. Sea-only units (galleys, cogs) may not run overland legs
 * and land-only units (donkey teams, camel caravans) may not run sea legs — see
 * {@link #canTraverse}. Storms and piracy only ever strike sea legs.
 */
public final class TravelEngine {
    private TravelEngine() { }

    // ---- Hazard tuning (ASSUMPTION — tunable) --------------------------
    /** Fraction of each cargo good lost when a storm damages a unit. */
    private static final double STORM_CARGO_LOSS = 0.25;
    /** Chance a storm is severe enough to sink the unit outright. */
    private static final double STORM_SINK_CHANCE = 0.03;
    /** Base per-sea-leg-turn chance of a pirate encounter. */
    private static final double PIRACY_CHANCE = 0.09;
    /** Fraction of each cargo good plundered in a pirate raid. */
    private static final double PIRACY_CARGO_LOSS = 0.35;
    /** Chance a pirate raid sinks the unit rather than merely looting it. */
    private static final double PIRACY_SINK_CHANCE = 0.02;

    /** Whole turns for a unit to travel between two cities (minimum 1). */
    public static int turnsBetween(City a, City b, UnitType type) {
        double dist = a.distanceTo(b);
        double perTurn = Math.max(1.0, type.speed * Constants.DISTANCE_PER_SPEED);
        return Math.max(1, (int) Math.ceil(dist / perTurn));
    }

    /**
     * True if a leg between two coastal cities counts as a sea leg. Overland legs
     * (any leg touching an inland city) are false. Used both to gate unit capability
     * and to decide whether storm/piracy hazards apply.
     */
    public static boolean isSeaLeg(City a, City b) {
        return a.coastal && b.coastal;
    }

    /**
     * Whether a unit of the given type may traverse the leg between two cities:
     * sea-capable units on sea legs, land-capable units on overland legs.
     */
    public static boolean canTraverse(City a, City b, UnitType type) {
        return isSeaLeg(a, b) ? type.sea : type.land;
    }

    /** Send a docked unit toward a destination city. */
    public static void dispatch(GameState s, TransportUnit u, City dest) {
        City from = s.city(u.locationCityId);
        u.destinationCityId = dest.id;
        u.turnsRemaining = turnsBetween(from, dest, u.type);
    }

    /**
     * Advance every in-transit unit by one turn, resolving arrivals, sea hazards, and
     * automated-route continuation. Units sunk by storm or piracy are removed from the
     * game after the pass (so we never mutate the list mid-iteration).
     */
    public static void advanceUnits(GameState s, Rng rng) {
        List<TransportUnit> sunk = new ArrayList<TransportUnit>();

        for (TransportUnit u : s.units) {
            if (!u.inTransit()) continue;

            City from = s.city(u.locationCityId);
            City to = s.city(u.destinationCityId);

            // Sea legs expose the unit to storms and pirates for this turn of travel.
            if (from != null && to != null && isSeaLeg(from, to) && u.type.sea) {
                if (rollStorm(s, u, rng)) { sunk.add(u); continue; }
                if (rollPiracy(s, u, rng)) { sunk.add(u); continue; }
            }

            // Advance one turn; dock (and possibly continue an auto-route) on arrival.
            u.turnsRemaining--;
            if (u.turnsRemaining <= 0) {
                u.locationCityId = u.destinationCityId;
                u.destinationCityId = -1;
                u.turnsRemaining = 0;
                if (u.autoRoute) continueRoute(s, u);
            }
        }

        s.units.removeAll(sunk);
    }

    /**
     * Roll a storm for one sea-leg turn. Storm-prone galleys risk it more than sturdy
     * cogs (via {@link UnitType#stormRisk}). A storm usually spoils some cargo; rarely
     * it sinks the unit. Returns true if the unit was lost.
     */
    private static boolean rollStorm(GameState s, TransportUnit u, Rng rng) {
        if (!rng.chance(u.type.stormRisk)) return false;
        if (rng.chance(STORM_SINK_CHANCE)) {
            s.logEvent(new Event(EventType.STORM, s.year, u.locationCityId,
                    u.displayName() + " is lost with all cargo in a storm at sea."));
            return true;
        }
        damageCargo(u, STORM_CARGO_LOSS);
        s.logEvent(new Event(EventType.STORM, s.year, u.locationCityId,
                u.displayName() + " is battered by a storm and sheds cargo."));
        return false;
    }

    /**
     * Roll a pirate encounter for one sea-leg turn: usually a portion of cargo is
     * plundered; rarely the unit is taken/sunk. Returns true if the unit was lost.
     */
    private static boolean rollPiracy(GameState s, TransportUnit u, Rng rng) {
        if (!rng.chance(PIRACY_CHANCE)) return false;
        if (rng.chance(PIRACY_SINK_CHANCE)) {
            s.logEvent(new Event(EventType.PIRACY, s.year, u.locationCityId,
                    u.displayName() + " is seized and sunk by corsairs."));
            return true;
        }
        damageCargo(u, PIRACY_CARGO_LOSS);
        s.logEvent(new Event(EventType.PIRACY, s.year, u.locationCityId,
                u.displayName() + " is boarded by pirates and plundered."));
        return false;
    }

    /** Remove a fraction (rounded up) of every good the unit carries. */
    private static void damageCargo(TransportUnit u, double fraction) {
        for (Good g : Good.ALL) {
            int held = u.cargo[g.ordinal()];
            if (held <= 0) continue;
            int lost = (int) Math.ceil(held * fraction);
            u.cargo[g.ordinal()] = Math.max(0, held - lost);
        }
    }

    /**
     * Continue an automated route: dispatch the freshly docked unit toward the next
     * distinct city on its loop. We advance from wherever the unit actually is (not a
     * cached index, which can drift out of sync) and skip any stop equal to the
     * current location, so a unit cannot get stuck re-"arriving" where it already
     * sits. No-op for a route of fewer than two cities.
     */
    private static void continueRoute(GameState s, TransportUnit u) {
        int size = u.route.size();
        if (size < 2) return;
        // Find where we are in the loop; default to the current index if not listed.
        int here = u.routeIndex;
        for (int i = 0; i < size; i++) {
            if (u.route.get(i) == u.locationCityId) { here = i; break; }
        }
        // Advance to the next stop that isn't the current city (guards degenerate loops).
        for (int step = 1; step <= size; step++) {
            int idx = (here + step) % size;
            City next = s.city(u.route.get(idx));
            if (next != null && next.id != u.locationCityId) {
                u.routeIndex = idx;
                dispatch(s, u, next);
                return;
            }
        }
    }
}

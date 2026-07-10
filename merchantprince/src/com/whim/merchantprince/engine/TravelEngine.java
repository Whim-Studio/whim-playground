package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.model.UnitType;

/**
 * Movement and per-leg hazards (GAME_DESIGN_REFERENCE §2, §4, §5). Travel time is
 * derived from map distance and unit speed; each sea leg risks a storm (galleys
 * more than cogs) and piracy. Automated routes cycle a unit through a city list.
 *
 * <p>Contract frozen for T0. Full hazard resolution to be completed by the Economy task (T1).
 */
public final class TravelEngine {
    private TravelEngine() { }

    /** Whole turns for a unit to travel between two cities (minimum 1). */
    public static int turnsBetween(City a, City b, UnitType type) {
        double dist = a.distanceTo(b);
        double perTurn = Math.max(1.0, type.speed * Constants.DISTANCE_PER_SPEED);
        return Math.max(1, (int) Math.ceil(dist / perTurn));
    }

    /** Send a docked unit toward a destination city. */
    public static void dispatch(GameState s, TransportUnit u, City dest) {
        City from = s.city(u.locationCityId);
        u.destinationCityId = dest.id;
        u.turnsRemaining = turnsBetween(from, dest, u.type);
    }

    /**
     * Advance every in-transit unit by one turn, resolving arrivals, hazards, and
     * automated-route continuation.
     */
    public static void advanceUnits(GameState s, Rng rng) {
        // TODO(T1): roll storm (UnitType.stormRisk on sea legs) and piracy per turn;
        // on arrival, dock the unit and, if autoRoute, dispatch to the next route city.
        for (TransportUnit u : s.units) {
            if (!u.inTransit()) continue;
            u.turnsRemaining--;
            if (u.turnsRemaining <= 0) {
                u.locationCityId = u.destinationCityId;
                u.destinationCityId = -1;
                u.turnsRemaining = 0;
            }
        }
    }
}

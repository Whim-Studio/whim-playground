package com.railroad.model;

/**
 * Central, tunable economic constants and the demand-based revenue formula for
 * Phase 2. Keeping every balance number in one place means the numbers below are
 * the only knobs to turn — none are meant to reproduce the 1990 game's exact
 * economy, only to feel like it (bulk cargo cheap, premium cargo dear; longer
 * hauls pay more).
 *
 * <h3>Revenue formula</h3>
 * <pre>
 *   revenue(cargo, distanceTiles) = round(cargo.baseValue * distanceTiles * DEMAND_FACTOR)
 * </pre>
 * where {@code distanceTiles} is the number of track hops the carload travelled
 * from the station it was loaded at to the station it is delivered to. Revenue is
 * only ever booked when the destination station actually demands that cargo type
 * (see {@link Station#demands}); undelivered or undemanded carloads earn nothing.
 */
public final class Economy {

    private Economy() {
    }

    // --- Revenue --------------------------------------------------------------

    /** Global multiplier on all delivery revenue; the master difficulty knob. */
    public static final double DEMAND_FACTOR = 1.0;

    /** Cost, in company cash, to build one {@link Station}. */
    public static final long STATION_COST = 25_000L;

    /** Chebyshev radius (in tiles) of a station's catchment area. */
    public static final int STATION_RADIUS = 2;

    // --- Production rates (carloads accrued per in-game day) -------------------

    public static final double TOWN_PASSENGER_RATE = 0.45;
    public static final double TOWN_MAIL_RATE = 0.30;
    public static final double TOWN_SUPPLY_CAP = 12.0;

    public static final double MINE_COAL_RATE = 0.60;
    public static final double MINE_COAL_CAP = 16.0;

    /** Coal consumed (and steel produced) per day by a mill that has coal on hand. */
    public static final double MILL_CONVERT_RATE = 0.50;
    public static final double MILL_STEEL_CAP = 12.0;
    public static final double MILL_COAL_INPUT_CAP = 20.0;

    /**
     * Revenue for delivering one carload of {@code type} that was carried
     * {@code distanceTiles} track hops to a station that demands it.
     */
    public static long deliveryRevenue(CargoType type, int distanceTiles) {
        if (distanceTiles < 1) {
            distanceTiles = 1;
        }
        return Math.round(type.getBaseValue() * (double) distanceTiles * DEMAND_FACTOR);
    }
}

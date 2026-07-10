package com.whim.merchantprince.model;

/**
 * Transport units confirmed for Merchant Prince (1994): galleys (fast but
 * storm-vulnerable), cogs (slow but storm-resistant), and overland donkey teams
 * and camel caravans (GAME_DESIGN_REFERENCE §4). Up to 15 may travel grouped.
 *
 * <p>Values here are the tunable knobs the travel/economy engines read:
 * {@code speed} in map-units per turn, {@code capacity} in cargo units, and
 * {@code stormRisk} the per-sea-leg base hazard chance. {@code sea}/{@code land}
 * gate which legs a unit may traverse.
 */
public enum UnitType {
    SMALL_GALLEY("Small Galley", 5, 60,  0.18, true,  false, 1200),
    LARGE_GALLEY("Large Galley", 4, 140, 0.22, true,  false, 3000),
    SMALL_COG   ("Small Cog",    3, 90,  0.06, true,  false, 1500),
    LARGE_COG   ("Large Cog",    2, 200, 0.08, true,  false, 3600),
    DONKEY_TEAM ("Donkey Team",  2, 40,  0.05, false, true,   500),
    CAMEL_CARAVAN("Camel Caravan",2, 110, 0.07, false, true,  1400);

    public static final UnitType[] ALL = values();

    public final String label;
    public final int speed;
    public final int capacity;
    public final double stormRisk;
    public final boolean sea;
    public final boolean land;
    public final int cost;

    UnitType(String label, int speed, int capacity, double stormRisk,
             boolean sea, boolean land, int cost) {
        this.label = label;
        this.speed = speed;
        this.capacity = capacity;
        this.stormRisk = stormRisk;
        this.sea = sea;
        this.land = land;
        this.cost = cost;
    }
}

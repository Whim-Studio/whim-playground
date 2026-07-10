package com.whim.merchantprince.engine;

/**
 * All tunable game numbers live here, never as magic literals in logic
 * (per the project quality bar). Where a value fills a mechanic that research
 * could not confirm exactly, it is an internally-consistent ASSUMPTION and is
 * marked as such — the point is that these can be retuned in one place.
 */
public final class Constants {
    private Constants() { }

    // ---- Game framing --------------------------------------------------
    public static final int START_YEAR = 1300;
    public static final int DEFAULT_END_YEAR = 1360;     // 60-year game (a confirmed length)
    public static final long STARTING_FLORINS = 5000;
    public static final long WIN_FLORINS = 1_000_000;    // confirmed instant-win threshold (§7)
    public static final int NUM_FAMILIES = 4;            // confirmed: four competing families

    // ---- Pricing model (ASSUMPTION — exact original formula unconfirmed) ----
    /** Reference stock at which a good trades at its local base price. */
    public static final int REFERENCE_STOCK = 100;
    /** Price elasticity: how strongly price rises as stock falls toward zero. */
    public static final double PRICE_ELASTICITY = 0.6;
    /** Merchant's cut: sell price is this fraction of buy price at equal stock. */
    public static final double SELL_SPREAD = 0.85;
    /** Yearly mean-reversion of local base price toward the good's nominal value. */
    public static final double PRICE_DRIFT = 0.15;
    /** Yearly random shock applied to each local base price. */
    public static final double PRICE_SHOCK = 0.10;
    /** Units of a good bought/sold before local stock & price meaningfully move. */
    public static final int TRADE_STOCK_STEP = 1;

    // ---- Travel --------------------------------------------------------
    /** Map distance covered per unit of a unit's speed each turn. */
    public static final double DISTANCE_PER_SPEED = 30.0;

    // ---- Events (ASSUMPTION — timing/odds tuned, not lifted from original) ----
    public static final double PLAGUE_YEARLY_CHANCE = 0.06;
    public static final double WAR_YEARLY_CHANCE = 0.05;
    public static final double INTERDICT_YEARLY_CHANCE = 0.04;
    public static final int REFORMATION_EARLIEST_YEAR = 1517; // historical anchor

    // ---- Dirty tricks (§6) ---------------------------------------------
    /** Chance a dirty trick is discovered, costing reputation. */
    public static final double DIRTY_TRICK_CAUGHT_CHANCE = 0.35;
    public static final int DIRTY_TRICK_REPUTATION_HIT = 15;
    public static final int DEN_COST = 4000;
}

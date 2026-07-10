package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;

/**
 * Supply/demand pricing (GAME_DESIGN_REFERENCE §3). The exact 1994 formula is
 * unconfirmed, so this is an internally-consistent, tunable model:
 *
 * <ul>
 *   <li><b>Live price</b> ({@link #buyPrice}/{@link #sellPrice}) scales inversely
 *       with local stock around {@link Constants#REFERENCE_STOCK}: a good grows dear
 *       as its local stock is bought down, and cheap when a city is glutted. Sell
 *       price is the buy price less the merchant's spread.</li>
 *   <li><b>Yearly drift</b> ({@link #driftPrices}) mean-reverts each city's slow base
 *       price toward the good's nominal value — biased cheap where produced and dear
 *       where merely demanded — adds a bounded random shock, then folds in any active
 *       world-event disturbance for that year (plague/war spikes, interdict slumps)
 *       via {@link EventEngine#priceMultiplier}. Local stock also drifts back toward a
 *       natural level so markets recover between visits.</li>
 * </ul>
 */
public final class PricingEngine {
    private PricingEngine() { }

    // ---- Stock regeneration (ASSUMPTION — tunable) ---------------------
    /** Natural stock a producing city trends toward (glut → cheap to buy). */
    private static final int PRODUCER_TARGET_STOCK = 200;
    /** Natural stock a demanding city trends toward (scarce → dear). */
    private static final int CONSUMER_TARGET_STOCK = 40;
    /** Share of the gap to the natural level a market closes each year. */
    private static final double STOCK_REGEN_RATE = 0.25;
    /** Price bias factors used by yearly drift (cheap where made, dear where wanted). */
    private static final double PRODUCED_TARGET_MULT = 0.6;
    private static final double DEMANDED_TARGET_MULT = 1.4;

    /** Price a trader pays to BUY one unit of a good at a city. */
    public static long buyPrice(City c, Good g) {
        double base = c.basePrice[g.ordinal()];
        if (base <= 0) base = g.nominalValue;
        int stock = Math.max(1, c.stock[g.ordinal()]);
        double factor = Math.pow((double) Constants.REFERENCE_STOCK / stock, Constants.PRICE_ELASTICITY);
        return Math.max(1, Math.round(base * factor));
    }

    /** Price a trader receives to SELL one unit of a good at a city. */
    public static long sellPrice(City c, Good g) {
        return Math.max(1, Math.round(buyPrice(c, g) * Constants.SELL_SPREAD));
    }

    /**
     * Yearly drift of every city's base prices toward nominal, plus a bounded shock,
     * plus this year's active world-event disturbances. Also nudges local stock back
     * toward its natural level so buy/sell prices (which are derived from stock)
     * recover organically between trader visits.
     */
    public static void driftPrices(GameState s, Rng rng) {
        for (City c : s.cities) {
            for (Good g : Good.ALL) {
                int gi = g.ordinal();
                boolean makes = c.produces[gi];

                // 1. Mean-revert the slow base price toward the good's nominal value,
                //    biased cheap where produced and dear where merely demanded.
                double base = c.basePrice[gi];
                if (base <= 0) base = g.nominalValue;
                double target = g.nominalValue * (makes ? PRODUCED_TARGET_MULT : DEMANDED_TARGET_MULT);
                base += (target - base) * Constants.PRICE_DRIFT;

                // 2. Apply a bounded random shock in [-PRICE_SHOCK, +PRICE_SHOCK].
                base *= 1.0 + (rng.nextDouble() * 2 - 1) * Constants.PRICE_SHOCK;

                // 3. Fold in any active world event affecting this city/good this year
                //    (plague/war spike, interdict/reformation slump). 1.0 when none.
                base *= EventEngine.priceMultiplier(c.id, g);

                c.basePrice[gi] = Math.max(1, base);

                // 4. Regenerate local stock toward its natural level so supply/demand
                //    (and hence buy/sell prices) recover between visits.
                int naturalStock = makes ? PRODUCER_TARGET_STOCK : CONSUMER_TARGET_STOCK;
                int stock = c.stock[gi];
                stock += (int) Math.round((naturalStock - stock) * STOCK_REGEN_RATE);
                c.stock[gi] = Math.max(0, stock);
            }
        }
    }
}

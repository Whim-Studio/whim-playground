package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;

/**
 * Supply/demand pricing (GAME_DESIGN_REFERENCE §3). The exact 1994 formula is
 * unconfirmed, so this is an internally-consistent, tunable model: local price
 * scales inversely with local stock around {@link Constants#REFERENCE_STOCK}, and
 * base prices mean-revert with a yearly shock.
 *
 * <p>Contract frozen for T0. Implementation to be completed by the Economy task (T1).
 */
public final class PricingEngine {
    private PricingEngine() { }

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

    /** Yearly drift of every city's base prices toward nominal, plus a shock. */
    public static void driftPrices(GameState s, Rng rng) {
        // TODO(T1): mean-revert basePrice toward Good.nominalValue by PRICE_DRIFT,
        // apply +/- PRICE_SHOCK, and let plague/war events multiply affected goods.
        for (City c : s.cities) {
            for (Good g : Good.ALL) {
                double base = c.basePrice[g.ordinal()];
                if (base <= 0) base = g.nominalValue;
                double target = g.nominalValue * (c.produces[g.ordinal()] ? 0.6 : 1.4);
                base += (target - base) * Constants.PRICE_DRIFT;
                base *= 1.0 + (rng.nextDouble() * 2 - 1) * Constants.PRICE_SHOCK;
                c.basePrice[g.ordinal()] = Math.max(1, base);
            }
        }
    }
}

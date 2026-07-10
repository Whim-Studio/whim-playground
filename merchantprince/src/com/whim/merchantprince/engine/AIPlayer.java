package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.model.TransportUnit;

import java.util.List;

/**
 * Heuristic AI for the rival families (GAME_DESIGN_REFERENCE §1). Each rival takes a
 * turn: sell whatever cargo it is sitting on, send idle units on the most profitable
 * buy-low/sell-high run it can reach, occasionally invest surplus florins in offices
 * and bribery, and (rarely) run a dirty trick against another rival if it owns a den.
 *
 * <p>Implemented by the Politics task (T2). Deliberately lightweight —
 * O(units × cities × goods) per family — and it never touches the human family's
 * units or trading decisions.
 */
public final class AIPlayer {
    private AIPlayer() { }

    // ---- Trading thresholds (ASSUMPTION — tuned, not from the original) ----
    /** A city needs at least this much stock of a good before the AI will buy it. */
    private static final int MIN_STOCK_TO_BUY = 20;
    /** Minimum per-unit profit that makes a run worth dispatching. */
    private static final long MIN_PROFIT_PER_UNIT = 5;

    // ---- Politics thresholds (ASSUMPTION) ------------------------------
    /** Keep this much working capital in reserve before spending on politics. */
    private static final long POLITICS_RESERVE = 8000L;
    /** Per-turn chance a cash-rich rival spends on offices/bribery. */
    private static final double POLITICS_CHANCE = 0.35;
    /** Per-turn chance a rich rival builds a den of iniquities. */
    private static final double DEN_CHANCE = 0.15;
    /** Per-turn chance a den-owning rival attempts a dirty trick. */
    private static final double DIRTY_TRICK_CHANCE = 0.08;

    /** Run one rival family's turn. Never mutates the human family. */
    public static void takeTurn(GameState s, Family f, Rng rng) {
        if (f.human || f.eliminated) return;

        // 1. Trade & route every idle unit the family owns.
        for (TransportUnit u : s.unitsOf(f.id)) {
            if (u.inTransit()) continue;
            runUnit(s, f, u, rng);
        }

        // 2. Opportunistic politics once the family is comfortably liquid.
        investInPolitics(s, f, rng);

        // 3. Rarely, a den-owner sabotages a rival.
        maybeDirtyTrick(s, f, rng);
    }

    // ---- Trading -------------------------------------------------------

    /**
     * For a docked unit: liquidate its cargo at the current (open) city, then find the
     * single most profitable good to buy here and reachable city to sell it in, buy as
     * much as capacity and cash allow, and dispatch there. O(cities × goods).
     */
    private static void runUnit(GameState s, Family f, TransportUnit u, Rng rng) {
        City here = s.city(u.locationCityId);
        if (here == null) return;

        // Sell everything we are carrying, if we can trade here.
        if (here.open) sellAll(f, u, here);

        // We can only shop where the market is open.
        if (!here.open) {
            redirectToOpenMarket(s, u);
            return;
        }

        // Find the best reachable open city to sell each good into (one pass).
        long[] bestSellPrice = new long[Good.COUNT];
        City[] bestSellCity = new City[Good.COUNT];
        for (City there : s.cities) {
            if (there.id == here.id || !there.open || !reachable(u, there)) continue;
            for (Good g : Good.ALL) {
                long sp = PricingEngine.sellPrice(there, g);
                if (sp > bestSellPrice[g.ordinal()]) {
                    bestSellPrice[g.ordinal()] = sp;
                    bestSellCity[g.ordinal()] = there;
                }
            }
        }

        // Pick the good with the best per-unit profit buying here and selling there.
        Good bestGood = null;
        City bestDest = null;
        long bestProfit = MIN_PROFIT_PER_UNIT;
        for (Good g : Good.ALL) {
            if (here.stock[g.ordinal()] < MIN_STOCK_TO_BUY) continue;
            City dest = bestSellCity[g.ordinal()];
            if (dest == null) continue;
            long profit = bestSellPrice[g.ordinal()] - PricingEngine.buyPrice(here, g);
            if (profit > bestProfit) {
                bestProfit = profit;
                bestGood = g;
                bestDest = dest;
            }
        }

        if (bestGood == null || bestDest == null) return; // nothing worth doing

        // Buy as much as capacity and cash allow, then set sail.
        int bought = buy(f, u, here, bestGood);
        if (bought > 0) {
            TravelEngine.dispatch(s, u, bestDest);
        }
    }

    /** Sell all of a unit's cargo at the docked city, moving florins and stock. */
    private static void sellAll(Family f, TransportUnit u, City here) {
        for (Good g : Good.ALL) {
            int qty = u.cargo[g.ordinal()];
            if (qty <= 0) continue;
            long unit = PricingEngine.sellPrice(here, g);
            f.florins += unit * qty;
            here.stock[g.ordinal()] += qty;       // goods flow into the local market
            u.cargo[g.ordinal()] = 0;
        }
    }

    /**
     * Buy as many units of {@code g} as the hold and the purse permit, moving florins
     * and reducing local stock. Returns the quantity actually bought.
     */
    private static int buy(Family f, TransportUnit u, City here, Good g) {
        long price = PricingEngine.buyPrice(here, g);
        if (price <= 0) return 0;
        int byCash = (int) Math.min(Integer.MAX_VALUE, f.florins / price);
        int byHold = u.cargoFree();
        int byStock = here.stock[g.ordinal()];
        int qty = Math.min(byCash, Math.min(byHold, byStock));
        if (qty <= 0) return 0;
        f.florins -= price * qty;
        u.cargo[g.ordinal()] += qty;
        here.stock[g.ordinal()] -= qty;
        return qty;
    }

    /** Send a unit stuck at a closed city toward the nearest reachable open market. */
    private static void redirectToOpenMarket(GameState s, TransportUnit u) {
        City here = s.city(u.locationCityId);
        if (here == null) return;
        City best = null;
        double bestDist = Double.MAX_VALUE;
        for (City c : s.cities) {
            if (c.id == here.id || !c.open || !reachable(u, c)) continue;
            double d = here.distanceTo(c);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        if (best != null) TravelEngine.dispatch(s, u, best);
    }

    /** Whether a unit's transport class may travel to a given city. */
    private static boolean reachable(TransportUnit u, City c) {
        // Ships need a coastal city; overland units can reach any city.
        if (u.type.sea && !u.type.land) return c.coastal;
        return true;
    }

    // ---- Politics ------------------------------------------------------

    /**
     * With spare capital, a rival occasionally buys the cheapest eligible office it
     * can afford, or bribes a senator/cardinal — mirroring the human's political
     * options and growing its net worth.
     */
    private static void investInPolitics(GameState s, Family f, Rng rng) {
        if (f.florins <= POLITICS_RESERVE) return;

        // Occasionally build a den so dirty tricks become possible later.
        if (!f.denOfIniquities && rng.chance(DEN_CHANCE)) {
            PoliticsEngine.buildDen(s, f);
        }

        if (!rng.chance(POLITICS_CHANCE)) return;
        long spendable = f.florins - POLITICS_RESERVE;

        // Prefer the cheapest eligible, unheld office within budget.
        Office cheapest = null;
        long cheapestCost = Long.MAX_VALUE;
        for (Office o : Office.ALL) {
            if (!PoliticsEngine.eligibleFor(f, o)) continue;
            long cost = PoliticsEngine.officeCost(f, o);
            if (cost <= spendable && cost < cheapestCost) {
                cheapestCost = cost;
                cheapest = o;
            }
        }
        if (cheapest != null) {
            PoliticsEngine.buyOffice(s, f, cheapest);
            return;
        }

        // Otherwise pad the Council or the College, whichever is affordable/cheaper.
        long senator = PoliticsEngine.senatorCost(f);
        long cardinal = PoliticsEngine.cardinalCost(f);
        if (senator <= spendable && senator <= cardinal) {
            PoliticsEngine.bribeSenator(s, f);
        } else if (cardinal <= spendable) {
            PoliticsEngine.bribeCardinal(s, f);
            // A growing College may let the family seize the papacy.
            Papacy.tryControlPapacy(s, f, rng);
        }
    }

    /** Rarely, a den-owning rival mounts a dirty trick against another rival. */
    private static void maybeDirtyTrick(GameState s, Family f, Rng rng) {
        if (!f.denOfIniquities || !rng.chance(DIRTY_TRICK_CHANCE)) return;

        // Target only other rival (non-human) families — never the human player.
        Family target = pickRivalTarget(s, f);
        if (target == null) return;

        // Weight toward cheaper mischief; assassination is a rare, expensive gambit.
        String kind;
        double roll = rng.nextDouble();
        if (roll < 0.55) kind = PoliticsEngine.RUMOUR;
        else if (roll < 0.90) kind = PoliticsEngine.ARSON;
        else kind = PoliticsEngine.ASSASSINATION;

        PoliticsEngine.dirtyTrick(s, f, target, kind, rng);
    }

    /** The richest non-human rival other than {@code f}, or null if none exist. */
    private static Family pickRivalTarget(GameState s, Family f) {
        Family best = null;
        long bestWorth = Long.MIN_VALUE;
        List<Family> families = s.families;
        for (Family other : families) {
            if (other.human || other.eliminated || other.id == f.id) continue;
            long w = WinConditions.netWorth(s, other);
            if (w > bestWorth) { bestWorth = w; best = other; }
        }
        return best;
    }
}

package com.whim.startrek.engine;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.FacilityType;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.ResourceType;
import com.whim.startrek.domain.StarSystem;

/**
 * Dynamic supply/demand market.
 *
 * <p>Base price per 1000 units of a resource is driven by how scarce the resource is relative
 * to the total amount of credits sloshing around the galaxy:
 * <pre>
 *     basePricePer1000(r) = totalGalacticSupply(r) / (totalGalacticCredits * 1000)
 * </pre>
 * A small supply against a large pile of credits drives the unit price down (lots of money chasing
 * little stock would normally push prices up, but the contract pins the formula exactly, so we honour
 * it verbatim).
 *
 * <p>The per-empire {@link #interestMultiplier(Empire)} layers a trading "spread" on top: WAR/UNREST
 * make trade more expensive, while owning TRADE_FACILITY buildings claws that back.
 *
 * <p>OFFICERS are never tradable — {@link #buy} and {@link #sell} reject them outright.
 */
public class EconomyEngine {

    public EconomyEngine() {
    }

    /** Total amount of a resource held across every empire treasury and every star-system stockpile. */
    public long totalGalacticSupply(ResourceType r, GameState s) {
        long total = 0L;
        if (s == null || r == null) {
            return 0L;
        }
        for (Empire e : s.getEmpires()) {
            total += Math.max(0L, e.getTreasury(r));
        }
        for (StarSystem sys : s.getMap().allSystems()) {
            total += Math.max(0L, sys.getStockpile(r));
        }
        return total;
    }

    /** Convenience: galaxy-wide CREDITS, used as the denominator of the price formula. */
    public long totalGalacticCredits(GameState s) {
        return totalGalacticSupply(ResourceType.CREDITS, s);
    }

    /**
     * Base price per 1000 units = totalGalacticSupply(r) / (totalGalacticCredits * 1000).
     * Returns 0 when there are no credits in the galaxy (avoids divide-by-zero).
     */
    public double basePricePer1000(ResourceType r, GameState s) {
        long credits = totalGalacticCredits(s);
        if (credits <= 0L) {
            return 0.0;
        }
        double supply = (double) totalGalacticSupply(r, s);
        return supply / ((double) credits * 1000.0);
    }

    /**
     * Trade spread for an empire. Starts at 1.0, rises with WAR (+0.5) / UNREST (+0.25), and is
     * reduced by 0.05 per TRADE_FACILITY the empire owns. Floored at 0.1 so it never goes free/negative.
     */
    public double interestMultiplier(Empire e) {
        if (e == null) {
            return 1.0;
        }
        double m = 1.0;
        switch (e.getStatus()) {
            case WAR:
                m += 0.5;
                break;
            case UNREST:
                m += 0.25;
                break;
            case PEACE:
            default:
                break;
        }
        int tradeFacilities = countFacilities(e, FacilityType.TRADE_FACILITY);
        m -= 0.05 * tradeFacilities;
        if (m < 0.1) {
            m = 0.1;
        }
        return m;
    }

    /**
     * Buy {@code units} of {@code r} for the empire, paying CREDITS at the current effective price.
     * Returns false (and changes nothing) when the resource is non-tradable, units are non-positive,
     * or the empire cannot afford it.
     */
    public boolean buy(Empire e, ResourceType r, long units, GameState s) {
        if (!canTrade(e, r, units)) {
            return false;
        }
        long cost = (long) Math.ceil(unitCost(r, units, e, s));
        if (cost < 0L) {
            cost = 0L;
        }
        if (e.getTreasury(ResourceType.CREDITS) < cost) {
            return false;
        }
        e.addTreasury(ResourceType.CREDITS, -cost);
        e.addTreasury(r, units);
        return true;
    }

    /**
     * Sell {@code units} of {@code r} from the empire's treasury, receiving CREDITS.
     * Returns false (and changes nothing) when the resource is non-tradable, units are non-positive,
     * or the empire does not hold enough stock.
     */
    public boolean sell(Empire e, ResourceType r, long units, GameState s) {
        if (!canTrade(e, r, units)) {
            return false;
        }
        if (e.getTreasury(r) < units) {
            return false;
        }
        long proceeds = (long) Math.floor(unitCost(r, units, e, s));
        if (proceeds < 0L) {
            proceeds = 0L;
        }
        e.addTreasury(r, -units);
        e.addTreasury(ResourceType.CREDITS, proceeds);
        return true;
    }

    /** Effective credit value of {@code units} of {@code r} for empire {@code e}. */
    private double unitCost(ResourceType r, long units, Empire e, GameState s) {
        double per1000 = basePricePer1000(r, s);
        double multiplier = interestMultiplier(e);
        return per1000 * multiplier * ((double) units / 1000.0);
    }

    private boolean canTrade(Empire e, ResourceType r, long units) {
        if (e == null || r == null) {
            return false;
        }
        if (!r.isTradable()) {
            return false; // OFFICERS, etc.
        }
        return units > 0L;
    }

    private int countFacilities(Empire e, FacilityType t) {
        int n = 0;
        for (StarSystem sys : e.getSystems()) {
            n += Math.max(0, sys.getFacility(t));
        }
        return n;
    }
}

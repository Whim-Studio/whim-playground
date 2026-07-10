package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.event.Event;
import com.whim.merchantprince.model.event.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * World-scale events (GAME_DESIGN_REFERENCE §5): plague, war, papal interdict, and
 * the early Reformation. Rolled once per year; effects feed pricing (crisis goods
 * spike, interdict depresses relics) and city stock/population.
 *
 * <p>Odds and effect magnitudes are ASSUMPTIONS (the original's exact tables are
 * unconfirmed) and live as named constants here / in {@link Constants}.
 *
 * <h3>Coupling to pricing</h3>
 * {@link TurnManager} rolls events immediately before {@link PricingEngine#driftPrices}
 * each turn. Rather than mutate every affected price twice, this engine records the
 * year's price disturbances in {@link #priceEffects} and exposes
 * {@link #priceMultiplier} so {@code driftPrices} can fold them into each city's base
 * price for exactly this year. The list is transient (rebuilt each roll, not
 * serialised) — a plague spike lasts one trading year, matching the yearly cadence.
 */
public final class EventEngine {
    private EventEngine() { }

    // ---- Event odds already live in Constants (PLAGUE/WAR/INTERDICT_YEARLY_CHANCE).

    // ---- Effect magnitudes (ASSUMPTION — tunable) ----------------------
    /** Fraction of a city's stock a plague wipes out (population/trade collapse). */
    private static final double PLAGUE_STOCK_LOSS = 0.40;
    /** Plague makes scarce comforts dear: these goods spike in the stricken city. */
    private static final double PLAGUE_GOOD_SPIKE = 1.6;
    /** War strangles a city's supply lines: stock falls and prices climb. */
    private static final double WAR_STOCK_LOSS = 0.30;
    private static final double WAR_GOOD_SPIKE = 1.4;
    /** An interdict freezes the relic trade world-wide: relic prices slump. */
    private static final double INTERDICT_RELIC_MULT = 0.5;
    /** The Reformation permanently cools relic demand: a deeper, world-wide slump. */
    private static final double REFORMATION_RELIC_MULT = 0.4;

    /** Reformation is a one-off; guard so it fires at most once per game. */
    private static boolean reformationDone = false;

    /**
     * A single price disturbance active for the current trading year: multiply the
     * base price of good {@link #goodOrdinal} (or all goods when {@code -1}) at city
     * {@link #cityId} (or every city when {@code -1}) by {@link #multiplier}.
     */
    private static final class PriceEffect {
        final int cityId;       // -1 = world-wide
        final int goodOrdinal;  // -1 = every good
        final double multiplier;
        PriceEffect(int cityId, int goodOrdinal, double multiplier) {
            this.cityId = cityId;
            this.goodOrdinal = goodOrdinal;
            this.multiplier = multiplier;
        }
    }

    /** This year's active price disturbances, consumed by {@link PricingEngine#driftPrices}. */
    private static List<PriceEffect> priceEffects = new ArrayList<PriceEffect>();

    /**
     * Combined multiplier the active events impose on a city/good's base price this
     * year (1.0 = no active disturbance). Called by {@link PricingEngine#driftPrices}.
     */
    public static double priceMultiplier(int cityId, Good g) {
        double m = 1.0;
        for (PriceEffect e : priceEffects) {
            boolean cityHit = (e.cityId < 0 || e.cityId == cityId);
            boolean goodHit = (e.goodOrdinal < 0 || e.goodOrdinal == g.ordinal());
            if (cityHit && goodHit) m *= e.multiplier;
        }
        return m;
    }

    /** Roll and apply this year's world events, logging each into {@link GameState#log}. */
    public static void rollYearlyEvents(GameState s, Rng rng) {
        // Fresh disturbance set each year — last year's spikes have already been
        // folded into base prices and should not stack.
        priceEffects = new ArrayList<PriceEffect>();
        if (s.cities.isEmpty()) return;

        if (rng.chance(Constants.PLAGUE_YEARLY_CHANCE)) rollPlague(s, rng);
        if (rng.chance(Constants.WAR_YEARLY_CHANCE)) rollWar(s, rng);
        if (rng.chance(Constants.INTERDICT_YEARLY_CHANCE)) rollInterdict(s, rng);

        // The Reformation is a historical anchor: it cannot occur before its earliest
        // year and, once it does, permanently reshapes the relic trade (fires once).
        if (!reformationDone && s.year >= Constants.REFORMATION_EARLIEST_YEAR) {
            rollReformation(s);
        }
    }

    /** A plague strikes one city: population/stock collapses; comfort goods spike. */
    private static void rollPlague(GameState s, Rng rng) {
        City c = s.cities.get(rng.nextInt(s.cities.size()));
        for (Good g : Good.ALL) {
            c.stock[g.ordinal()] = (int) Math.round(c.stock[g.ordinal()] * (1.0 - PLAGUE_STOCK_LOSS));
        }
        // Faith and remedies grow dear where the plague bites: relics and spices spike.
        priceEffects.add(new PriceEffect(c.id, Good.RELICS.ordinal(), PLAGUE_GOOD_SPIKE));
        priceEffects.add(new PriceEffect(c.id, Good.SPICES.ordinal(), PLAGUE_GOOD_SPIKE));
        s.logEvent(new Event(EventType.PLAGUE, s.year, c.id,
                "Plague sweeps " + c.name + " — stores empty and relics grow dear."));
    }

    /** A war disrupts one city's region: supply lines choke, prices climb. */
    private static void rollWar(GameState s, Rng rng) {
        City c = s.cities.get(rng.nextInt(s.cities.size()));
        for (Good g : Good.ALL) {
            c.stock[g.ordinal()] = (int) Math.round(c.stock[g.ordinal()] * (1.0 - WAR_STOCK_LOSS));
        }
        // Everything the city cannot make itself becomes scarce and dear.
        priceEffects.add(new PriceEffect(c.id, -1, WAR_GOOD_SPIKE));
        s.logEvent(new Event(EventType.WAR, s.year, c.id,
                "War engulfs " + c.name + " — trade is choked and prices soar."));
    }

    /** A papal interdict suspends the relic trade world-wide: relic prices slump. */
    private static void rollInterdict(GameState s, Rng rng) {
        priceEffects.add(new PriceEffect(-1, Good.RELICS.ordinal(), INTERDICT_RELIC_MULT));
        s.logEvent(new Event(EventType.INTERDICT, s.year, -1,
                "A papal interdict is declared — the relic trade collapses across Christendom."));
    }

    /** The Reformation permanently cools relic demand across the world (fires once). */
    private static void rollReformation(GameState s) {
        reformationDone = true;
        priceEffects.add(new PriceEffect(-1, Good.RELICS.ordinal(), REFORMATION_RELIC_MULT));
        s.logEvent(new Event(EventType.REFORMATION, s.year, -1,
                "The Reformation shakes the faithful — demand for holy relics withers."));
    }

    /**
     * Reset transient event state for a brand-new game. Safe to call from setup; the
     * per-year {@link #priceEffects} list is rebuilt on every roll regardless.
     */
    public static void reset() {
        reformationDone = false;
        priceEffects = new ArrayList<PriceEffect>();
    }
}

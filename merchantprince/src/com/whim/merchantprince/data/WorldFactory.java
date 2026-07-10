package com.whim.merchantprince.data;

import com.whim.merchantprince.engine.Constants;
import com.whim.merchantprince.engine.EventEngine;
import com.whim.merchantprince.engine.Rng;
import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.model.UnitType;

import java.awt.Color;

/**
 * Builds a fresh {@link GameState}: the four competing families (one human), the
 * Afro-Eurasian city map with per-good stock and local prices, and the player's
 * starting units docked at Venice (GAME_DESIGN_REFERENCE §2, §3).
 *
 * <p>T1 replaces the T0 starter map with a curated ~18-city Afro-Eurasian map that
 * spans the real trade world of 1300–1492: the Italian home ports, the Levant and
 * Egypt, Saharan/African gold-and-ivory termini, the silk cities of Byzantium and
 * Cathay, the spice ports of the Indian Ocean, and the legendary Xiangrala
 * (Shangri-La). Each region {@code produces} one or two goods cheaply and, by
 * omission, demands the rest — the pricing engine reads {@link City#produces} to
 * mean-revert cheap-where-made / dear-where-wanted.
 */
public final class WorldFactory {
    private WorldFactory() { }

    private static final String[] RIVAL_NAMES = { "Contarini", "Morosini", "Dandolo" };
    private static final int[] RIVAL_CRESTS = {
            new Color(160, 40, 40).getRGB(),
            new Color(40, 90, 160).getRGB(),
            new Color(50, 130, 70).getRGB(),
    };

    // ---- Starting-stock tuning (ASSUMPTION — tunable) ------------------
    /** Abundant local stock for a good the city produces. */
    private static final int PRODUCED_STOCK_LO = 150;
    private static final int PRODUCED_STOCK_HI = 260;
    /** Scarce local stock for a good the city merely demands. */
    private static final int DEMANDED_STOCK_LO = 15;
    private static final int DEMANDED_STOCK_HI = 70;
    /** Local base price is this multiple of nominal where produced / demanded. */
    private static final double PRODUCED_PRICE_MULT = 0.6;
    private static final double DEMANDED_PRICE_MULT = 1.4;

    public static GameState newGame(String surname, int crestColor, int endYear, Rng rng) {
        // Clear transient cross-game event state (e.g. the one-shot Reformation flag).
        EventEngine.reset();

        GameState s = new GameState();
        s.startYear = Constants.START_YEAR;
        s.year = Constants.START_YEAR;
        s.endYear = endYear;

        // Families: player id 0, then rivals.
        s.families.add(new Family(0, surname, crestColor, true, Constants.STARTING_FLORINS));
        for (int i = 0; i < RIVAL_NAMES.length; i++) {
            s.families.add(new Family(i + 1, RIVAL_NAMES[i], RIVAL_CRESTS[i], false, Constants.STARTING_FLORINS));
        }
        s.playerId = 0;

        buildCities(s, rng);

        // Player starts with a small galley and a large cog at Venice (city 0).
        s.units.add(new TransportUnit(0, 0, UnitType.SMALL_GALLEY, 0));
        s.units.add(new TransportUnit(1, 0, UnitType.LARGE_COG, 0));
        // Each rival gets one ship at Venice too.
        int uid = 2;
        for (int i = 0; i < RIVAL_NAMES.length; i++) {
            s.units.add(new TransportUnit(uid++, i + 1, UnitType.SMALL_GALLEY, 0));
        }

        return s;
    }

    /**
     * Seed the curated Afro-Eurasian map. Coordinates run roughly west→east in x
     * and north→south in y (abstract map units used only for straight-line travel
     * time). {@code coastal} gates ship access and classifies travel legs (a leg
     * between two coastal cities is a sea leg; any leg touching an inland city is
     * overland — see {@code TravelEngine}). Many eastern/African cities start
     * {@code closed} and must be opened before foreign traders may deal there.
     * Venice is fixed as city id 0: open, coastal, the player's home port.
     */
    private static void buildCities(GameState s, Rng rng) {
        //      name              region           x    y   coastal open   produced goods (cheap here)
        addCity(s, "Venice",        "Adriatic",     300, 150, true,  true,  Good.GLASS);
        addCity(s, "Genoa",         "Liguria",      250, 155, true,  true,  Good.GLASS);
        addCity(s, "Marseille",     "Provence",     200, 165, true,  true,  Good.GROG);
        addCity(s, "Ragusa",        "Dalmatia",     330, 185, true,  true,  Good.GROG);
        addCity(s, "Tunis",         "Maghreb",      235, 260, true,  true,  Good.GOLD);
        addCity(s, "Constantinople","Byzantium",    405, 150, true,  true,  Good.SILK);
        addCity(s, "Trebizond",     "Black Sea",    475, 135, true,  false, Good.SILK);
        addCity(s, "Alexandria",    "Egypt",        365, 275, true,  true,  Good.SPICES);
        addCity(s, "Cairo",         "Egypt",        375, 300, false, true,  Good.IVORY);
        addCity(s, "Jerusalem",     "Holy Land",    415, 260, false, false, Good.RELICS);
        addCity(s, "Aleppo",        "Levant",       440, 215, false, false, Good.SPICES);
        addCity(s, "Hormuz",        "Persian Gulf", 560, 250, true,  false, Good.SPICES);
        addCity(s, "Timbuktu",      "West Africa",  150, 330, false, false, Good.GOLD, Good.IVORY);
        addCity(s, "Kilwa",         "East Africa",  455, 425, true,  false, Good.IVORY);
        addCity(s, "Calicut",       "India",        655, 330, true,  false, Good.SPICES);
        addCity(s, "Malacca",       "Malaya",       760, 365, true,  false, Good.SPICES);
        addCity(s, "Khanbaliq",     "Cathay",       820, 130, false, false, Good.SILK);
        addCity(s, "Xiangrala",     "Himalaya",     700, 190, false, false, Good.RELICS);

        // Every city gets baseline stock/prices: goods it makes are abundant & cheap,
        // goods it only demands are scarce & dear. Prices then drift year to year.
        for (City c : s.cities) {
            for (Good g : Good.ALL) {
                boolean makes = c.produces[g.ordinal()];
                c.stock[g.ordinal()] = makes
                        ? rng.range(PRODUCED_STOCK_LO, PRODUCED_STOCK_HI)
                        : rng.range(DEMANDED_STOCK_LO, DEMANDED_STOCK_HI);
                c.basePrice[g.ordinal()] = g.nominalValue * (makes ? PRODUCED_PRICE_MULT : DEMANDED_PRICE_MULT);
            }
        }
    }

    private static void addCity(GameState s, String name, String region,
                                int x, int y, boolean coastal, boolean open, Good... makes) {
        City c = new City(s.cities.size(), name, region, x, y, coastal, open);
        for (Good g : makes) {
            c.produces[g.ordinal()] = true;
        }
        s.cities.add(c);
    }
}

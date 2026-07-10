package com.whim.merchantprince.data;

import com.whim.merchantprince.engine.Constants;
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
 * <p>This T0 skeleton seeds a small representative map so the game is runnable;
 * the Economy task (T1) replaces {@link #buildCities} with the full curated map
 * (~15–20 cities) and richer regional production tables.
 */
public final class WorldFactory {
    private WorldFactory() { }

    private static final String[] RIVAL_NAMES = { "Contarini", "Morosini", "Dandolo" };
    private static final int[] RIVAL_CRESTS = {
            new Color(160, 40, 40).getRGB(),
            new Color(40, 90, 160).getRGB(),
            new Color(50, 130, 70).getRGB(),
    };

    public static GameState newGame(String surname, int crestColor, int endYear, Rng rng) {
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

    /** Seed a representative map. Each city produces some goods (cheap) and demands others (dear). */
    private static void buildCities(GameState s, Rng rng) {
        // id, name, region, x, y, coastal, open
        addCity(s, rng, "Venice",         "Adriatic",     300, 150, true,  true,  Good.GLASS);
        addCity(s, rng, "Alexandria",     "Egypt",        360, 280, true,  true,  Good.SPICES);
        addCity(s, rng, "Constantinople", "Byzantium",    420, 170, true,  true,  Good.SILK);
        addCity(s, rng, "Trebizond",      "Black Sea",    500, 150, true,  false, Good.SILK);
        addCity(s, rng, "Timbuktu",       "West Africa",  120, 360, false, false, Good.GOLD);
        addCity(s, rng, "Calicut",        "India",        640, 320, true,  false, Good.SPICES);
        addCity(s, rng, "Khanbaliq",      "Cathay",       780, 140, false, false, Good.SILK);
        addCity(s, rng, "Marseille",      "Provence",     180, 170, true,  true,  Good.GROG);

        // Every city gets baseline stock/prices; its produced good is abundant & cheap.
        for (City c : s.cities) {
            for (Good g : Good.ALL) {
                boolean makes = c.produces[g.ordinal()];
                c.stock[g.ordinal()] = makes ? rng.range(150, 260) : rng.range(20, 80);
                c.basePrice[g.ordinal()] = g.nominalValue * (makes ? 0.6 : 1.4);
            }
        }
    }

    private static void addCity(GameState s, Rng rng, String name, String region,
                                int x, int y, boolean coastal, boolean open, Good makes) {
        City c = new City(s.cities.size(), name, region, x, y, coastal, open);
        c.produces[makes.ordinal()] = true;
        s.cities.add(c);
    }
}

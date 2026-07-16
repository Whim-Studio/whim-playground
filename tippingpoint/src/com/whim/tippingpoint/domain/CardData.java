package com.whim.tippingpoint.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deck factories for Tipping Point.
 *
 * <p>The development deck is tuned so a skilled player can grow toward
 * {@link Rules#TARGET_POPULATION} citizens while keeping global CO2 below the
 * tipping point ({@link Rules#TIPPING_POINT_CO2}). INDUSTRIAL cards give strong
 * economy/food but emit CO2; GREEN cards pull CO2 back down (and still offer
 * modest income/food); INFRASTRUCTURE cards are roughly CO2-neutral growth.
 * A disciplined "grow with industry, then decarbonise with green" line keeps a
 * city's own CO2 near the X1/X2 band while the population climbs.
 */
public final class CardData {
    private CardData() {}

    private static final AtomicLong CITIZEN_SEQ = new AtomicLong(0L);

    // ---- helpers -----------------------------------------------------------

    private static void dev(List<DevelopmentCard> out, String id, String name, DevelopmentType type,
                            int cost, int cashFlowDelta, int foodDelta, int co2Delta) {
        out.add(new DevelopmentCard(id, name, type, cost, cashFlowDelta, foodDelta, co2Delta));
    }

    private static void weather(List<WeatherCard> out, String id, String name, WeatherSeverity severity,
                                int citizensLost, int cashLost, int foodProductionLost) {
        out.add(new WeatherCard(id, name, severity, citizensLost, cashLost, foodProductionLost));
    }

    // ---- development deck --------------------------------------------------

    /**
     * A tuned development deck ({@code >= 24} cards, mixing GREEN / INDUSTRIAL /
     * INFRASTRUCTURE). Returned in a fixed order; the engine shuffles it.
     */
    public static List<DevelopmentCard> developmentDeck() {
        List<DevelopmentCard> d = new ArrayList<DevelopmentCard>();

        // INDUSTRIAL — strong economy/food, positive CO2.                cost cF food co2
        dev(d, "IND_COAL_1",     "Coal Power Plant",   DevelopmentType.INDUSTRIAL, 3, 3, 0,  3);
        dev(d, "IND_COAL_2",     "Coal Power Plant",   DevelopmentType.INDUSTRIAL, 3, 3, 0,  3);
        dev(d, "IND_FACTORY_1",  "Factory",            DevelopmentType.INDUSTRIAL, 3, 2, 1,  2);
        dev(d, "IND_FACTORY_2",  "Factory",            DevelopmentType.INDUSTRIAL, 3, 2, 1,  2);
        dev(d, "IND_OIL_1",      "Oil Refinery",       DevelopmentType.INDUSTRIAL, 4, 4, 0,  3);
        dev(d, "IND_STEEL_1",    "Steel Mill",         DevelopmentType.INDUSTRIAL, 4, 3, 1,  3);
        dev(d, "IND_RANCH_1",    "Cattle Ranch",       DevelopmentType.INDUSTRIAL, 3, 1, 3,  2);
        dev(d, "IND_RANCH_2",    "Cattle Ranch",       DevelopmentType.INDUSTRIAL, 3, 1, 3,  2);
        dev(d, "IND_MEGAFARM_1", "Mega Farm",          DevelopmentType.INDUSTRIAL, 4, 1, 4,  2);
        dev(d, "IND_PORT_1",     "Shipping Port",      DevelopmentType.INDUSTRIAL, 4, 4, 1,  2);
        dev(d, "IND_CEMENT_1",   "Cement Works",       DevelopmentType.INDUSTRIAL, 3, 2, 0,  2);
        dev(d, "IND_MINE_1",     "Mining Complex",     DevelopmentType.INDUSTRIAL, 4, 4, 0,  3);

        // GREEN — reduce CO2, still offer modest income/food.
        dev(d, "GRN_SOLAR_1",    "Solar Array",        DevelopmentType.GREEN,      3, 1, 0, -2);
        dev(d, "GRN_SOLAR_2",    "Solar Array",        DevelopmentType.GREEN,      3, 1, 0, -2);
        dev(d, "GRN_WIND_1",     "Wind Farm",          DevelopmentType.GREEN,      3, 2, 0, -2);
        dev(d, "GRN_WIND_2",     "Wind Farm",          DevelopmentType.GREEN,      3, 2, 0, -2);
        dev(d, "GRN_FOREST_1",   "Reforestation",      DevelopmentType.GREEN,      2, 0, 1, -3);
        dev(d, "GRN_FOREST_2",   "Reforestation",      DevelopmentType.GREEN,      2, 0, 1, -3);
        dev(d, "GRN_CAPTURE_1",  "Carbon Capture",     DevelopmentType.GREEN,      4, 0, 0, -4);
        dev(d, "GRN_CAPTURE_2",  "Carbon Capture",     DevelopmentType.GREEN,      4, 0, 0, -4);
        dev(d, "GRN_TIDAL_1",    "Tidal Generator",    DevelopmentType.GREEN,      4, 2, 0, -3);
        dev(d, "GRN_VFARM_1",    "Vertical Farm",      DevelopmentType.GREEN,      3, 0, 3, -1);
        dev(d, "GRN_VFARM_2",    "Vertical Farm",      DevelopmentType.GREEN,      3, 0, 3, -1);
        dev(d, "GRN_RECYCLE_1",  "Recycling Center",   DevelopmentType.GREEN,      2, 1, 0, -2);
        dev(d, "GRN_GEO_1",      "Geothermal Plant",   DevelopmentType.GREEN,      4, 3, 0, -2);

        // INFRASTRUCTURE — roughly CO2-neutral growth.
        dev(d, "INF_HOUSING_1",  "Housing Block",      DevelopmentType.INFRASTRUCTURE, 2, 2, 0,  0);
        dev(d, "INF_HOUSING_2",  "Housing Block",      DevelopmentType.INFRASTRUCTURE, 2, 2, 0,  0);
        dev(d, "INF_AQUA_1",     "Aqueduct",           DevelopmentType.INFRASTRUCTURE, 2, 0, 2,  0);
        dev(d, "INF_AQUA_2",     "Aqueduct",           DevelopmentType.INFRASTRUCTURE, 2, 0, 2,  0);
        dev(d, "INF_RAIL_1",     "Rail Network",       DevelopmentType.INFRASTRUCTURE, 3, 3, 0,  1);
        dev(d, "INF_MARKET_1",   "Marketplace",        DevelopmentType.INFRASTRUCTURE, 2, 2, 1,  0);
        dev(d, "INF_MARKET_2",   "Marketplace",        DevelopmentType.INFRASTRUCTURE, 2, 2, 1,  0);
        dev(d, "INF_HOSPITAL_1", "Hospital",           DevelopmentType.INFRASTRUCTURE, 3, 2, 1,  0);
        dev(d, "INF_SCHOOL_1",   "School",             DevelopmentType.INFRASTRUCTURE, 2, 2, 0,  0);
        dev(d, "INF_WATER_1",    "Water Works",        DevelopmentType.INFRASTRUCTURE, 3, 1, 2,  1);
        dev(d, "INF_TRANSIT_1",  "Public Transit",     DevelopmentType.INFRASTRUCTURE, 3, 2, 0, -1);
        dev(d, "INF_TRANSIT_2",  "Public Transit",     DevelopmentType.INFRASTRUCTURE, 3, 2, 0, -1);

        return d;
    }

    // ---- weather deck ------------------------------------------------------

    /**
     * A weather deck ({@code >= 16} cards) spanning MILD / MODERATE / SEVERE.
     * Base magnitudes are scaled by the target player's Risk Factor multiplier
     * when resolved, so a low-CO2 city (X1) shrugs off much of the damage.
     */
    public static List<WeatherCard> weatherDeck() {
        List<WeatherCard> w = new ArrayList<WeatherCard>();

        // MILD — minor disruption.                                    cit cash food
        weather(w, "WX_DRIZZLE_1",   "Passing Storm",     WeatherSeverity.MILD,     0, 1, 0);
        weather(w, "WX_HEATWAVE_1",  "Heat Wave",         WeatherSeverity.MILD,     0, 0, 1);
        weather(w, "WX_FROST_1",     "Early Frost",       WeatherSeverity.MILD,     0, 1, 1);
        weather(w, "WX_HAIL_1",      "Hailstorm",         WeatherSeverity.MILD,     1, 0, 0);
        weather(w, "WX_DRY_1",       "Dry Spell",         WeatherSeverity.MILD,     0, 0, 1);
        weather(w, "WX_GALE_1",      "Coastal Gale",      WeatherSeverity.MILD,     0, 1, 0);

        // MODERATE — real damage.
        weather(w, "WX_FLOOD_1",     "River Flood",       WeatherSeverity.MODERATE, 1, 1, 1);
        weather(w, "WX_DROUGHT_1",   "Drought",           WeatherSeverity.MODERATE, 0, 1, 2);
        weather(w, "WX_WILDFIRE_1",  "Wildfire",          WeatherSeverity.MODERATE, 1, 0, 2);
        weather(w, "WX_STORM_1",     "Severe Storm",      WeatherSeverity.MODERATE, 1, 2, 0);
        weather(w, "WX_BLIZZARD_1",  "Blizzard",          WeatherSeverity.MODERATE, 1, 1, 1);
        weather(w, "WX_LOCUST_1",    "Locust Swarm",      WeatherSeverity.MODERATE, 0, 0, 3);

        // SEVERE — catastrophic; punishing for high-risk cities.
        weather(w, "WX_HURRICANE_1", "Hurricane",         WeatherSeverity.SEVERE,   2, 2, 1);
        weather(w, "WX_MEGADRT_1",   "Mega-Drought",      WeatherSeverity.SEVERE,   1, 1, 3);
        weather(w, "WX_SUPERSTORM_1","Superstorm",        WeatherSeverity.SEVERE,   2, 3, 1);
        weather(w, "WX_FIRESTORM_1", "Firestorm",         WeatherSeverity.SEVERE,   2, 1, 2);
        weather(w, "WX_SURGE_1",     "Ocean Surge",       WeatherSeverity.SEVERE,   2, 2, 2);
        weather(w, "WX_TORNADO_1",   "Tornado Outbreak",  WeatherSeverity.SEVERE,   3, 1, 1);

        return w;
    }

    // ---- citizens ----------------------------------------------------------

    /** A fresh, uniquely-identified citizen card of the requested type. */
    public static CitizenCard newCitizen(CitizenType type) {
        long n = CITIZEN_SEQ.incrementAndGet();
        String id = (type == CitizenType.FARMER ? "CIT_FARMER_" : "CIT_WORKER_") + n;
        String name = (type == CitizenType.FARMER ? "Farmer #" : "Worker #") + n;
        return new CitizenCard(id, name, type);
    }
}

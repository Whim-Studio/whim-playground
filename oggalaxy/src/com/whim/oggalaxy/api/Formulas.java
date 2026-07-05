package com.whim.oggalaxy.api;

/**
 * Pure, side-effect-free game formulas shared by the simulation and the UI so that
 * a cost or production figure shown on screen is exactly the one the engine charges.
 *
 * These follow the well-documented OGame formula family. Where OG Galaxy's exact
 * constants are unknown they are approximated and the approximation lives in
 * {@link Catalog} (base costs / factors) rather than being hard-coded here.
 */
public final class Formulas {

    private Formulas() {
    }

    /**
     * Cost of raising something from (level-1) to {@code level} given a base cost at
     * level 1 and a geometric cost factor. Level is 1-based (the cost of the FIRST
     * level == base). OGame: cost(n) = base * factor^(n-1).
     */
    public static Cost levelCost(Cost base, double factor, int level) {
        double f = Math.pow(factor, Math.max(0, level - 1));
        return new Cost(Math.floor(base.metal * f),
                Math.floor(base.crystal * f),
                Math.floor(base.deuterium * f),
                Math.floor(base.energy * f));
    }

    /** Metal Mine style production: base * level * 1.1^level, then * economy speed. */
    public static double mineProduction(double perLevelBase, int level) {
        if (level <= 0) return 0;
        return perLevelBase * level * Math.pow(1.1, level) * GameConfig.ECONOMY_SPEED;
    }

    /** Energy consumed by a mine at a level: 10 * level * 1.1^level (base configurable). */
    public static double mineEnergyUse(double perLevelBase, int level) {
        if (level <= 0) return 0;
        return Math.ceil(perLevelBase * level * Math.pow(1.1, level));
    }

    /** Solar plant energy output: 20 * level * 1.1^level. */
    public static double solarOutput(double perLevelBase, int level) {
        if (level <= 0) return 0;
        return Math.floor(perLevelBase * level * Math.pow(1.1, level));
    }

    /** Fusion reactor energy output scales with energy technology. */
    public static double fusionOutput(double perLevelBase, int level, int energyTech) {
        if (level <= 0) return 0;
        return Math.floor(perLevelBase * level * Math.pow(1.05 + 0.01 * energyTech, level));
    }

    /** Fusion reactor deuterium consumption per tick. */
    public static double fusionDeutUse(int level) {
        if (level <= 0) return 0;
        return Math.floor(10 * level * Math.pow(1.1, level)) * GameConfig.ECONOMY_SPEED;
    }

    /** Deuterium synthesizer temperature factor. */
    public static double deutTempFactor(int maxTemp) {
        return 1.44 - 0.004 * maxTemp;
    }

    /** Storage capacity: 5000 * floor(2.5 * e^(20*level/33)). Level 0 == BASE_STORAGE. */
    public static double storageCapacity(int level) {
        if (level <= 0) return GameConfig.BASE_STORAGE;
        return Math.floor(2.5 * Math.exp(20.0 * level / 33.0)) * 5000.0;
    }

    /**
     * Build time in ticks for a building of the given metal+crystal cost.
     * OGame: hours = (metal+crystal) / (2500 * (1+robotics) * 2^nanite * speed).
     * Rounded up to at least 1 tick.
     */
    public static int buildTimeTicks(Cost cost, int roboticsLevel, int naniteLevel) {
        double hours = (cost.metal + cost.crystal)
                / (2500.0 * (1 + roboticsLevel) * Math.pow(2, naniteLevel) * GameConfig.ECONOMY_SPEED);
        return Math.max(1, (int) Math.ceil(hours));
    }

    /**
     * Research time in ticks. OGame: hours = (metal+crystal)/(1000*(1+labLevel)*speed).
     */
    public static int researchTimeTicks(Cost cost, int labLevel) {
        double hours = (cost.metal + cost.crystal)
                / (1000.0 * (1 + labLevel) * GameConfig.ECONOMY_SPEED);
        return Math.max(1, (int) Math.ceil(hours));
    }

    /**
     * Shipyard build time in ticks for a single unit.
     * hours = (metal+crystal)/(2500*(1+shipyard)*2^nanite*speed).
     */
    public static int shipBuildTimeTicks(Cost cost, int shipyardLevel, int naniteLevel) {
        double hours = (cost.metal + cost.crystal)
                / (2500.0 * (1 + shipyardLevel) * Math.pow(2, naniteLevel) * GameConfig.FLEET_SPEED);
        return Math.max(1, (int) Math.ceil(hours));
    }

    /** Combat stat multiplier for a percentage tech (weapons/shield/armour): 1 + 0.1*level. */
    public static double techMultiplier(int techLevel) {
        return 1.0 + 0.1 * techLevel;
    }

    /** Number of extra planet colonies unlocked by Astrophysics level (OGame: ceil(level/2)). */
    public static int maxColoniesFromAstro(int astroLevel) {
        return (int) Math.ceil(astroLevel / 2.0);
    }

    /**
     * Fleet flight time in ticks between two positions given the slowest ship speed and
     * a fleet-speed percentage (10..100). Distance is the OGame galaxy/system/position metric.
     * hours = (10 + 35/speedPct * sqrt(10*distance/slowestSpeed)) / 3600 ... simplified to ticks.
     */
    public static int flightTimeTicks(int distance, double slowestSpeed, int speedPct) {
        double pct = Math.max(10, Math.min(100, speedPct)) / 100.0;
        double seconds = (10.0 + (35000.0 / pct) * Math.sqrt((double) distance / slowestSpeed))
                / GameConfig.FLEET_SPEED;
        int ticks = (int) Math.ceil(seconds / GameConfig.SECONDS_PER_TICK);
        return Math.max(1, ticks);
    }

    /** OGame distance metric between two coordinates. */
    public static int distance(int g1, int s1, int p1, int g2, int s2, int p2) {
        if (g1 != g2) return 20000 * Math.abs(g1 - g2);
        if (s1 != s2) return 2700 + 95 * Math.abs(s1 - s2);
        if (p1 != p2) return 1000 + 5 * Math.abs(p1 - p2);
        return 5; // same planet (moon <-> planet)
    }

    /** Fuel (deuterium) consumed for a round-trip-less one-way flight, per slowest-ship model. */
    public static double fuelConsumption(int distance, double baseFuelPerShip, int shipCount, int speedPct) {
        double pct = Math.max(10, Math.min(100, speedPct)) / 100.0;
        double consumption = baseFuelPerShip * shipCount * distance / 35000.0
                * Math.pow(pct + 0.1, 2);
        return Math.max(1, Math.floor(consumption));
    }
}

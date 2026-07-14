package com.whim.stars.sim;

import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.race.Race;

/**
 * Stateless helpers for a planet's yearly economic output: how many factories
 * and mines a colony can actually operate (limited by population), the resources
 * it produces, and how much of each mineral it mines.
 *
 * <p>All coefficients come from the race-wizard settings on {@link Race}; the
 * mapping from those settings to raw output is RECONSTRUCTED and lives here so a
 * corrected value is a one-line change.
 */
public final class Economy {

    private Economy() {
    }

    /** Factories a colony can staff: capped by population (per-10k-pop rate). */
    public static int operableFactories(Planet planet, Race race) {
        int staffed = (int) (race.factoriesPer10kPop() * (planet.population() / 10_000.0));
        return Math.min(planet.factories(), staffed);
    }

    /** Mines a colony can staff: capped by population (per-10k-pop rate). */
    public static int operableMines(Planet planet, Race race) {
        int staffed = (int) (race.minesPer10kPop() * (planet.population() / 10_000.0));
        return Math.min(planet.mines(), staffed);
    }

    /**
     * Resources produced this year: a direct contribution from population plus
     * the output of operable factories.
     * RECONSTRUCTED: 1 resource per 1000 colonists + factoryOutput per 10
     * operable factories.
     */
    public static int resources(Planet planet, Race race) {
        int fromPopulation = (int) (planet.population() / 1000);
        int fromFactories = operableFactories(planet, race) * race.factoryOutput() / 10;
        return fromPopulation + fromFactories;
    }

    /**
     * Minerals mined this year for one mineral: scales with operable mines and
     * the planet's concentration of that mineral.
     * RECONSTRUCTED: mineOutput kT per 10 operable mines at 100% concentration.
     */
    public static long miningOutput(Planet planet, Race race, Mineral mineral) {
        int mines = operableMines(planet, race);
        double rate = race.mineOutput() / 10.0;
        return (long) (mines * rate * planet.concentration(mineral) / 100.0);
    }
}

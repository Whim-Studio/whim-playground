package com.whim.stars.model.formulas;

import com.whim.stars.model.TechField;
import com.whim.stars.model.race.HabBand;
import com.whim.stars.model.race.Race;

/**
 * Central home for every gameplay formula whose exact source value is uncertain.
 *
 * <p>Each method is tagged {@code SOURCE:} (a confirmed value from the manual /
 * community consensus) or {@code RECONSTRUCTED:} (a structurally-correct best
 * reconstruction that must be balance-verified against source tables before it
 * is treated as authoritative). Isolating them here means a corrected value is a
 * one-line change and never hides inside the simulation loop.
 *
 * <p>This class imports no other model state beyond {@link Race} and primitives,
 * so it stays trivially unit-testable and free of side effects.
 */
public final class Formulas {

    /** Base maximum population (colonists) an ideal (green) world supports. */
    public static final long BASE_MAX_POP = 1_000_000L;

    private Formulas() {
    }

    /**
     * Habitability of a planet for a race, on [-1.0, +1.0]. +1 is a perfect
     * green world, 0 sits on the edge of a comfortable band, negative values
     * are hostile (red) worlds that kill colonists.
     *
     * RECONSTRUCTED: the real game normalizes the three axes and applies a
     * red-scaling curve. This reproduces the sign and monotonic ordering (ideal
     * &gt; edge &gt; hostile) with a clean normalized-distance model; the precise
     * red-value curve still needs source verification.
     */
    public static double habitability(Race race, int gravity, int temperature, int radiation) {
        HabBand[] bands = { race.gravity(), race.temperature(), race.radiation() };
        int[] values = { gravity, temperature, radiation };

        double sumInBandSq = 0.0;
        int consideredAxes = 0;
        double redness = 0.0;
        boolean anyOutside = false;

        for (int i = 0; i < 3; i++) {
            HabBand band = bands[i];
            if (band.isImmune()) {
                continue; // immune axes are always ideal, contribute nothing
            }
            consideredAxes++;
            int off = Math.abs(values[i] - band.center());
            int half = Math.max(1, band.halfWidth());
            if (off <= half) {
                double t = (double) off / half; // 0 at center, 1 at band edge
                sumInBandSq += t * t;
            } else {
                anyOutside = true;
                // How far beyond the band edge, in band-widths (capped at 1 per axis).
                redness += Math.min(1.0, (double) (off - half) / half);
            }
        }

        if (consideredAxes == 0) {
            return 1.0; // immune to everything -> every world is ideal
        }
        if (anyOutside) {
            // Hostile world: strictly negative, scaled by how many axes and how far.
            return -Math.min(1.0, redness / consideredAxes);
        }
        double meanDistort = Math.sqrt(sumInBandSq / consideredAxes); // 0..1
        return 1.0 - meanDistort; // 1 at ideal, 0 at a band edge
    }

    /**
     * Maximum colonists a planet supports for a race given its habitability.
     * RECONSTRUCTED: linear in habitability against a per-race base; a green
     * world holds {@link #BASE_MAX_POP}, a hostile world holds none.
     */
    public static long maxPopulation(Race race, double habitability) {
        if (habitability <= 0) {
            return 0;
        }
        double joatBonus = 1.0; // JOAT/HE modifiers wired by the engine later
        return Math.round(BASE_MAX_POP * habitability * joatBonus);
    }

    /**
     * Signed one-year population change (colonists). Positive on habitable
     * worlds (logistic, throttled by crowding as pop nears max), negative on
     * hostile worlds (die-off proportional to how red the world is).
     *
     * RECONSTRUCTED: logistic crowding model with an over-max culling term.
     */
    public static long populationGrowth(Race race, long population, long maxPopulation, double habitability) {
        if (population <= 0) {
            return 0;
        }
        if (habitability <= 0) {
            // Red-world die-off: lose up to ~10% per full-red year.
            double dieRate = 0.10 * Math.min(1.0, -habitability);
            return -(long) Math.ceil(population * dieRate);
        }
        if (maxPopulation <= 0) {
            return 0;
        }
        if (population >= maxPopulation) {
            // Over capacity: culled back toward the cap.
            return -(long) ((population - maxPopulation) / 2);
        }
        double crowding = 1.0 - (double) population / maxPopulation; // ->0 as pop nears cap
        double growth = population * race.maxGrowthRate() * habitability * crowding;
        return (long) Math.floor(growth);
    }

    /**
     * Resource cost to advance {@code field} from {@code currentLevel} to the
     * next level. Rises steeply with the field's own level and, per the famous
     * Stars! interplay, also with the sum of all other field levels.
     *
     * RECONSTRUCTED: a quadratic-in-level curve plus a cross-field term. Exposed
     * as one function so the true polynomial can be dropped in unchanged.
     */
    public static long researchCost(TechField field, int currentLevel, int totalAllFieldLevels, double raceCostFactor) {
        int next = currentLevel + 1;
        double base = (next * next) * 10.0 + next * 30.0 + 50.0;
        int otherLevels = Math.max(0, totalAllFieldLevels - currentLevel);
        double crossField = otherLevels * next * 1.0;
        return Math.round((base + crossField) * raceCostFactor);
    }

    /**
     * Light-years a fleet travels in one year at a given warp.
     * SOURCE: distance per year = warp^2 (warp 5 = 25 ly, warp 9 = 81 ly).
     */
    public static int warpDistance(int warp) {
        return warp * warp;
    }

    /**
     * Fuel (mg) consumed to move {@code massKt} of fleet one year at {@code warp}.
     * RECONSTRUCTED: rises with mass and with the square of warp; a real engine's
     * per-warp fuel table overrides this once component data is filled in.
     */
    public static long fuelUsage(long massKt, int warp) {
        if (warp <= 1) {
            return 0; // warp-1 crawl is free (matches the "out of fuel" fallback)
        }
        double perKt = (warp * warp) / 200.0;
        return (long) Math.ceil(massKt * perKt);
    }
}

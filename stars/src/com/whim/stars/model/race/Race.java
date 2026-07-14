package com.whim.stars.model.race;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.whim.stars.model.TechField;

/**
 * A player's race as defined in the race wizard: its primary trait, lesser
 * traits, three habitability bands, growth rate and economy settings
 * (factories/mines output and operability, plus per-field research cost).
 *
 * <p>The economy numbers mirror the race-wizard knobs of the original game.
 * Where the mapping from wizard clicks to raw numbers is not source-confirmed
 * it is marked RECONSTRUCTED and isolated here so it can be corrected in one
 * place.
 */
public final class Race implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String pluralName;
    private final PRT prt;
    private final Set<LRT> lrts;

    private final HabBand gravity;
    private final HabBand temperature;
    private final HabBand radiation;

    /** Fraction of population that can be added per year on an ideal world, e.g. 0.15 = 15%. */
    private final double maxGrowthRate;

    // --- Economy settings (race-wizard knobs) ---
    private final int factoryOutput;      // resources produced per 10 operable factories
    private final int factoryCost;        // germanium kT to build one factory
    private final int factoriesPer10kPop; // operable factories per 10k colonists
    private final int mineOutput;         // mineral kT per 10 mines at 100% concentration
    private final int mineCost;           // resources to build one mine
    private final int minesPer10kPop;     // operable mines per 10k colonists

    private final double[] researchCostFactor; // per-field multiplier (1.0 = normal)

    public Race(String name, String pluralName, PRT prt, Set<LRT> lrts,
                HabBand gravity, HabBand temperature, HabBand radiation,
                double maxGrowthRate,
                int factoryOutput, int factoryCost, int factoriesPer10kPop,
                int mineOutput, int mineCost, int minesPer10kPop,
                double[] researchCostFactor) {
        this.name = name;
        this.pluralName = pluralName;
        this.prt = prt;
        this.lrts = lrts == null ? EnumSet.noneOf(LRT.class) : EnumSet.copyOf(lrts);
        this.gravity = gravity;
        this.temperature = temperature;
        this.radiation = radiation;
        this.maxGrowthRate = maxGrowthRate;
        this.factoryOutput = factoryOutput;
        this.factoryCost = factoryCost;
        this.factoriesPer10kPop = factoriesPer10kPop;
        this.mineOutput = mineOutput;
        this.mineCost = mineCost;
        this.minesPer10kPop = minesPer10kPop;
        this.researchCostFactor = researchCostFactor.clone();
    }

    /**
     * A balanced Humanoid-style race: the game's default starting point. Values
     * are the widely published Humanoid defaults (✅) where known; growth 15%.
     */
    public static Race humanoid(String name) {
        return humanoid(name, PRT.JOAT);
    }

    /**
     * A balanced Humanoid-style race with a chosen primary trait. The economy
     * numbers are the Humanoid defaults; only the PRT label varies, so the race
     * picker's choice is preserved on the model even though most PRT mechanical
     * effects are later-phase wiring.
     */
    public static Race humanoid(String name, PRT prt) {
        double[] rc = new double[TechField.values().length];
        for (int i = 0; i < rc.length; i++) rc[i] = 1.0;
        return new Race(name, name + "s", prt == null ? PRT.JOAT : prt, EnumSet.noneOf(LRT.class),
                HabBand.of(50, 25), HabBand.of(50, 25), HabBand.of(50, 25),
                0.15,
                10, 10, 10,   // 10 resources / 10 factories, 10 G each, 10 per 10k pop
                10, 5, 10,     // 10 kT / 10 mines, 5 res each, 10 per 10k pop
                rc);
    }

    public String name() { return name; }
    public String pluralName() { return pluralName; }
    public PRT prt() { return prt; }
    public Set<LRT> lrts() { return Collections.unmodifiableSet(lrts); }
    public boolean has(LRT lrt) { return lrts.contains(lrt); }

    public HabBand gravity() { return gravity; }
    public HabBand temperature() { return temperature; }
    public HabBand radiation() { return radiation; }

    public double maxGrowthRate() { return maxGrowthRate; }

    public int factoryOutput() { return factoryOutput; }
    public int factoryCost() { return factoryCost; }
    public int factoriesPer10kPop() { return factoriesPer10kPop; }
    public int mineOutput() { return mineOutput; }
    public int mineCost() { return mineCost; }
    public int minesPer10kPop() { return minesPer10kPop; }

    public double researchCostFactor(TechField field) {
        return researchCostFactor[field.ordinal()];
    }
}

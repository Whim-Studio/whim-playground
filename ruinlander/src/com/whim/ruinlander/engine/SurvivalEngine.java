package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Item;
import com.whim.ruinlander.domain.Player;
import com.whim.ruinlander.domain.StatType;
import com.whim.ruinlander.domain.TerrainType;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic survival math. No RNG, no Swing. Models the per-step decay of the
 * six survival meters and converts critical meters into HEALTH damage.
 *
 * <p>Stat conventions (see {@link StatType}):
 * HUNGER/THIRST/FATIGUE/RADIATION accumulate upward (0 good, high bad);
 * TEMPERATURE is comfort (100 ideal, low = hypothermia); HEALTH downward = death.
 */
public class SurvivalEngine {

    // Per-step base decay.
    private static final int HUNGER_PER_STEP = 2;
    private static final int THIRST_PER_STEP = 3;
    private static final int FATIGUE_PER_STEP = 2;

    // Temperature relaxes toward the terrain's ambient comfort each step.
    private static final int TEMP_DRIFT = 5;

    // Radiation accrued per step while standing in a TOXIC tile.
    private static final int TOXIC_RAD_PER_STEP = 7;

    // Thresholds at which a meter starts bleeding HEALTH, and the damage it deals.
    private static final int HUNGER_CRIT = 90, HUNGER_DMG = 3;
    private static final int THIRST_CRIT = 90, THIRST_DMG = 4;
    private static final int FATIGUE_CRIT = 95, FATIGUE_DMG = 2;
    private static final int RAD_CRIT = 80, RAD_DMG = 5;
    private static final int TEMP_CRIT = 25, TEMP_DMG = 3; // hypothermia (low temp)

    public SurvivalEngine() {
    }

    /**
     * Apply one overworld step on {@code terrain}: advance hunger/thirst/fatigue,
     * drift temperature toward ambient, add radiation in toxic zones, then apply
     * any HEALTH damage from critical meters. Returns human-readable log notes
     * (possibly empty). Mutates the player via its clamping setters.
     */
    public List<String> applyStep(Player p, TerrainType terrain) {
        List<String> notes = new ArrayList<String>();

        p.addStat(StatType.HUNGER, HUNGER_PER_STEP);
        p.addStat(StatType.THIRST, THIRST_PER_STEP);
        p.addStat(StatType.FATIGUE, FATIGUE_PER_STEP);

        driftTemperature(p, ambientFor(terrain));

        if (terrain == TerrainType.TOXIC) {
            p.addStat(StatType.RADIATION, TOXIC_RAD_PER_STEP);
            notes.add("The geiger counter screams — radiation rising.");
        }

        applyEnvironmentalDamage(p, notes);
        return notes;
    }

    /** Consume an item: apply its restorative effects to the player's meters. */
    public List<String> consume(Player p, Item item) {
        List<String> notes = new ArrayList<String>();
        if (item == null) {
            return notes;
        }
        if (item.getHungerRestore() != 0) {
            p.addStat(StatType.HUNGER, -item.getHungerRestore());
        }
        if (item.getThirstRestore() != 0) {
            p.addStat(StatType.THIRST, -item.getThirstRestore());
        }
        if (item.getFatigueRestore() != 0) {
            p.addStat(StatType.FATIGUE, -item.getFatigueRestore());
        }
        if (item.getHealthRestore() != 0) {
            p.addStat(StatType.HEALTH, item.getHealthRestore());
        }
        // radiationReduce may be negative (dirty water ADDS radiation).
        if (item.getRadiationReduce() != 0) {
            p.addStat(StatType.RADIATION, -item.getRadiationReduce());
        }
        notes.add("You use the " + item.getName() + ".");
        if (item.getRadiationReduce() < 0) {
            notes.add("It was contaminated — radiation ticks up.");
        }
        return notes;
    }

    private void driftTemperature(Player p, int ambient) {
        int temp = p.getStat(StatType.TEMPERATURE);
        if (temp < ambient) {
            p.setStat(StatType.TEMPERATURE, Math.min(ambient, temp + TEMP_DRIFT));
        } else if (temp > ambient) {
            p.setStat(StatType.TEMPERATURE, Math.max(ambient, temp - TEMP_DRIFT));
        }
    }

    /** Ambient comfort target by terrain (higher = warmer / safer). */
    private int ambientFor(TerrainType terrain) {
        switch (terrain) {
            case SETTLEMENT:
                return 85;
            case CITY:
            case ROAD:
                return 75;
            case FOREST:
                return 70;
            case WASTELAND:
                return 55;
            case WATER:
                return 45;
            case TOXIC:
                return 40;
            default:
                return 65;
        }
    }

    private void applyEnvironmentalDamage(Player p, List<String> notes) {
        if (p.getStat(StatType.HUNGER) >= HUNGER_CRIT) {
            p.addStat(StatType.HEALTH, -HUNGER_DMG);
            notes.add("Starving — you lose strength.");
        }
        if (p.getStat(StatType.THIRST) >= THIRST_CRIT) {
            p.addStat(StatType.HEALTH, -THIRST_DMG);
            notes.add("Dehydration is killing you.");
        }
        if (p.getStat(StatType.FATIGUE) >= FATIGUE_CRIT) {
            p.addStat(StatType.HEALTH, -FATIGUE_DMG);
            notes.add("Exhaustion gnaws at your body.");
        }
        if (p.getStat(StatType.RADIATION) >= RAD_CRIT) {
            p.addStat(StatType.HEALTH, -RAD_DMG);
            notes.add("Radiation sickness wracks you.");
        }
        if (p.getStat(StatType.TEMPERATURE) <= TEMP_CRIT) {
            p.addStat(StatType.HEALTH, -TEMP_DMG);
            notes.add("Hypothermia sets in — you're freezing.");
        }
    }
}

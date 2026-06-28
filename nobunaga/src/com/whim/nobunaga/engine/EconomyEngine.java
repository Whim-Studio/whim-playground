package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;
import com.whim.nobunaga.domain.Season;

import java.util.ArrayList;
import java.util.List;

/**
 * Seasonal economy: Fall harvest, per-season taxation + loyalty drift, and
 * soldier upkeep with starvation attrition. All formulas are deterministic
 * given the province state (no RNG here; randomness lives in EventEngine).
 */
public class EconomyEngine {

    /** Run the full economic pass for every owned province; returns log lines. */
    public List<String> process(GameState s) {
        List<String> log = new ArrayList<String>();
        boolean fall = (s.season == Season.FALL);
        for (Province p : s.provinces) {
            if (p.isNeutral()) {
                continue; // neutral fiefs are inert background garrisons
            }
            if (fall) {
                int harvest = harvest(p);
                p.setRice(p.getRice() + harvest);
                log.add(p.getName() + ": fall harvest +" + harvest + " koku");
            }
            int tax = taxYield(p);
            p.setGold(p.getGold() + tax);
            driftLoyalty(p);

            int upkeep = upkeep(p.getSoldiers());
            int rice = p.getRice() - upkeep;
            if (rice < 0) {
                int deficit = -rice;
                int attrition = Math.min(p.getSoldiers(), starvation(deficit));
                p.setSoldiers(p.getSoldiers() - attrition);
                p.setRice(0);
                if (attrition > 0) {
                    log.add(p.getName() + ": starvation! -" + attrition + " soldiers (no rice)");
                }
            } else {
                p.setRice(rice);
            }
        }
        return log;
    }

    /** Fall harvest: rice += round(cultivation * 12 * (0.6 + 0.01*loyalty)). */
    public int harvest(Province p) {
        return (int) Math.round(p.getCultivation() * 12.0 * (0.6 + 0.01 * p.getLoyalty()));
    }

    /** Per-season tax: gold += round(cultivation * taxRate * 0.05 * loyalty/100). */
    public int taxYield(Province p) {
        return (int) Math.round(p.getCultivation() * p.getTaxRate() * 0.05 * (p.getLoyalty() / 100.0));
    }

    /** Loyalty drifts toward (100 - taxRate), at most 5 points per season. */
    public void driftLoyalty(Province p) {
        int target = 100 - p.getTaxRate();
        int diff = target - p.getLoyalty();
        if (diff == 0) {
            return;
        }
        int sign = diff > 0 ? 1 : -1;
        int step = Math.min(5, Math.abs(diff));
        int next = p.getLoyalty() + sign * step;
        if (next < 0) next = 0;
        if (next > 100) next = 100;
        p.setLoyalty(next);
    }

    /** Rice upkeep per season: ceil(soldiers/100 * 4). */
    public int upkeep(int soldiers) {
        return (int) Math.ceil(soldiers / 100.0 * 4.0);
    }

    /** Soldiers lost when rice deficit is {@code deficit} koku. */
    public int starvation(int deficit) {
        return (int) Math.ceil(deficit * 5.0);
    }
}

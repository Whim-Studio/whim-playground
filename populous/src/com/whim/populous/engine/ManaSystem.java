package com.whim.populous.engine;

import java.util.List;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.domain.GameStateManager;
import com.whim.populous.domain.Settlement;

/**
 * Accrues mana for each side every tick, proportional to that side's population
 * and settlement tiers, clamped to {@code maxMana}. Bigger/flatter settlements
 * (see the CONTRACT weight table) generate more mana per tick.
 *
 * Runs exclusively on the simulation thread.
 */
final class ManaSystem {

    /** Small baseline so a fledgling side still accrues mana before it builds. */
    private static final int BASELINE_GAIN = 1;

    /** Mana weight per settlement tier — mirrors the CONTRACT table. */
    static int manaWeight(SettlementType type) {
        switch (type) {
            case TENT:   return 1;
            case HUT:    return 2;
            case HOUSE:  return 4;
            case TOWER:  return 7;
            case CASTLE: return 12;
            default:     return 0;
        }
    }

    void accrue(GameStateManager mgr) {
        int goodGain = BASELINE_GAIN;
        int evilGain = BASELINE_GAIN;

        List<Settlement> settlements = mgr.settlements();
        for (int i = 0; i < settlements.size(); i++) {
            Settlement s = settlements.get(i);
            int w = manaWeight(s.type());
            if (s.owner() == Allegiance.GOOD) {
                goodGain += w;
            } else if (s.owner() == Allegiance.EVIL) {
                evilGain += w;
            }
        }

        // Population trickle: every walker contributes a little faith.
        goodGain += mgr.population(Allegiance.GOOD) / 4;
        evilGain += mgr.population(Allegiance.EVIL) / 4;

        addClamped(mgr, Allegiance.GOOD, goodGain);
        addClamped(mgr, Allegiance.EVIL, evilGain);
    }

    private void addClamped(GameStateManager mgr, Allegiance side, int gain) {
        int max = mgr.maxMana();
        int next = mgr.getMana(side) + gain;
        if (next > max) {
            next = max;
        }
        if (next < 0) {
            next = 0;
        }
        mgr.setMana(side, next);
    }
}

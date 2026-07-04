package com.whim.swd6.ui;

import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.RpgEngine;
import com.whim.swd6.api.WoundLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DEV STUB, replaced by Main (D6Engine) at runtime.
 *
 * A faithful-enough {@link RpgEngine} implementation of the R&amp;E dice rules so the
 * UI is fully runnable and demoable without Task 2. Real dice math via
 * {@link java.util.Random}; the Wild Die explodes on 6 and flags a complication on 1.
 *
 * Owned by Task 3 (ui). Not shipped in the wired app.
 */
public final class StubEngine implements RpgEngine {

    private final Random rng;

    public StubEngine() {
        this.rng = new Random();
    }

    public StubEngine(long seed) {
        this.rng = new Random(seed);
    }

    private int d6() {
        return rng.nextInt(6) + 1;
    }

    @Override
    public RollResult roll(DiceCode code, boolean useWildDie, int target) {
        int dice = code.getDice();
        int pips = code.getPips();

        // Determine how many "normal" dice to roll; one die of the pool is the Wild Die.
        int normalCount = useWildDie ? Math.max(0, dice - 1) : dice;

        List<Integer> normal = new ArrayList<Integer>();
        int normalSum = 0;
        int highestNormal = 0;
        for (int i = 0; i < normalCount; i++) {
            int v = d6();
            normal.add(v);
            normalSum += v;
            if (v > highestNormal) {
                highestNormal = v;
            }
        }

        List<Integer> wildChain = new ArrayList<Integer>();
        boolean complication = false;
        boolean exploded = false;
        int wildSum = 0;

        if (useWildDie) {
            int first = d6();
            wildChain.add(first);
            wildSum += first;
            if (first == 1) {
                complication = true;
            } else if (first == 6) {
                exploded = true;
                int next = d6();
                while (true) {
                    wildChain.add(next);
                    wildSum += next;
                    if (next != 6) {
                        break;
                    }
                    next = d6();
                }
            }
        }

        int total = normalSum + wildSum + pips;
        if (complication) {
            // R&E default: subtract the Wild Die's 1 and the single highest other die.
            total = total - 1 - highestNormal;
        }
        if (total < 0) {
            total = 0;
        }

        return new RollResult(code, normal, wildChain, pips, total, complication, exploded, target);
    }

    @Override
    public RollResult roll(DiceCode code, boolean useWildDie, DifficultyTier tier) {
        return roll(code, useWildDie, tier.representativeTarget());
    }

    @Override
    public List<RollResult> opposedRoll(DiceCode actorCode, DiceCode opponentCode) {
        List<RollResult> out = new ArrayList<RollResult>(2);
        out.add(roll(actorCode, true, -1));
        out.add(roll(opponentCode, true, -1));
        return out;
    }

    @Override
    public DamageResult resolveDamage(DiceCode damageCode, DiceCode resistCode) {
        RollResult dmg = roll(damageCode, true, -1);
        RollResult res = roll(resistCode, true, -1);
        int margin = dmg.getTotal() - res.getTotal();
        WoundLevel inflicted = WoundLevel.fromDamageMargin(margin);
        return new DamageResult(dmg, res, margin, inflicted);
    }

    @Override
    public DiceCode multiActionPenalty(int actions) {
        int extra = Math.max(0, actions - 1);
        return DiceCode.of(extra, 0);
    }

    @Override
    public DiceCode effectiveAttackCode(Combatant c) {
        DiceCode base = c.getAttackCode();
        DiceCode penalty = multiActionPenalty(c.getDeclaredActions());
        DiceCode wound = DiceCode.of(c.getWoundLevel().penaltyDice(), 0);
        return base.subtract(penalty).subtract(wound);
    }
}

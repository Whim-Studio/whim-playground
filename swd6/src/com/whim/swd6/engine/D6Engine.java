package com.whim.swd6.engine;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.Combatant;
import com.whim.swd6.api.DamageResult;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.DifficultyTier;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RollResult;
import com.whim.swd6.api.RpgEngine;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.WoundLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Star Wars D6 (Revised &amp; Expanded) rules engine.
 *
 * <p>Implements every dice, difficulty, damage, and combat computation described in
 * the build contract. All randomness flows through a single injectable {@link Dice}
 * so behaviour is deterministic under a fixed seed.</p>
 *
 * <h3>Wild Die (R&amp;E)</h3>
 * When {@code useWildDie} is true, exactly one die of the pool is the Wild Die and
 * the remaining {@code dice - 1} are "normal" dice (a 0D code rolls the Wild Die
 * alone). The Wild Die explodes on a 6 (reroll and keep adding while it rolls 6 —
 * the whole chain is recorded in {@code wildDieRolls} and {@code isWildExploded} is
 * set). A Wild Die of 1 on its first roll is a complication: the engine's default
 * handling subtracts that 1 <em>and</em> the single highest other die from the total.
 *
 * <h3>PC combat-skill choice (documented per contract)</h3>
 * For {@link #effectiveAttackCode(Combatant)} on a player character we pick the
 * character's best <em>combat</em> skill: the trained skill whose name matches a
 * known attack skill (blaster, melee combat, brawling, bowcaster, firearms, grenade,
 * missile weapons, thrown weapons, archaic guns/melee, vehicle blasters) and which
 * has the highest effective {@link PlayerCharacter#skillCode(Skill)}. If the PC has
 * no such trained skill we fall back to the raw Dexterity attribute code.
 *
 * Owned by Task 2 (engine).
 */
public final class D6Engine implements RpgEngine {

    /** Lower-cased substrings identifying a combat/attack skill. */
    private static final String[] COMBAT_SKILL_KEYS = {
        "blaster", "melee", "brawl", "bowcaster", "firearms", "grenade",
        "missile weapons", "thrown weapons", "archaic", "vehicle blasters", "gunnery"
    };

    private final Dice dice;

    /** Default constructor: an unseeded dice source. */
    public D6Engine() {
        this(new Dice());
    }

    /** Inject a dice source (wrap a fixed-seed {@link Random} for tests). */
    public D6Engine(Dice dice) {
        this.dice = dice == null ? new Dice() : dice;
    }

    /** Convenience: build an engine seeded from the given {@link Random}. */
    public D6Engine(Random random) {
        this(new Dice(random));
    }

    // ------------------------------------------------------------------
    // Core roll
    // ------------------------------------------------------------------

    @Override
    public RollResult roll(DiceCode code, boolean useWildDie, int target) {
        DiceCode c = code == null ? DiceCode.ZERO : code;
        int poolDice = c.getDice();
        int pips = c.getPips();

        List<Integer> normalDice = new ArrayList<Integer>();
        List<Integer> wildDieRolls = new ArrayList<Integer>();
        boolean complication = false;
        boolean wildExploded = false;

        if (useWildDie) {
            // Exactly one die of the pool is the Wild Die; the rest are normal.
            // A 0D code still rolls the Wild Die alone.
            int normalCount = Math.max(0, poolDice - 1);
            for (int i = 0; i < normalCount; i++) {
                normalDice.add(dice.rollDie());
            }
            int first = dice.rollDie();
            wildDieRolls.add(first);
            if (first == 1) {
                complication = true;
            } else if (first == 6) {
                wildExploded = true;
                int r = first;
                while (r == 6) {
                    r = dice.rollDie();
                    wildDieRolls.add(r);
                }
            }
        } else {
            for (int i = 0; i < poolDice; i++) {
                normalDice.add(dice.rollDie());
            }
        }

        int normalSum = sum(normalDice);
        int wildSum = sum(wildDieRolls);
        int total = normalSum + wildSum + pips;

        if (complication) {
            // Subtract the Wild Die's 1 and the single highest other (normal) die.
            total -= 1;
            if (!normalDice.isEmpty()) {
                total -= max(normalDice);
            }
        }

        return new RollResult(c, normalDice, wildDieRolls, pips, total,
                complication, wildExploded, target);
    }

    @Override
    public RollResult roll(DiceCode code, boolean useWildDie, DifficultyTier tier) {
        int target = tier == null ? -1 : tier.representativeTarget();
        return roll(code, useWildDie, target);
    }

    @Override
    public List<RollResult> opposedRoll(DiceCode actorCode, DiceCode opponentCode) {
        List<RollResult> results = new ArrayList<RollResult>(2);
        results.add(roll(actorCode, true, -1));
        results.add(roll(opponentCode, true, -1));
        return results;
    }

    @Override
    public DamageResult resolveDamage(DiceCode damageCode, DiceCode resistCode) {
        RollResult damageRoll = roll(damageCode, true, -1);
        RollResult resistRoll = roll(resistCode, true, -1);
        int margin = damageRoll.getTotal() - resistRoll.getTotal();
        WoundLevel inflicted = WoundLevel.fromDamageMargin(margin);
        return new DamageResult(damageRoll, resistRoll, margin, inflicted);
    }

    @Override
    public DiceCode multiActionPenalty(int actions) {
        return DiceCode.of(Math.max(0, actions - 1), 0);
    }

    @Override
    public DiceCode effectiveAttackCode(Combatant c) {
        if (c == null) {
            return DiceCode.ZERO;
        }
        DiceCode base;
        if (c.isPlayerCharacter() && c.getPc() != null) {
            base = bestCombatCode(c.getPc());
        } else {
            base = c.getAttackCode();
        }
        // Multiple-action penalty for this round.
        base = base.subtract(multiActionPenalty(c.getDeclaredActions()));
        // Wound penalty dice.
        int woundDice = c.getWoundLevel() == null ? 0 : c.getWoundLevel().penaltyDice();
        base = base.subtract(DiceCode.of(woundDice, 0));
        return base; // DiceCode.subtract already clamps at ZERO (0D).
    }

    /**
     * The PC's best combat skill code, or the raw Dexterity attribute if the
     * character has no trained combat skill (see class doc).
     */
    private DiceCode bestCombatCode(PlayerCharacter pc) {
        DiceCode best = null;
        for (Skill s : pc.getSkills()) {
            if (isCombatSkill(s.getName())) {
                DiceCode code = pc.skillCode(s);
                if (best == null || code.pipValue() > best.pipValue()) {
                    best = code;
                }
            }
        }
        if (best == null) {
            best = pc.getAttribute(Attribute.DEXTERITY);
        }
        return best;
    }

    private static boolean isCombatSkill(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase();
        for (String key : COMBAT_SKILL_KEYS) {
            if (n.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static int sum(List<Integer> values) {
        int total = 0;
        for (Integer v : values) {
            total += v;
        }
        return total;
    }

    private static int max(List<Integer> values) {
        int m = Integer.MIN_VALUE;
        for (Integer v : values) {
            if (v > m) {
                m = v;
            }
        }
        return m;
    }
}

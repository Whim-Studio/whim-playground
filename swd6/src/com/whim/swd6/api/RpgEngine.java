package com.whim.swd6.api;

import java.util.List;

/**
 * The rules engine: all dice, difficulty, damage, and combat math. Implemented by
 * the engine layer (Task 2), consumed by the UI (Task 3). Deterministic behavior is
 * expected apart from the RNG; implementations should accept a seedable source for
 * testing.
 *
 * Owned by the orchestrator (api). DO NOT modify signatures without updating the
 * build contract.
 */
public interface RpgEngine {

    /**
     * Roll a dice code. When {@code useWildDie} is true, one die of the pool is the
     * Wild Die: it explodes on 6 (reroll and add) and flags a complication on 1.
     * A code of 0D still rolls the Wild Die alone (per R&E). {@code target} is the
     * difficulty target number, or -1 for an untargeted roll.
     */
    RollResult roll(DiceCode code, boolean useWildDie, int target);

    /** Convenience: roll against a difficulty tier's representative target. */
    RollResult roll(DiceCode code, boolean useWildDie, DifficultyTier tier);

    /**
     * Opposed roll: both sides roll (with Wild Die). Returns the two results in a
     * two-element list [actor, opponent]; the caller compares totals. Ties favor the
     * opponent per house default.
     */
    List<RollResult> opposedRoll(DiceCode actorCode, DiceCode opponentCode);

    /**
     * Resolve a damage roll against a resistance code and return the wound inflicted
     * by this single hit (before escalation against existing wounds).
     */
    DamageResult resolveDamage(DiceCode damageCode, DiceCode resistCode);

    /**
     * The multiple-action penalty (R&E): declaring {@code actions} actions in one
     * round subtracts (actions - 1) dice from every action that round. Returns the
     * dice code to subtract (e.g. 2 actions -> 1D).
     */
    DiceCode multiActionPenalty(int actions);

    /**
     * Compute the effective code for a combatant's attack this round, applying the
     * multiple-action penalty and current wound penalty.
     */
    DiceCode effectiveAttackCode(Combatant c);
}

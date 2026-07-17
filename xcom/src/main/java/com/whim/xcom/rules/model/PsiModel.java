package com.whim.xcom.rules.model;

/**
 * Psionic-attack arbitration. A psi-capable alien (non-zero psi strength) can
 * assault a soldier's mind; success depends on the attacker's psi strength, the
 * defender's psi strength (resistance) and the distance. This is a pluggable
 * ruleset strategy just like {@link AccuracyModel} — {@code Ruleset1994Psi} is the
 * default and a variant can swap the formula without touching the engine.
 */
public interface PsiModel {

    /**
     * @return the percent chance (0..100) that a psi attack succeeds (e.g. panics
     *         the target). Returns 0 when the attacker has no psi ability.
     */
    int panicChancePercent(int attackerPsiStrength, int defenderPsiStrength, int distanceTiles);
}

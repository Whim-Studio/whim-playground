package com.whim.xcom.rules.model;

/**
 * 1994-style psionic model (reconstruction). Attack strength scales with the
 * attacker's psi strength; the defender's psi strength halves it as resistance,
 * and distance applies a mild falloff:
 *
 * <pre>chance% = attackerPsi − defenderPsi/2 − distance × 2</pre>
 *
 * clamped to 0..100. An attacker with no psi ability always returns 0. See
 * DESIGN.md for the flagged assumptions around psi.
 */
public final class Ruleset1994Psi implements PsiModel {

    @Override
    public int panicChancePercent(int attackerPsiStrength, int defenderPsiStrength, int distanceTiles) {
        if (attackerPsiStrength <= 0) {
            return 0;
        }
        int chance = attackerPsiStrength - defenderPsiStrength / 2 - distanceTiles * 2;
        return Math.max(0, Math.min(100, chance));
    }
}

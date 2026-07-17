package com.whim.xcom.rules.model;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * 1994 accuracy model.
 *
 * <pre>
 * final% = firingAccuracy × weaponAccuracy(mode) / 100
 *          × (kneeling      ? 1.15 : 1.0)
 *          × (oneHandedPen  ? 0.80 : 1.0)     // holding a two-handed weapon in one hand
 *          × arm-wound and smoke penalties
 * </pre>
 *
 * Sources: UFOpaedia "Firing Accuracy" and per-weapon pages. The kneel (×1.15)
 * and one-handed (×0.8) multipliers are the widely-documented reconstructions;
 * see DESIGN.md for the flagged assumptions.
 */
public final class Ruleset1994Accuracy implements AccuracyModel {

    private static final double KNEEL_BONUS = 1.15;
    private static final double ONE_HANDED_PENALTY = 0.80;

    @Override
    public int hitChancePercent(WeaponDef weapon, FireMode mode, ShotContext ctx) {
        if (weapon == null || !weapon.supports(mode)) {
            return 0;
        }
        double acc = ctx.firingAccuracy() * (double) weapon.accuracyPercent(mode) / 100.0;

        if (ctx.kneeling()) {
            acc *= KNEEL_BONUS;
        }
        if (weapon.twoHanded() && !ctx.usingBothHands()) {
            acc *= ONE_HANDED_PENALTY;
        }
        // Each fatal wound to the firing arm removes 10% accuracy (documented reconstruction).
        int wounds = ctx.fatalWoundsToFiringArm();
        if (wounds > 0) {
            acc *= Math.max(0.0, 1.0 - 0.10 * wounds);
        }
        // Smoke over the target roughly halves accuracy (assumption; see DESIGN.md).
        if (ctx.targetSmoke()) {
            acc *= 0.5;
        }
        if (acc < 0.0) {
            acc = 0.0;
        }
        return (int) Math.floor(acc);
    }
}

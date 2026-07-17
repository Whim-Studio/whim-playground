package com.whim.xcom.rules.model;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * Computes the to-hit chance of a shot. The 1994 core formula is
 * {@code finalAccuracy = firingAccuracy × weaponAccuracy(mode) / 100}, then
 * situational multipliers (kneel ×1.15, one-handed a two-handed weapon ×0.8,
 * fatal firing-arm wounds, smoke). See {@code Ruleset1994Accuracy} and DESIGN.md.
 */
public interface AccuracyModel {

    /**
     * @return the final to-hit percentage (0..∞ before clamping; a display layer
     *         typically clamps to 0..100). Returns 0 for an unsupported mode.
     */
    int hitChancePercent(WeaponDef weapon, FireMode mode, ShotContext ctx);
}

package com.whim.xcom.rules.model;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * 1994 TU-cost model.
 *
 * <ul>
 *   <li>Walk orthogonal = terrain move cost (standard ground = 4).</li>
 *   <li>Walk diagonal   = round(orthogonal × 1.5) (→ 6 on standard ground).</li>
 *   <li>Turn            = 1 TU per 45° step.</li>
 *   <li>Kneel down      = 4 TU; stand up = 8 TU (flagged assumptions — see DESIGN.md).</li>
 *   <li>Fire            = round(maxTU × weapon.tuPercent(mode) / 100).</li>
 * </ul>
 *
 * Source: UFOpaedia "Time Units" and per-weapon pages.
 */
public final class Ruleset1994TimeUnits implements TimeUnitModel {

    private static final int TURN_TU_PER_STEP = 1;
    private static final int KNEEL_TU = 4;
    private static final int STAND_TU = 8;

    @Override
    public int walkCost(int terrainMoveCost) {
        return terrainMoveCost;
    }

    @Override
    public int walkDiagonalCost(int terrainMoveCost) {
        return (int) Math.round(terrainMoveCost * 1.5);
    }

    @Override
    public int turnCost(int fortyFiveDegreeSteps) {
        return Math.max(0, fortyFiveDegreeSteps) * TURN_TU_PER_STEP;
    }

    @Override
    public int kneelCost() {
        return KNEEL_TU;
    }

    @Override
    public int standCost() {
        return STAND_TU;
    }

    @Override
    public int fireCost(WeaponDef weapon, FireMode mode, int shooterMaxTU) {
        if (weapon == null || !weapon.supports(mode)) {
            return 0;
        }
        return (int) Math.round(shooterMaxTU * (double) weapon.tuPercent(mode) / 100.0);
    }
}

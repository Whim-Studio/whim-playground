package com.whim.xcom.rules.model;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * TU (Time Unit) costs of battlescape actions. 1994 values (see DESIGN.md):
 * walking an orthogonal tile costs the terrain's move cost (4 on standard
 * ground), a diagonal step costs ×1.5 (→ 6), firing costs a percentage of the
 * unit's <em>maximum</em> TUs published by the weapon, kneeling/standing and
 * turning are small fixed costs.
 */
public interface TimeUnitModel {

    /** TU to walk one orthogonal tile of the given terrain move cost (base 4). */
    int walkCost(int terrainMoveCost);

    /** TU to walk one diagonal tile of the given terrain move cost (×1.5 of orthogonal). */
    int walkDiagonalCost(int terrainMoveCost);

    /** TU to turn by the given number of 45° facing steps. */
    int turnCost(int fortyFiveDegreeSteps);

    /** TU to kneel down. */
    int kneelCost();

    /** TU to stand up from kneeling. */
    int standCost();

    /** TU cost of a shot: {@code round(maxTU × weapon.tuPercent(mode) / 100)}. */
    int fireCost(WeaponDef weapon, FireMode mode, int shooterMaxTU);
}

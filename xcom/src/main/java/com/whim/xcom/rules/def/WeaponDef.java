package com.whim.xcom.rules.def;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.model.FireMode;

/**
 * A battlescape weapon. Accuracy multipliers and TU costs are published per
 * {@link FireMode}; a mode a weapon does not support returns {@code 0}.
 *
 * <p>Values follow the 1994 tables (see DESIGN.md). Example — Rifle:
 * Snap 60% acc / 25% TU, Aimed 110% acc / 80% TU, Auto 35% acc / 35% TU.</p>
 */
public interface WeaponDef extends GameDef {

    /** Nominal weapon power (feeds the {@code DamageModel} 0..200% roll). */
    int power();

    DamageType damageType();

    /** {@code true} if the weapon requires two hands (one-handed use is penalised). */
    boolean twoHanded();

    /** Weight in inventory units (affects encumbrance / TU). */
    int weight();

    /** Whether this mode is available on the weapon. */
    boolean supports(FireMode mode);

    /** Weapon accuracy multiplier for the mode, as a percentage (e.g. 110 for Rifle Aimed). */
    int accuracyPercent(FireMode mode);

    /** TU cost of the shot as a percentage of the shooter's maximum TUs (e.g. 80 for Rifle Aimed). */
    int tuPercent(FireMode mode);

    /** Shots fired per activation (1 for snap/aimed, e.g. 3 for auto). */
    int shots(FireMode mode);

    /** Ammo capacity of the loaded clip, or {@code 0} for a weapon that needs no ammo. */
    int clipSize();
}

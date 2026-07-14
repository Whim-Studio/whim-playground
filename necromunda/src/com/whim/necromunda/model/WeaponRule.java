package com.whim.necromunda.model;

/**
 * Functional special-rule flags a {@link Weapon} may carry. Behaviour is driven
 * entirely by these flags interpreted by the combat rules — there are no weapon
 * subclasses. Names are mechanical/functional, not trademarked.
 */
public enum WeaponRule {
    /** Usable in close combat as well as at range; short-ranged. */
    PISTOL,
    /** Roll extra dice for multiple hits, at a jam/ammo risk. */
    RAPID_FIRE,
    /** Auto-hits everything under a blast/teardrop marker; no to-hit roll. */
    TEMPLATE,
    /** Cannot move and fire in the same turn (heavy weapons). */
    MOVE_OR_FIRE,
    /** A hit pushes the target back and may pin it. */
    KNOCKBACK,
    /** Lets the defender force one attack re-roll in melee. */
    PARRY,
    /** Certain to-hit results force a roll to see if the weapon runs dry. */
    AMMO_CHECK,
    /** A dedicated close-combat weapon (no ranged profile). */
    MELEE,
    /** Ignores armour partially/fully via high AP; marker flag for UI. */
    UNWIELDY;
}

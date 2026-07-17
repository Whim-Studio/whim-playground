package com.whim.xcom.model;

/**
 * 1994 damage types. Armour resistances and race vulnerabilities are expressed
 * per type; the {@code Ruleset} owns the resistance tables so variants can retune
 * them without touching engine code.
 */
public enum DamageType {
    ARMOR_PIERCING,
    INCENDIARY,
    HIGH_EXPLOSIVE,
    LASER,
    PLASMA,
    STUN,
    MELEE,
    ACID,
    SMOKE
}

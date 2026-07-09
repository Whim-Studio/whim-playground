package com.whim.alganon.api;

import com.whim.alganon.api.Enums.DamageType;

/**
 * Combat-facing view of any fighter (the player or a spawned mob). Implemented by
 * Task 1's character model and mob entities; consumed by Task 2's combat engine so
 * the same damage math applies to both sides.
 */
public interface Combatant {
    String name();
    boolean isPlayer();
    int hp();
    int maxHp();
    boolean alive();
    int attackPower();
    int defense();
    /** Apply raw incoming damage after the engine has chosen it; model applies mitigation. Returns damage actually dealt. */
    int takeDamage(int amount, DamageType type);
    void heal(int amount);
}

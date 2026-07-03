package com.arpg.model;

/**
 * What an {@link Ability} does when it resolves. The engine reads this to decide
 * how to apply the ability's magnitude.
 */
public enum EffectType {
    DAMAGE,
    HEAL,
    BUFF,
    DEBUFF,
    SUMMON
}

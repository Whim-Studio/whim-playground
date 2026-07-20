package com.heroquest.model;

/**
 * A single-use spell card. The {@code effect} describes how it resolves; the
 * logic layer interprets these fields when a Hero casts.
 */
public final class Spell {
    /** How the spell resolves when cast. */
    public enum Effect {
        DAMAGE,   // inflicts Body Points on a target (bypasses defence dice)
        HEAL,     // restores Body Points to a target Hero
        DEFEND,   // grants extra defend dice for one attack
        PASS      // utility / narrative; no combat effect
    }

    private final String name;
    private final SpellElement element;
    private final Effect effect;
    private final int magnitude; // dice or points, depending on effect

    public Spell(String name, SpellElement element, Effect effect, int magnitude) {
        this.name = name;
        this.element = element;
        this.effect = effect;
        this.magnitude = magnitude;
    }

    public String getName() {
        return name;
    }

    public SpellElement getElement() {
        return element;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getMagnitude() {
        return magnitude;
    }

    @Override
    public String toString() {
        return name + " (" + element + ")";
    }
}

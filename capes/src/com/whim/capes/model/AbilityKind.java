package com.whim.capes.model;

/**
 * The four flavors of Ability (rulebook p.69). A given character has only
 * three of the four columns:
 * <ul>
 *   <li>Super-powered character: POWER + ATTITUDE + STYLE</li>
 *   <li>Mundane character:       SKILL + ATTITUDE + STYLE</li>
 * </ul>
 * POWER is always super-powered (costs a Debt Token per use). SKILL and
 * ATTITUDE are always mundane (Block: usable once per Scene, no resource cost).
 * STYLE may be either, decided at character creation ({@link #superPowered}).
 */
public enum AbilityKind {
    POWER(true),
    SKILL(false),
    ATTITUDE(false),
    STYLE(false); // default mundane; a Style bundled with a Power-Set is marked super at build time

    private final boolean alwaysSuper;

    AbilityKind(boolean alwaysSuper) {
        this.alwaysSuper = alwaysSuper;
    }

    /** True if abilities of this kind are always super-powered regardless of context. */
    public boolean alwaysSuper() { return alwaysSuper; }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}

package com.whim.capes.model;

/**
 * One Ability on a character sheet (p.69). Abilities are ranked 1-up within
 * their column; the {@link #score} is that rank and gates what the Ability may
 * affect: it must be &ge; the value of the die or Inspiration being acted on
 * (p.38).
 *
 * <p>{@code superPowered} is resolved at creation time: POWER abilities are
 * always super; STYLE abilities inherit super/mundane from the module they
 * came bundled with (a Style in a Power-Set is super; a Style in a
 * Persona/Skill-Set is mundane, p.69).
 */
public final class Ability implements java.io.Serializable {
    private String name;
    private final AbilityKind kind;
    private int score;
    private final boolean superPowered;

    public Ability(String name, AbilityKind kind, int score, boolean superPowered) {
        this.name = name;
        this.kind = kind;
        this.score = score;
        this.superPowered = superPowered;
    }

    public String name() { return name; }
    public void rename(String name) { this.name = name; }

    public AbilityKind kind() { return kind; }
    public int score() { return score; }
    public void setScore(int score) { this.score = score; }

    /** True if using this Ability earns a Debt Token (Power, or a Style from a Power-Set). */
    public boolean isSuperPowered() { return superPowered; }

    /** True if this Ability can affect a die/Inspiration of the given value (score must be &ge; value). */
    public boolean canAffect(int value) { return score >= value; }

    @Override public String toString() { return score + ". " + name + " (" + kind.displayName() + ")"; }
}

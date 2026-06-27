package com.whim.tacticalnexus.engine;

/**
 * Immutable result of a deterministic combat calculation between the player and
 * an enemy. Produced by {@link CombatCalculator#resolve}. Pure data — no rules,
 * no Swing.
 */
public final class CombatResult {

    private final boolean canFight;
    private final int hitsToKill;
    private final int hpLost;
    private final boolean survivable;

    public CombatResult(boolean canFight, int hitsToKill, int hpLost, boolean survivable) {
        this.canFight = canFight;
        this.hitsToKill = hitsToKill;
        this.hpLost = hpLost;
        this.survivable = survivable;
    }

    /** false ⇒ player ATK ≤ enemy DEF ⇒ enemy unkillable ⇒ move blocked. */
    public boolean canFight() {
        return canFight;
    }

    /** Number of player strikes required to kill the enemy (0 when !canFight). */
    public int hitsToKill() {
        return hitsToKill;
    }

    /** Total HP the player loses if the fight happens. */
    public int hpLost() {
        return hpLost;
    }

    /** true ⇒ hpLost is strictly less than the player's current HP. */
    public boolean survivable() {
        return survivable;
    }

    @Override
    public String toString() {
        return "CombatResult{canFight=" + canFight
                + ", hitsToKill=" + hitsToKill
                + ", hpLost=" + hpLost
                + ", survivable=" + survivable + "}";
    }
}

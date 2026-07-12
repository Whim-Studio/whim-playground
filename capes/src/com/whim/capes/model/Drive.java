package com.whim.capes.model;

/**
 * A moral Drive on a character, with a Strength (1-5) and the Debt Tokens
 * currently resting on it (pp.32-36).
 *
 * <p>Invariants enforced elsewhere by the engine:
 * <ul>
 *   <li>Debt Staked on a Conflict is NOT held here (p.32) — this counter is
 *       only the Debt "at rest" on the Drive.</li>
 *   <li>The Drive is {@link #isOverdrawn() Overdrawn} when debt &gt; strength.</li>
 *   <li>A single Stake may move at most {@code strength} Debt off this Drive
 *       (p.36).</li>
 * </ul>
 */
public final class Drive implements java.io.Serializable {
    private final DriveType type;
    private int strength;   // 1-5
    private int debt;       // tokens at rest on this Drive (excludes Staked debt)

    public Drive(DriveType type, int strength) {
        this.type = type;
        this.strength = strength;
        this.debt = 0;
    }

    public DriveType type() { return type; }
    public int strength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }

    public int debt() { return debt; }

    public void addDebt(int n) { debt += n; }

    public void removeDebt(int n) {
        debt -= n;
        if (debt < 0) debt = 0;
    }

    /** Overdrawn when the Drive holds more Debt Tokens than its Strength (p.32). */
    public boolean isOverdrawn() { return debt > strength; }

    @Override public String toString() {
        return type.displayName() + " " + strength + " [" + debt + " debt]";
    }
}

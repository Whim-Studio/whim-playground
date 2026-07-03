package com.whim.powermonger.domain;

/**
 * The Balance of Power tracked in [-1, +1]. Positive favours the player.
 * Town captures and enemy captain eliminations shift it toward the player;
 * losses shift it toward the enemy.
 */
public final class BalanceOfPower {

    /** Default nudge applied when a town changes hands. */
    public static final double TOWN_SHIFT = 0.12;
    /** Default nudge applied when a captain is eliminated. */
    public static final double CAPTAIN_SHIFT = 0.20;

    private double value; // -1..+1

    public BalanceOfPower() { this(0.0); }

    public BalanceOfPower(double initial) { this.value = clamp(initial); }

    public double value() { return value; }

    /** Directly set (clamped). */
    public void set(double v) { this.value = clamp(v); }

    /** Apply a raw shift (clamped). Positive = toward player. */
    public void shift(double delta) { this.value = clamp(this.value + delta); }

    /** A town was captured. playerGained=true when the player took it. */
    public void onTownCaptured(boolean playerGained) {
        shift(playerGained ? TOWN_SHIFT : -TOWN_SHIFT);
    }

    /** A captain was eliminated. enemyCaptain=true when it was an enemy's. */
    public void onCaptainEliminated(boolean enemyCaptain) {
        shift(enemyCaptain ? CAPTAIN_SHIFT : -CAPTAIN_SHIFT);
    }

    public boolean playerVictory() { return value >= 1.0; }
    public boolean playerDefeat() { return value <= -1.0; }

    private static double clamp(double v) {
        if (v < -1.0) return -1.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}

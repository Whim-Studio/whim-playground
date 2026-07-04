package com.rampart.model;

/**
 * Class of enemy vessel. Larger ships carry more health and score more when sunk;
 * the engine (Task 2) decides which types spawn in which round and how they move
 * and fire. The numeric baselines here are data hints, not behavior.
 */
public enum ShipType {
    /** Small, fast scout. */
    SLOOP(2, 100, 1.0),
    /** Mid-size warship. */
    FRIGATE(4, 250, 0.7),
    /** Large, slow, heavily-armed galleon. */
    GALLEON(6, 500, 0.5);

    private final int baseHealth;
    private final int scoreValue;
    private final double baseSpeed;

    ShipType(int baseHealth, int scoreValue, double baseSpeed) {
        this.baseHealth = baseHealth;
        this.scoreValue = scoreValue;
        this.baseSpeed = baseSpeed;
    }

    /** Suggested starting hit points for a fresh ship of this type. */
    public int baseHealth() { return baseHealth; }

    /** Points awarded when a ship of this type is destroyed. */
    public int scoreValue() { return scoreValue; }

    /** Suggested movement speed in cells per second (engine may scale by round). */
    public double baseSpeed() { return baseSpeed; }
}

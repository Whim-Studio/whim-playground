package com.whim.xcom.rules.def;

/**
 * An alien race/rank template. The base stats below are the Beginner-difficulty
 * values; the {@code Ruleset} applies per-difficulty scaling on top.
 */
public interface AlienDef extends GameDef {

    int timeUnits();

    int stamina();

    int health();

    int reactions();

    int firingAccuracy();

    int strength();

    /** Psionic strength (0 if the race has no psi ability). */
    int psiStrength();

    /** Innate armour on each facing when the race has no wearable armour item. */
    int frontArmor();

    /** Points awarded to the Council when this alien is killed/captured. */
    int scoreValue();
}

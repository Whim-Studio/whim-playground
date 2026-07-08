package com.whim.necromunda.engine.rules;

/**
 * The to-wound table, cross-referencing attack Strength against target Toughness:
 *
 * <pre>
 *   S >= 2*T          -> 2+
 *   S >  T            -> 3+
 *   S == T            -> 4+
 *   S == T - 1        -> 5+
 *   S <= T - 2        -> 6+   (also the floor for anything far below)
 * </pre>
 *
 * There is no "cannot wound" band — even a wildly outmatched hit wounds on a 6,
 * and a natural 6 always wounds.
 */
public final class WoundTable {

    private WoundTable() {
    }

    /** Minimum D6 needed to wound: 2..6. */
    public static int target(int strength, int toughness) {
        if (strength >= 2 * toughness) {
            return 2;
        }
        if (strength > toughness) {
            return 3;
        }
        if (strength == toughness) {
            return 4;
        }
        if (strength == toughness - 1) {
            return 5;
        }
        return 6;
    }

    /** Whether a die roll wounds a given target number (natural 6 always wounds). */
    public static boolean wounds(int roll, int target) {
        if (roll >= 6) {
            return true;
        }
        return roll >= target;
    }
}

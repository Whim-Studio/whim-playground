package com.whim.capes.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure, Swing-free rules mathematics — the decoupled core the constraints
 * require to be unit-testable independent of the UI. Every method here is a
 * deterministic function of its inputs (no RNG, no mutation of model objects).
 *
 * <p>Covers the arithmetic pieces flagged as hardest to retrofit: even
 * Splitting, Overdraw detection, and Inspiration pairing on Resolve
 * (pp.30-31, 36-37). The stateful engine (Phase 3) will call into these.
 */
public final class RulesMath {
    private RulesMath() {}

    /** A Drive/stack is Overdrawn when Debt exceeds Strength (p.32). */
    public static boolean isOverdrawn(int debt, int strength) {
        return debt > strength;
    }

    /**
     * Splits a die of {@code value} into {@code parts} dice "as evenly as
     * possible" (p.37): the remainder is spread one-per-part across the largest
     * parts. E.g. 5 into 2 -&gt; {3,2}; 7 into 3 -&gt; {3,2,2}. Each resulting
     * die is at least 1, so {@code parts} may not exceed {@code value}.
     */
    public static int[] evenSplit(int value, int parts) {
        if (parts < 1) throw new IllegalArgumentException("parts must be >= 1");
        if (parts > value) throw new IllegalMoveException("Cannot split " + value + " into " + parts + " dice (min 1 each).");
        int base = value / parts;
        int remainder = value % parts;
        int[] out = new int[parts];
        for (int i = 0; i < parts; i++) {
            out[i] = base + (i < remainder ? 1 : 0); // larger parts first
        }
        return out;
    }

    /** Result of pairing dice on Resolve: Inspirations for the Resolver and for the opposing side. */
    public static final class InspirationSplit {
        public final List<Integer> resolverInspirations;
        public final List<Integer> opposingInspirations;
        public InspirationSplit(List<Integer> resolver, List<Integer> opposing) {
            this.resolverInspirations = resolver;
            this.opposingInspirations = opposing;
        }
    }

    /**
     * Pairs winning vs losing dice highest-to-highest and yields Inspirations
     * (p.30):
     * <ul>
     *   <li>Each pair: difference (winner - loser). Positive &rarr; Resolver
     *       Inspiration; negative &rarr; Inspiration for the opposing side;
     *       zero &rarr; none.</li>
     *   <li>Unpaired excess WINNING dice &rarr; Resolver Inspiration of full
     *       value. Unpaired excess losing dice yield nothing (§Ambiguity A3).</li>
     * </ul>
     * Inputs are copied and sorted descending; callers pass raw face values.
     */
    public static InspirationSplit pairInspirations(List<Integer> winningDice, List<Integer> losingDice) {
        List<Integer> win = new ArrayList<Integer>(winningDice);
        List<Integer> lose = new ArrayList<Integer>(losingDice);
        Collections.sort(win, Collections.reverseOrder());
        Collections.sort(lose, Collections.reverseOrder());

        List<Integer> resolver = new ArrayList<Integer>();
        List<Integer> opposing = new ArrayList<Integer>();

        int pairs = Math.min(win.size(), lose.size());
        for (int i = 0; i < pairs; i++) {
            int diff = win.get(i) - lose.get(i);
            if (diff > 0) resolver.add(diff);
            else if (diff < 0) opposing.add(-diff);
            // diff == 0 -> no Inspiration (p.30)
        }
        for (int i = pairs; i < win.size(); i++) {
            resolver.add(win.get(i)); // excess winning die -> full value
        }
        return new InspirationSplit(resolver, opposing);
    }

    /**
     * Deadlock predicate (p.30): a tied Conflict is Deadlocked when no future
     * tie-break is possible — nobody can Stake more Debt and every die is
     * already at the maximum face.
     */
    public static boolean isDeadlocked(boolean tied, boolean anyDebtAvailable, boolean allDiceMaxed) {
        return tied && !anyDebtAvailable && allDiceMaxed;
    }
}

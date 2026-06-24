package com.whim.coda.engine;

import com.whim.coda.model.Attribute;
import com.whim.coda.model.AttributeSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Random and point-buy attribute generation for the Coda character creator.
 *
 * <p>Random: roll 2d6 nine times, drop the three lowest, return the six kept
 * scores ordered high to low. No exploding dice.</p>
 */
public final class AttributeGenerator {

    private AttributeGenerator() {
    }

    /**
     * Point-buy validation: each base must be within {@code 2..12} and the total
     * spent within {@link #POINT_BUY_BUDGET}.
     */
    public static int POINT_BUY_BUDGET = 46; // sum of standard example array (10+9+7+7+5+4)+8

    /** Lowest legal base score for both random keep and point-buy. */
    public static final int MIN_BASE = 2;
    /** Highest legal starting base score (pre-species cap). */
    public static final int MAX_BASE = 12;

    /**
     * RANDOM: roll 2d6 nine times, DROP the three lowest, return the six kept
     * scores (high to low). NO exploding dice. Caller assigns them to attributes.
     */
    public static List<Integer> rollScores(Random rng) {
        if (rng == null) {
            throw new IllegalArgumentException("rng must not be null");
        }
        List<Integer> rolls = new ArrayList<Integer>(9);
        for (int i = 0; i < 9; i++) {
            int d1 = rng.nextInt(6) + 1;
            int d2 = rng.nextInt(6) + 1;
            rolls.add(d1 + d2);
        }
        // Sort high -> low, then keep the first six (drops the three lowest).
        Collections.sort(rolls, Collections.reverseOrder());
        List<Integer> kept = new ArrayList<Integer>(6);
        for (int i = 0; i < 6; i++) {
            kept.add(rolls.get(i));
        }
        return kept;
    }

    /**
     * Point-buy validation: each base must be {@code 2..12} and the total spent
     * within {@link #POINT_BUY_BUDGET}. Total spent is the plain sum of the six
     * base scores.
     */
    public static boolean validatePointBuy(AttributeSet attrs) {
        if (attrs == null) {
            return false;
        }
        int total = 0;
        for (Attribute a : Attribute.values()) {
            int base = attrs.getBase(a);
            if (base < MIN_BASE || base > MAX_BASE) {
                return false;
            }
            total += base;
        }
        return total <= POINT_BUY_BUDGET;
    }
}

package com.whim.digitallife.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * A single multiple-choice answer option.
 *
 * <p>Every choice carries a display label plus a set of trait weights. When the
 * user selects a choice, each weight is added to the running score for that
 * {@link Trait}. Weights are typically small positive integers (0-2); a choice
 * may contribute to several traits at once.</p>
 */
public final class Choice {

    private final String label;
    private final Map<Trait, Integer> weights;

    /**
     * @param label   the text shown on the answer button
     * @param weights trait contributions applied when this choice is selected
     */
    public Choice(String label, Map<Trait, Integer> weights) {
        this.label = label;
        this.weights = new EnumMap<Trait, Integer>(weights);
    }

    /** @return the text shown to the user for this option. */
    public String getLabel() {
        return label;
    }

    /** @return an unmodifiable view of the trait weights for this choice. */
    public Map<Trait, Integer> getWeights() {
        return Collections.unmodifiableMap(weights);
    }

    /**
     * Convenience factory for building a choice with a fluent weight list.
     *
     * @param label       the option text
     * @param traitPoints alternating {@link Trait}/Integer pairs
     * @return a new {@link Choice}
     */
    public static Choice of(String label, Object... traitPoints) {
        Map<Trait, Integer> map = new EnumMap<Trait, Integer>(Trait.class);
        for (int i = 0; i + 1 < traitPoints.length; i += 2) {
            Trait trait = (Trait) traitPoints[i];
            Integer points = (Integer) traitPoints[i + 1];
            map.put(trait, points);
        }
        return new Choice(label, map);
    }
}

package com.whim.hbdi.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable {@link Question} implementation. */
public final class DefaultQuestion implements Question {

    private final int id;
    private final String text;
    private final String category;
    private final Map<Quadrant, Integer> weights;

    public DefaultQuestion(int id, String text, String category, Map<Quadrant, Integer> weights) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        if (weights == null) {
            throw new IllegalArgumentException("weights must not be null");
        }
        EnumMap<Quadrant, Integer> copy = new EnumMap<Quadrant, Integer>(Quadrant.class);
        for (Quadrant q : Quadrant.values()) {
            Integer w = weights.get(q);
            int value = (w == null) ? 0 : w.intValue();
            if (value < 0) {
                throw new IllegalArgumentException("weight must be >= 0 for quadrant " + q);
            }
            copy.put(q, Integer.valueOf(value));
        }
        this.id = id;
        this.text = text;
        this.category = category;
        this.weights = Collections.unmodifiableMap(copy);
    }

    /** Convenience constructor taking the four weights positionally (A, B, C, D). */
    public DefaultQuestion(int id, String category, String text,
                           int weightA, int weightB, int weightC, int weightD) {
        this(id, text, category, buildWeights(weightA, weightB, weightC, weightD));
    }

    private static Map<Quadrant, Integer> buildWeights(int a, int b, int c, int d) {
        EnumMap<Quadrant, Integer> m = new EnumMap<Quadrant, Integer>(Quadrant.class);
        m.put(Quadrant.A, Integer.valueOf(a));
        m.put(Quadrant.B, Integer.valueOf(b));
        m.put(Quadrant.C, Integer.valueOf(c));
        m.put(Quadrant.D, Integer.valueOf(d));
        return m;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getCategory() {
        return category;
    }

    public Map<Quadrant, Integer> getQuadrantWeights() {
        return weights;
    }

    @Override
    public String toString() {
        return "Question#" + id + "[" + category + "] " + text;
    }
}

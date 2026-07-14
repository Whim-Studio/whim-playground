package com.whim.stars.model;

import java.io.Serializable;

/**
 * A player's current level in each of the six {@link TechField}s (0..26).
 * Levels are the gate for which hull types and components can be designed and
 * which planetary installations are available.
 */
public final class TechLevels implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Highest level any single field can reach in the original game. */
    public static final int MAX_LEVEL = 26;

    private final int[] levels = new int[TechField.values().length];

    public TechLevels() {
    }

    public int get(TechField field) {
        return levels[field.ordinal()];
    }

    public void set(TechField field, int level) {
        levels[field.ordinal()] = clamp(level);
    }

    public void increment(TechField field) {
        set(field, get(field) + 1);
    }

    /** Sum of all six field levels — used by the research cost curve. */
    public int total() {
        int sum = 0;
        for (int l : levels) {
            sum += l;
        }
        return sum;
    }

    /** True if this tech meets a per-field requirement map. */
    public boolean meets(TechLevels requirement) {
        for (TechField f : TechField.values()) {
            if (get(f) < requirement.get(f)) {
                return false;
            }
        }
        return true;
    }

    private static int clamp(int level) {
        if (level < 0) return 0;
        if (level > MAX_LEVEL) return MAX_LEVEL;
        return level;
    }

    public TechLevels copy() {
        TechLevels t = new TechLevels();
        System.arraycopy(this.levels, 0, t.levels, 0, this.levels.length);
        return t;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TechField f : TechField.values()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(f.name().charAt(0)).append(get(f));
        }
        return sb.toString();
    }
}

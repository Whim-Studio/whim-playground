package com.whim.b5wars.data;

/** Critical-hit table row (data-driven). {@code effect} strings are engine-interpreted. */
public final class CriticalEntry {
    private final int rollMin;
    private final int rollMax;
    private final String effect;

    public CriticalEntry(int rollMin, int rollMax, String effect) {
        this.rollMin = rollMin;
        this.rollMax = rollMax;
        this.effect = effect;
    }

    public int getRollMin() {
        return rollMin;
    }

    public int getRollMax() {
        return rollMax;
    }

    /** e.g. "REACTOR", "ENGINE", "WEAPON", "SENSOR", "CREW", "NONE". */
    public String getEffect() {
        return effect;
    }
}

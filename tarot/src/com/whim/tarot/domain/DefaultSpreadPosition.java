package com.whim.tarot.domain;

/** Immutable concrete {@link SpreadPosition}. */
public final class DefaultSpreadPosition implements SpreadPosition {
    private final int index;
    private final String name;
    private final String meaning;

    public DefaultSpreadPosition(int index, String name, String meaning) {
        this.index = index;
        this.name = name;
        this.meaning = meaning;
    }

    public int getIndex() { return index; }
    public String getName() { return name; }
    public String getMeaning() { return meaning; }

    @Override
    public String toString() { return index + ": " + name; }
}

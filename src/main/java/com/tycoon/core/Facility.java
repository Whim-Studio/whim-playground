package com.tycoon.core;

/**
 * A placed object on the floor grid.
 */
public final class Facility {
    private final FacilityType type;
    private final GridPos pos;

    private Facility(FacilityType type, GridPos pos) {
        this.type = type;
        this.pos = pos;
    }

    public static Facility at(FacilityType type, GridPos pos) {
        return new Facility(type, pos);
    }

    public FacilityType type() {
        return type;
    }

    public GridPos pos() {
        return pos;
    }

    @Override
    public String toString() {
        return type + "@" + pos;
    }
}

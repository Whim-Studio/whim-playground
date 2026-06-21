package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public final class Facility {
    private final FacilityType type;
    private final GridPos pos;
    private Facility(FacilityType type, GridPos pos) { this.type = type; this.pos = pos; }
    public static Facility at(FacilityType type, GridPos pos) { return new Facility(type, pos); }
    public FacilityType type() { return type; }
    public GridPos pos() { return pos; }
}

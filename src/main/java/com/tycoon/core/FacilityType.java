package com.tycoon.core;

/**
 * Kinds of placeable objects. DESK is the only workstation; the rest provide
 * hourly stress relief within their room.
 */
public enum FacilityType {
    DESK(500, 0.0, true),            // workstation; required for an employee to produce points
    COFFEE_MACHINE(300, 2.0, false), // stress recovery
    HEATER(150, 1.0, false),         // stress recovery
    PLANT(80, 0.5, false),           // stress recovery
    ARCADE_CABINET(900, 3.0, false); // stress recovery

    private final int cost;
    private final double stressRelief;
    private final boolean workstation;

    FacilityType(int cost, double stressRelief, boolean workstation) {
        this.cost = cost;
        this.stressRelief = stressRelief;
        this.workstation = workstation;
    }

    public int cost() {
        return cost;
    }

    public double stressRelief() {
        return stressRelief;
    }

    public boolean isWorkstation() {
        return workstation;
    }
}

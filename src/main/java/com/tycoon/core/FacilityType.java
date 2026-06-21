package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public enum FacilityType {
    DESK(500, 0.0),
    COFFEE_MACHINE(300, 2.0),
    HEATER(200, 1.0),
    PLANT(100, 0.5),
    ARCADE_CABINET(800, 3.0);
    private final int cost;
    private final double relief;
    FacilityType(int cost, double relief) { this.cost = cost; this.relief = relief; }
    public int cost() { return cost; }
    public double stressRelief() { return relief; }
    public boolean isWorkstation() { return this == DESK; }
}

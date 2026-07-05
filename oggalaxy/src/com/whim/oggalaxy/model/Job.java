package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;

/**
 * A single queued production job — a building upgrade, a research, or a batch of ships
 * or defenses. Implements {@link Views.QueueItemView} so the UI can render it directly.
 * The engine owns all timing: it decrements {@link #remainingTicks} each tick and, for
 * ship/defense batches, pops one finished unit every {@link #unitTicks} ticks.
 */
public final class Job implements Views.QueueItemView, Serializable {

    private static final long serialVersionUID = 1L;

    public enum Kind { BUILDING, RESEARCH, SHIP, DEFENSE }

    public Kind kind;
    public String label;
    public int totalTicks;
    public int remainingTicks;

    // completion metadata (only the field matching kind is used)
    public Ids.BuildingType building;
    public Ids.TechType tech;
    public Ids.ShipType ship;
    public Ids.DefenseType defense;
    public String labPlanetId;      // research: which lab planet paid/timed it

    // ship / defense batch bookkeeping
    public int count;               // units still to be produced
    public int unitTicks;           // ticks to build one unit
    public int unitRemaining;       // ticks left on the current unit

    public Job() {
    }

    public static Job building(Ids.BuildingType type, String label, int ticks) {
        Job j = new Job();
        j.kind = Kind.BUILDING;
        j.building = type;
        j.label = label;
        j.totalTicks = ticks;
        j.remainingTicks = ticks;
        return j;
    }

    public static Job research(Ids.TechType type, String label, int ticks, String labPlanetId) {
        Job j = new Job();
        j.kind = Kind.RESEARCH;
        j.tech = type;
        j.label = label;
        j.totalTicks = ticks;
        j.remainingTicks = ticks;
        j.labPlanetId = labPlanetId;
        return j;
    }

    public static Job shipBatch(Ids.ShipType type, String label, int count, int unitTicks) {
        Job j = new Job();
        j.kind = Kind.SHIP;
        j.ship = type;
        j.label = label;
        j.count = count;
        j.unitTicks = Math.max(1, unitTicks);
        j.unitRemaining = j.unitTicks;
        j.totalTicks = j.unitTicks * count;
        j.remainingTicks = j.totalTicks;
        return j;
    }

    public static Job defenseBatch(Ids.DefenseType type, String label, int count, int unitTicks) {
        Job j = new Job();
        j.kind = Kind.DEFENSE;
        j.defense = type;
        j.label = label;
        j.count = count;
        j.unitTicks = Math.max(1, unitTicks);
        j.unitRemaining = j.unitTicks;
        j.totalTicks = j.unitTicks * count;
        j.remainingTicks = j.totalTicks;
        return j;
    }

    public boolean isBatch() {
        return kind == Kind.SHIP || kind == Kind.DEFENSE;
    }

    @Override public String label() { return label; }
    @Override public int totalTicks() { return totalTicks; }
    @Override public int remainingTicks() { return Math.max(0, remainingTicks); }
    @Override public double progressFraction() {
        if (totalTicks <= 0) return 1.0;
        double f = 1.0 - (double) Math.max(0, remainingTicks) / totalTicks;
        if (f < 0) return 0;
        if (f > 1) return 1;
        return f;
    }
}

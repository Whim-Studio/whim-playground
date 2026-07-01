package com.whim.warroom.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable render frame published by the engine for a single tick. The UI
 * reads these to draw; it never mutates domain objects while simulating.
 */
public final class SimSnapshot {

    /** Immutable per-unit view for one frame. */
    public static final class UnitView {
        public final int id;
        public final String typeId;
        public final Faction faction;
        public final double x;
        public final double y;
        public final double heading;
        public final double health;
        public final double maxHealth;
        public final double morale;
        public final double maxMorale;
        public final Stance stance;
        public final boolean routed;
        public final boolean alive;

        public UnitView(int id, String typeId, Faction faction, double x, double y,
                        double heading, double health, double maxHealth, double morale,
                        double maxMorale, Stance stance, boolean routed, boolean alive) {
            this.id = id;
            this.typeId = typeId;
            this.faction = faction;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.health = health;
            this.maxHealth = maxHealth;
            this.morale = morale;
            this.maxMorale = maxMorale;
            this.stance = stance;
            this.routed = routed;
            this.alive = alive;
        }
    }

    /** Immutable per-blast view; {@code age} runs 0..1 for a fade animation. */
    public static final class BlastView {
        public final double x;
        public final double y;
        public final double radius;
        public final double age;

        public BlastView(double x, double y, double radius, double age) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.age = age;
        }
    }

    private final int tick;
    private final List<UnitView> units;
    private final List<BlastView> blasts;
    private final boolean finished;

    public SimSnapshot(int tick, List<UnitView> units, List<BlastView> blasts, boolean finished) {
        this.tick = tick;
        this.units = Collections.unmodifiableList(
                new ArrayList<UnitView>(units == null ? new ArrayList<UnitView>() : units));
        this.blasts = Collections.unmodifiableList(
                new ArrayList<BlastView>(blasts == null ? new ArrayList<BlastView>() : blasts));
        this.finished = finished;
    }

    public int getTick() {
        return tick;
    }

    public List<UnitView> getUnits() {
        return units;
    }

    public List<BlastView> getBlasts() {
        return blasts;
    }

    /** True once one side is eliminated (all dead or routed). */
    public boolean isFinished() {
        return finished;
    }
}

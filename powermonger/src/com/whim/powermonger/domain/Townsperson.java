package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Views;

/**
 * An autonomous neutral villager. Position and job are mutable — the engine's
 * life AI drives them. Implements {@link Views.TownspersonView}.
 */
public final class Townsperson implements Views.TownspersonView {

    private final int id;
    private double x;
    private double y;
    private Job job;
    private Allegiance allegiance;

    public Townsperson(int id, double x, double y, Job job, Allegiance allegiance) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.job = job;
        this.allegiance = allegiance;
    }

    // ---- Views.TownspersonView ----
    @Override public int id() { return id; }
    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public Job job() { return job; }
    @Override public Allegiance allegiance() { return allegiance; }

    // ---- mutators (engine) ----
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void setJob(Job job) { this.job = job; }
    public void setAllegiance(Allegiance allegiance) { this.allegiance = allegiance; }
}

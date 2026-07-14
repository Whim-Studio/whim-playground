package com.whim.stars.model;

import java.io.Serializable;

/**
 * A single fleet order: travel to a point (optionally a planet) at a chosen warp
 * and perform a task on arrival. Waypoint 0 of a fleet is its current position;
 * subsequent waypoints are the route.
 */
public final class Waypoint implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Task {
        NONE("None"),
        COLONIZE("Colonize"),
        TRANSPORT("Transport"),
        REMOTE_MINE("Remote Mine"),
        LAY_MINES("Lay Mines"),
        PATROL("Patrol"),
        MERGE("Merge"),
        SCRAP("Scrap");

        private final String label;
        Task(String label) { this.label = label; }
        public String label() { return label; }
    }

    private double x;
    private double y;
    private int warp;
    private Task task;
    private int targetPlanetId = -1;

    public Waypoint(double x, double y, int warp, Task task) {
        this.x = x;
        this.y = y;
        this.warp = warp;
        this.task = task == null ? Task.NONE : task;
    }

    public static Waypoint toPlanet(Planet planet, int warp, Task task) {
        Waypoint w = new Waypoint(planet.x(), planet.y(), warp, task);
        w.targetPlanetId = planet.id();
        return w;
    }

    public double x() { return x; }
    public double y() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public int warp() { return warp; }
    public void setWarp(int warp) { this.warp = Math.max(1, Math.min(warp, 10)); }
    public Task task() { return task; }
    public void setTask(Task task) { this.task = task == null ? Task.NONE : task; }
    public int targetPlanetId() { return targetPlanetId; }
    public void setTargetPlanetId(int id) { this.targetPlanetId = id; }
}

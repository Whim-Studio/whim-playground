package com.tycoon.core;

/**
 * A studio employee with a stress meter, an optional assigned workstation (DESK)
 * and an optional assigned project.
 */
public class Employee {
    private final String id;
    private final String name;
    private final int baseSkill;
    private double stress = 0.0;
    private GridPos workstation = null;
    private String assignedProjectId = null;

    public Employee(String id, String name, int baseSkill) {
        this.id = id;
        this.name = name;
        this.baseSkill = baseSkill;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int baseSkill() {
        return baseSkill;
    }

    /** 0..100 stress meter (σ_stress). */
    public double stress() {
        return stress;
    }

    /** Clamps to [0, 100]. */
    public void setStress(double s) {
        if (s < 0.0) {
            s = 0.0;
        } else if (s > 100.0) {
            s = 100.0;
        }
        this.stress = s;
    }

    /** Assigned DESK position, or null if unassigned. */
    public GridPos workstation() {
        return workstation;
    }

    public void assignWorkstation(GridPos deskPos) {
        this.workstation = deskPos;
    }

    /** Assigned project id, or null if idle. */
    public String assignedProjectId() {
        return assignedProjectId;
    }

    public void assignProject(String projectId) {
        this.assignedProjectId = projectId;
    }
}

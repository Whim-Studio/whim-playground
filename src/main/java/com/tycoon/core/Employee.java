package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public class Employee {
    private final String id, name;
    private final int baseSkill;
    private double stress;
    private GridPos workstation;
    private String assignedProjectId;
    public Employee(String id, String name, int baseSkill) {
        this.id = id; this.name = name; this.baseSkill = baseSkill;
    }
    public String id() { return id; }
    public String name() { return name; }
    public int baseSkill() { return baseSkill; }
    public double stress() { return stress; }
    public void setStress(double s) { stress = Math.max(0.0, Math.min(100.0, s)); }
    public GridPos workstation() { return workstation; }
    public void assignWorkstation(GridPos deskPos) { workstation = deskPos; }
    public String assignedProjectId() { return assignedProjectId; }
    public void assignProject(String projectId) { assignedProjectId = projectId; }
}

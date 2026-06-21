package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.ArrayList;
import java.util.List;
public class GameStudio {
    private final String name;
    private long cash;
    private final FloorPlan floorPlan = new FloorPlan(40, 30);
    private final List<Employee> employees = new ArrayList<Employee>();
    private final List<GameProject> projects = new ArrayList<GameProject>();
    public GameStudio(String name) { this.name = name; }
    public String name() { return name; }
    public long cash() { return cash; }
    public void addCash(long delta) { cash += delta; }
    public FloorPlan floorPlan() { return floorPlan; }
    public List<Employee> employees() { return employees; }
    public List<GameProject> projects() { return projects; }
    public Employee employee(String id) {
        for (Employee e : employees) if (e.id().equals(id)) return e;
        return null;
    }
    public GameProject project(String id) {
        for (GameProject p : projects) if (p.id().equals(id)) return p;
        return null;
    }
}

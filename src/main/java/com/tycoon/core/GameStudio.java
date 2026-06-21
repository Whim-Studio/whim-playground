package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's studio: cash, the floor plan, employees and projects.
 */
public class GameStudio {
    private final String name;
    private long cash = 0L;
    private final FloorPlan floorPlan;
    private final List<Employee> employees = new ArrayList<Employee>();
    private final List<GameProject> projects = new ArrayList<GameProject>();

    public GameStudio(String name) {
        this(name, new FloorPlan(40, 30));
    }

    GameStudio(String name, FloorPlan floorPlan) {
        this.name = name;
        this.floorPlan = floorPlan;
    }

    public String name() {
        return name;
    }

    public long cash() {
        return cash;
    }

    public void addCash(long delta) {
        this.cash += delta;
    }

    public FloorPlan floorPlan() {
        return floorPlan;
    }

    public List<Employee> employees() {
        return employees;
    }

    public List<GameProject> projects() {
        return projects;
    }

    /** @return the employee with the given id, or null. */
    public Employee employee(String id) {
        for (int i = 0; i < employees.size(); i++) {
            Employee e = employees.get(i);
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /** @return the project with the given id, or null. */
    public GameProject project(String id) {
        for (int i = 0; i < projects.size(); i++) {
            GameProject p = projects.get(i);
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }
}

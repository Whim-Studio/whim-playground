package com.whim.colony.domain;

import com.whim.colony.api.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * A colony pawn: a data-only record of position, needs, skills and current
 * assignment. Deliberately contains NO decision logic — the engine (Task 2)
 * reads and mutates these fields; the UI (Task 3) renders them.
 */
public final class Colonist {
    private final int id;
    private String name;
    private int x;
    private int y;
    private final Needs needs;
    private final SkillSet skills;
    private Job currentJob; // nullable

    /**
     * The path the engine has planned for this colonist, as a flat list of
     * (x,y) step coordinates. Populated by the engine's pathfinder; may be empty.
     */
    private final List<int[]> path = new ArrayList<int[]>();

    public Colonist(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.needs = new Needs();
        this.skills = new SkillSet();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    /** Convenience mutator to move the colonist to (x,y) in one call. */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Needs getNeeds() {
        return needs;
    }

    public SkillSet getSkills() {
        return skills;
    }

    public Job getCurrentJob() {
        return currentJob;
    }

    public void setCurrentJob(Job currentJob) {
        this.currentJob = currentJob;
    }

    /** @return the mutable planned-path list; the engine fills/clears this. */
    public List<int[]> getPath() {
        return path;
    }

    public boolean hasPath() {
        return !path.isEmpty();
    }
}

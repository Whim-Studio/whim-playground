package com.whim.xcom.meta;

import com.whim.xcom.rules.def.ResearchNode;

/**
 * An in-progress research project. Progress accrues in scientist-days:
 * {@code scientists × elapsedDays}. When it reaches the node's cost the project
 * completes and its {@link ResearchNode#unlocks()} become available.
 */
public final class ResearchProject {

    private final ResearchNode node;
    private int assignedScientists;
    private double progressScientistDays;
    private boolean complete;

    public ResearchProject(ResearchNode node, int assignedScientists) {
        this.node = node;
        this.assignedScientists = assignedScientists;
    }

    public ResearchNode node() { return node; }
    public String id() { return node.id(); }
    public String name() { return node.name(); }
    public int assignedScientists() { return assignedScientists; }
    public void setAssignedScientists(int n) { this.assignedScientists = Math.max(0, n); }
    public boolean complete() { return complete; }

    /** Raw accrued scientist-days (for save/load). */
    public double progressScientistDays() { return progressScientistDays; }

    /** Restore accrued progress from a save; recomputes completion. */
    public void restoreProgress(double scientistDays) {
        this.progressScientistDays = scientistDays;
        this.complete = scientistDays >= node.researchCost();
    }

    public int percent() {
        if (node.researchCost() <= 0) {
            return 100;
        }
        return (int) Math.min(100, Math.round(progressScientistDays * 100.0 / node.researchCost()));
    }

    /** Advance by elapsed days; returns true the tick it completes. */
    public boolean advance(double days) {
        if (complete) {
            return false;
        }
        progressScientistDays += assignedScientists * days;
        if (progressScientistDays >= node.researchCost()) {
            complete = true;
            return true;
        }
        return false;
    }
}

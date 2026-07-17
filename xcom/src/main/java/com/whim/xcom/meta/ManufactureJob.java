package com.whim.xcom.meta;

import com.whim.xcom.rules.def.ManufactureNode;

/**
 * An in-progress manufacturing job. Progress accrues in engineer-hours
 * ({@code engineers × elapsedHours}); each completed unit consumes the node's
 * per-unit cost and yields its output item, repeating until the ordered quantity
 * is built.
 */
public final class ManufactureJob {

    private final ManufactureNode node;
    private int assignedEngineers;
    private int quantityRemaining;
    private double progressHours;

    public ManufactureJob(ManufactureNode node, int assignedEngineers, int quantity) {
        this.node = node;
        this.assignedEngineers = assignedEngineers;
        this.quantityRemaining = Math.max(1, quantity);
    }

    public ManufactureNode node() { return node; }
    public String id() { return node.id(); }
    public String name() { return node.name(); }
    public int assignedEngineers() { return assignedEngineers; }
    public void setAssignedEngineers(int n) { this.assignedEngineers = Math.max(0, n); }
    public int quantityRemaining() { return quantityRemaining; }
    public boolean done() { return quantityRemaining <= 0; }

    /** Raw accrued engineer-hours on the current unit (for save/load). */
    public double progressHours() { return progressHours; }

    public void restoreProgress(double hours) { this.progressHours = hours; }

    public int percentOfCurrentUnit() {
        if (node.engineerHours() <= 0) {
            return 100;
        }
        return (int) Math.min(100, Math.round(progressHours * 100.0 / node.engineerHours()));
    }

    /** Advance by elapsed hours; returns the number of units finished this tick. */
    public int advance(double hours) {
        if (done()) {
            return 0;
        }
        progressHours += assignedEngineers * hours;
        int finished = 0;
        while (quantityRemaining > 0 && progressHours >= node.engineerHours()) {
            progressHours -= node.engineerHours();
            quantityRemaining--;
            finished++;
        }
        return finished;
    }
}

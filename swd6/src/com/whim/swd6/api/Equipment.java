package com.whim.swd6.api;

/**
 * A generic piece of gear that is neither weapon nor armor (comlink, medpac,
 * tools, etc.). Kept deliberately simple: name, quantity, cost, and a note.
 *
 * Owned by the orchestrator (api). Plain data holder.
 */
public final class Equipment {

    private String name;
    private int quantity;
    private int cost;
    private String notes;

    public Equipment() {
        this.name = "";
        this.quantity = 1;
        this.notes = "";
    }

    public Equipment(String name, int quantity, int cost, String notes) {
        this.name = name;
        this.quantity = quantity;
        this.cost = cost;
        this.notes = notes == null ? "" : notes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes; }
}

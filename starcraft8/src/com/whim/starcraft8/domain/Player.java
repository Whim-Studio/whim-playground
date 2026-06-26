package com.whim.starcraft8.domain;

/** A single participant (human or AI) with resource and supply accounting. */
public final class Player {
    private static final int SUPPLY_HARD_CAP = 200;

    private final int id;
    private final Race race;
    private final boolean ai;

    private int minerals;
    private int gas;
    private int supplyUsed;
    private int supplyCap;
    private boolean defeated;

    public Player(int id, Race race, boolean ai) {
        this.id = id;
        this.race = race;
        this.ai = ai;
        this.minerals = 0;
        this.gas = 0;
        this.supplyUsed = 0;
        this.supplyCap = 0;
        this.defeated = false;
    }

    public int id() { return id; }
    public Race race() { return race; }
    public boolean isAi() { return ai; }

    public int minerals() { return minerals; }
    public void addMinerals(int d) { this.minerals += d; if (this.minerals < 0) this.minerals = 0; }

    public int gas() { return gas; }
    public void addGas(int d) { this.gas += d; if (this.gas < 0) this.gas = 0; }

    public int supplyUsed() { return supplyUsed; }
    public int supplyCap() { return supplyCap; }
    public void setSupplyUsed(int u) { this.supplyUsed = u < 0 ? 0 : u; }
    public void setSupplyCap(int c) {
        if (c < 0) c = 0;
        if (c > SUPPLY_HARD_CAP) c = SUPPLY_HARD_CAP;
        this.supplyCap = c;
    }

    public boolean defeated() { return defeated; }
    public void setDefeated(boolean d) { this.defeated = d; }
}

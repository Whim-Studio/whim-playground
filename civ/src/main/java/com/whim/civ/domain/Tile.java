package com.whim.civ.domain;

/** A single square-grid map tile. */
public final class Tile {
    private Terrain terrain;
    private Improvement improvement = Improvement.NONE;
    private boolean goodyHut;
    private int ownerCivId = -1;

    public Tile(Terrain terrain) {
        this.terrain = terrain;
    }

    public Terrain getTerrain() { return terrain; }
    public void setTerrain(Terrain t) { this.terrain = t; }

    public Improvement getImprovement() { return improvement; }
    public void setImprovement(Improvement i) { this.improvement = i; }

    /** ROAD or RAILROAD present. */
    public boolean hasRoad() {
        return improvement == Improvement.ROAD || improvement == Improvement.RAILROAD;
    }

    public boolean hasGoodyHut() { return goodyHut; }
    public void setGoodyHut(boolean b) { this.goodyHut = b; }

    public int getOwnerCivId() { return ownerCivId; }   // -1 if unowned
    public void setOwnerCivId(int id) { this.ownerCivId = id; }

    // Effective yields AFTER improvements (irrigation +food, mine +shields, road +trade)
    // but BEFORE government effects. The ENGINE applies government/despotism/corruption.

    public int yieldFood() {
        int food = terrain.getFood();
        if (improvement == Improvement.IRRIGATION) {
            food += 1;
        }
        return food;
    }

    public int yieldShields() {
        int shields = terrain.getShields();
        if (improvement == Improvement.MINE) {
            shields += 2;
        }
        return shields;
    }

    public int yieldTrade() {
        int trade = terrain.getTrade();
        if (hasRoad()) {
            trade += 1;
        }
        return trade;
    }
}

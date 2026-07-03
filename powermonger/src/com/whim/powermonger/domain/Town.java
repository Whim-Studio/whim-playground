package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Views;

/** A settlement on a TOWN tile. Implements {@link Views.TownView}. */
public final class Town implements Views.TownView {

    private final int id;
    private final int tileX;
    private final int tileY;
    private final String name;
    private int population;
    private Allegiance allegiance;
    private boolean captured;

    public Town(int id, int tileX, int tileY, String name,
                int population, Allegiance allegiance) {
        this.id = id;
        this.tileX = tileX;
        this.tileY = tileY;
        this.name = name;
        this.population = population;
        this.allegiance = allegiance;
    }

    // ---- Views.TownView ----
    @Override public int id() { return id; }
    @Override public int tileX() { return tileX; }
    @Override public int tileY() { return tileY; }
    @Override public String name() { return name; }
    @Override public int population() { return population; }
    @Override public Allegiance allegiance() { return allegiance; }
    @Override public boolean captured() { return captured; }

    // ---- mutators ----
    public void setPopulation(int population) {
        this.population = population < 0 ? 0 : population;
    }
    public void setAllegiance(Allegiance allegiance) { this.allegiance = allegiance; }
    public void setCaptured(boolean captured) { this.captured = captured; }
}

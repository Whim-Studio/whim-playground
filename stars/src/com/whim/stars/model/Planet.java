package com.whim.stars.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.whim.stars.model.formulas.Formulas;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;

/**
 * A planet (star system) in the galaxy: its fixed position and environment, its
 * mineral geology, and — once colonized — its owner, population and
 * installations plus a production queue.
 *
 * <p>Environment axes (gravity/temperature/radiation) and mineral
 * concentrations are on a normalized 0..100 scale. Ownership is stored as a
 * player id ({@code -1} = uncolonized) so the object graph stays a clean tree
 * for serialization.
 */
public final class Planet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private String name;
    private final double x;
    private final double y;

    // Environment (0..100).
    private int gravity;
    private int temperature;
    private int radiation;

    // Geology: concentration (0..100+) and mined surface amount (kT) per mineral.
    private final int[] concentration = new int[Mineral.values().length];
    private final long[] surface = new long[Mineral.values().length];

    // Colony state.
    private int ownerId = -1;
    private long population;
    private int factories;
    private int mines;
    private int defenses;
    private boolean planetaryScanner;
    private boolean homeworld;

    private final List<ProductionItem> productionQueue = new ArrayList<ProductionItem>();

    public Planet(int id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public int id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public double x() { return x; }
    public double y() { return y; }

    public int gravity() { return gravity; }
    public int temperature() { return temperature; }
    public int radiation() { return radiation; }
    public void setEnvironment(int gravity, int temperature, int radiation) {
        this.gravity = clamp(gravity);
        this.temperature = clamp(temperature);
        this.radiation = clamp(radiation);
    }

    public int concentration(Mineral m) { return concentration[m.ordinal()]; }
    public void setConcentration(Mineral m, int v) { concentration[m.ordinal()] = Math.max(0, v); }
    public long surface(Mineral m) { return surface[m.ordinal()]; }
    public void setSurface(Mineral m, long v) { surface[m.ordinal()] = Math.max(0, v); }
    public void addSurface(Mineral m, long v) { setSurface(m, surface[m.ordinal()] + v); }

    public boolean isColonized() { return ownerId >= 0; }
    public int ownerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public long population() { return population; }
    public void setPopulation(long population) {
        this.population = Math.max(0, population);
        if (this.population == 0 && ownerId >= 0) {
            ownerId = -1; // a depopulated planet reverts to unowned
        }
    }
    public void addPopulation(long delta) { setPopulation(population + delta); }

    public int factories() { return factories; }
    public void setFactories(int f) { this.factories = Math.max(0, f); }
    public int mines() { return mines; }
    public void setMines(int m) { this.mines = Math.max(0, m); }
    public int defenses() { return defenses; }
    public void setDefenses(int d) { this.defenses = Math.max(0, d); }
    public boolean hasPlanetaryScanner() { return planetaryScanner; }
    public void setPlanetaryScanner(boolean b) { this.planetaryScanner = b; }

    public boolean isHomeworld() { return homeworld; }
    public void setHomeworld(boolean b) { this.homeworld = b; }

    public List<ProductionItem> productionQueue() { return productionQueue; }

    /** Habitability of this planet for a race, on [-1, +1]. */
    public double habitability(Race race) {
        return Formulas.habitability(race, gravity, temperature, radiation);
    }

    /** Maximum population this planet supports for a race. */
    public long maxPopulation(Race race) {
        return Formulas.maxPopulation(race, habitability(race));
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    @Override
    public String toString() {
        return "Planet#" + id + " " + name;
    }
}

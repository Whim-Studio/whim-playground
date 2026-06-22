package com.whim.civ.domain;

import java.util.HashSet;
import java.util.Set;

/** A player or rival civilization. */
public final class Civilization {
    private final int id;
    private final String name;
    private final boolean human;
    private Government government = Government.DESPOTISM;
    private final Set<TechType> knownTechs = new HashSet<TechType>();
    private TechType researching;
    private int researchBeakers;
    private int treasury;
    private int taxRate = 40;
    private int scienceRate = 60;
    private int luxuryRate = 0;
    private boolean alive = true;

    public Civilization(int id, String name, boolean human) {
        this.id = id;
        this.name = name;
        this.human = human;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isHuman() { return human; }

    public Government getGovernment() { return government; }
    public void setGovernment(Government g) { this.government = g; }

    public Set<TechType> getKnownTechs() { return knownTechs; }   // mutable set
    public boolean knows(TechType t) { return knownTechs.contains(t); }

    public TechType getResearching() { return researching; }
    public void setResearching(TechType t) { this.researching = t; }

    public int getResearchBeakers() { return researchBeakers; }
    public void setResearchBeakers(int b) { this.researchBeakers = b; }

    public int getTreasury() { return treasury; }
    public void setTreasury(int g) { this.treasury = g; }

    public int getTaxRate() { return taxRate; }
    public int getScienceRate() { return scienceRate; }
    public int getLuxuryRate() { return luxuryRate; }

    /** Tax/science/luxury rates as percentages summing to 100 (each a multiple of 10). */
    public void setRates(int tax, int science, int luxury) {
        if (tax + science + luxury != 100) {
            throw new IllegalArgumentException(
                    "Rates must sum to 100: " + tax + "/" + science + "/" + luxury);
        }
        this.taxRate = tax;
        this.scienceRate = science;
        this.luxuryRate = luxury;
    }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean a) { this.alive = a; }
}

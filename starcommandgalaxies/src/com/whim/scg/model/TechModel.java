package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;

/** Mutable tech-tree track. */
public final class TechModel implements Views.TechView {
    public Enums.TechType type;
    public String name;
    public int level;
    public int maxLevel;
    public int baseCost;

    public TechModel() {}

    public TechModel(Enums.TechType type, String name, int maxLevel, int baseCost) {
        this.type = type; this.name = name; this.maxLevel = maxLevel; this.baseCost = baseCost;
    }

    @Override public Enums.TechType type() { return type; }
    @Override public int level() { return level; }
    @Override public int maxLevel() { return maxLevel; }
    @Override public int cost() { return baseCost * (level + 1); }
    @Override public boolean maxed() { return level >= maxLevel; }
}

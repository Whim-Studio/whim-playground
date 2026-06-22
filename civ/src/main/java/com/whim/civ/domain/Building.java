package com.whim.civ.domain;

/** City improvements and Wonders. isWonder marks a one-per-world Wonder. */
public enum Building {
    // cost(shields), upkeep(gold/turn), isWonder, prereqTech
    BARRACKS       (40,  1, false, null),
    GRANARY        (60,  1, false, TechType.POTTERY),
    TEMPLE         (40,  1, false, TechType.CEREMONIAL_BURIAL),
    MARKETPLACE    (80,  1, false, TechType.CURRENCY),
    LIBRARY        (80,  1, false, TechType.WRITING),
    CITY_WALLS     (60,  1, false, TechType.MASONRY),
    AQUEDUCT       (80,  2, false, TechType.CONSTRUCTION),
    UNIVERSITY_B   (160, 3, false, TechType.UNIVERSITY),
    PYRAMIDS       (200, 0, true,  TechType.MASONRY),
    GREAT_LIBRARY  (300, 0, true,  TechType.LITERACY),
    HANGING_GARDENS(200, 0, true,  TechType.POTTERY);

    private final int cost;
    private final int upkeep;
    private final boolean isWonder;
    private final TechType prereq;

    Building(int cost, int upkeep, boolean isWonder, TechType prereq) {
        this.cost = cost;
        this.upkeep = upkeep;
        this.isWonder = isWonder;
        this.prereq = prereq;
    }

    public int getCost() { return cost; }
    public int getUpkeep() { return upkeep; }
    public boolean isWonder() { return isWonder; }
    public TechType getPrereq() { return prereq; }
}

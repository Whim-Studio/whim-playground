package com.whim.civ.domain;

/**
 * The unit roster. attack/defense/movement are nominal strength; cost is shield cost to build.
 */
public enum UnitType {
    // attack, defense, movement, cost, isSettler, canFound, prereqTech (null == none)
    SETTLERS (0, 1, 1, 40, true,  true,  null),
    MILITIA  (1, 1, 1, 10, false, false, null),
    PHALANX  (1, 2, 1, 20, false, false, TechType.BRONZE_WORKING),
    LEGION   (4, 2, 1, 20, false, false, TechType.IRON_WORKING),
    CHARIOT  (4, 1, 2, 30, false, false, TechType.THE_WHEEL),
    CATAPULT (6, 1, 1, 40, false, false, TechType.MATHEMATICS),
    DIPLOMAT (0, 0, 2, 30, false, false, TechType.WRITING),
    CAVALRY  (2, 1, 2, 20, false, false, TechType.HORSEBACK_RIDING),
    MUSKETEER(3, 3, 1, 30, false, false, TechType.GUNPOWDER);

    private final int attack;
    private final int defense;
    private final int movement;
    private final int cost;
    private final boolean isSettler;
    private final boolean canFound;
    private final TechType prereq;

    UnitType(int attack, int defense, int movement, int cost,
             boolean isSettler, boolean canFound, TechType prereq) {
        this.attack = attack;
        this.defense = defense;
        this.movement = movement;
        this.cost = cost;
        this.isSettler = isSettler;
        this.canFound = canFound;
        this.prereq = prereq;
    }

    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getMovement() { return movement; }   // movement points per turn
    public int getCost() { return cost; }            // shields to produce
    public boolean isSettler() { return isSettler; } // can terraform / build roads
    public boolean canFound() { return canFound; }   // can found a city
    public TechType getPrereq() { return prereq; }   // null == always available
}

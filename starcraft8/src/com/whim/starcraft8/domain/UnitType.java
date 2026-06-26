package com.whim.starcraft8.domain;

import java.awt.Color;

/**
 * Embedded unit data dictionary. Each constant carries all combat/economy stats.
 * NOTE: enum constants here MUST NOT reference any other enum's constants in their
 * constructor arguments (only literals / this enum's own ArmorClass etc. which are
 * separate enums with no init dependency on UnitType). Production/tech relationships
 * live in {@code data.TechTree}, not here.
 */
public enum UnitType {
    // --- Terran ---
    SCV(Race.TERRAN, "SCV", 50, 0, 1, 0, 40, 0, 0, ArmorClass.SMALL,
        5, DamageType.NORMAL, AttackKind.MELEE, 1.0, 15, 0.055, 7, 300, true, false, 0,
        new Color(180, 170, 120)),
    MARINE(Race.TERRAN, "Marine", 50, 0, 1, 0, 40, 0, 0, ArmorClass.SMALL,
        6, DamageType.NORMAL, AttackKind.RANGED, 4.0, 15, 0.045, 7, 360, false, false, 0,
        new Color(200, 60, 50)),
    FIREBAT(Race.TERRAN, "Firebat", 50, 25, 1, 0, 50, 0, 1, ArmorClass.SMALL,
        8, DamageType.CONCUSSIVE, AttackKind.MELEE, 1.2, 22, 0.045, 7, 360, false, false, 1,
        new Color(220, 120, 40)),
    SIEGE_TANK(Race.TERRAN, "Siege Tank", 150, 100, 2, 0, 150, 0, 1, ArmorClass.LARGE,
        30, DamageType.EXPLOSIVE, AttackKind.RANGED, 7.0, 40, 0.030, 8, 600, false, false, 1,
        new Color(120, 110, 90)),

    // --- Zerg ---
    DRONE(Race.ZERG, "Drone", 50, 0, 1, 0, 40, 0, 0, ArmorClass.SMALL,
        5, DamageType.NORMAL, AttackKind.MELEE, 1.0, 15, 0.055, 7, 300, true, false, 0,
        new Color(150, 80, 160)),
    OVERLORD(Race.ZERG, "Overlord", 100, 0, 0, 8, 120, 0, 0, ArmorClass.LARGE,
        0, DamageType.NORMAL, AttackKind.NONE, 0.0, 0, 0.018, 9, 600, false, true, 0,
        new Color(120, 70, 140)),
    ZERGLING(Race.ZERG, "Zergling", 25, 0, 1, 0, 35, 0, 0, ArmorClass.SMALL,
        5, DamageType.NORMAL, AttackKind.MELEE, 1.0, 8, 0.075, 6, 180, false, false, 0,
        new Color(190, 100, 180)),
    HYDRALISK(Race.ZERG, "Hydralisk", 75, 25, 1, 0, 80, 0, 0, ArmorClass.MEDIUM,
        10, DamageType.EXPLOSIVE, AttackKind.RANGED, 4.0, 16, 0.050, 7, 240, false, false, 0,
        new Color(160, 90, 130)),

    // --- Protoss ---
    PROBE(Race.PROTOSS, "Probe", 50, 0, 1, 0, 20, 20, 0, ArmorClass.SMALL,
        5, DamageType.NORMAL, AttackKind.MELEE, 1.0, 15, 0.055, 8, 300, true, false, 0,
        new Color(220, 200, 80)),
    ZEALOT(Race.PROTOSS, "Zealot", 100, 0, 2, 0, 60, 40, 1, ArmorClass.MEDIUM,
        8, DamageType.NORMAL, AttackKind.MELEE, 1.0, 22, 0.045, 7, 360, false, false, 0,
        new Color(230, 180, 60)),
    DRAGOON(Race.PROTOSS, "Dragoon", 125, 50, 2, 0, 80, 80, 1, ArmorClass.LARGE,
        12, DamageType.EXPLOSIVE, AttackKind.RANGED, 5.0, 22, 0.050, 8, 360, false, false, 0,
        new Color(240, 200, 90));

    private final Race race;
    private final String displayName;
    private final int mineralCost;
    private final int gasCost;
    private final int supplyCost;
    private final int supplyProvided;
    private final int maxHp;
    private final int maxShield;
    private final int armor;
    private final ArmorClass armorClass;
    private final int damage;
    private final DamageType damageType;
    private final AttackKind attackKind;
    private final double range;
    private final int cooldown;
    private final double speed;
    private final double sight;
    private final int buildTicks;
    private final boolean isWorker;
    private final boolean isFlyer;
    private final int splashRadius;
    private final Color baseColor;

    UnitType(Race race, String displayName, int mineralCost, int gasCost, int supplyCost,
             int supplyProvided, int maxHp, int maxShield, int armor, ArmorClass armorClass,
             int damage, DamageType damageType, AttackKind attackKind, double range, int cooldown,
             double speed, double sight, int buildTicks, boolean isWorker, boolean isFlyer,
             int splashRadius, Color baseColor) {
        this.race = race;
        this.displayName = displayName;
        this.mineralCost = mineralCost;
        this.gasCost = gasCost;
        this.supplyCost = supplyCost;
        this.supplyProvided = supplyProvided;
        this.maxHp = maxHp;
        this.maxShield = maxShield;
        this.armor = armor;
        this.armorClass = armorClass;
        this.damage = damage;
        this.damageType = damageType;
        this.attackKind = attackKind;
        this.range = range;
        this.cooldown = cooldown;
        this.speed = speed;
        this.sight = sight;
        this.buildTicks = buildTicks;
        this.isWorker = isWorker;
        this.isFlyer = isFlyer;
        this.splashRadius = splashRadius;
        this.baseColor = baseColor;
    }

    public Race race() { return race; }
    public String displayName() { return displayName; }
    public int mineralCost() { return mineralCost; }
    public int gasCost() { return gasCost; }
    public int supplyCost() { return supplyCost; }
    public int supplyProvided() { return supplyProvided; }
    public int maxHp() { return maxHp; }
    public int maxShield() { return maxShield; }
    public int armor() { return armor; }
    public ArmorClass armorClass() { return armorClass; }
    public int damage() { return damage; }
    public DamageType damageType() { return damageType; }
    public AttackKind attackKind() { return attackKind; }
    public double range() { return range; }
    public int cooldown() { return cooldown; }
    public double speed() { return speed; }
    public double sight() { return sight; }
    public int buildTicks() { return buildTicks; }
    public boolean isWorker() { return isWorker; }
    public boolean isFlyer() { return isFlyer; }
    public int splashRadius() { return splashRadius; }
    public Color baseColor() { return baseColor; }
}

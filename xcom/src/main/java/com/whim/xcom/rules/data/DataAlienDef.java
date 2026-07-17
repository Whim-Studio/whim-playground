package com.whim.xcom.rules.data;

import com.whim.xcom.rules.def.AlienDef;

/** Immutable {@link AlienDef} backed by data-pack fields (Beginner base stats). */
public final class DataAlienDef implements AlienDef {

    private String id;
    private String name;
    private int timeUnits;
    private int stamina;
    private int health;
    private int reactions;
    private int firingAccuracy;
    private int strength;
    private int psiStrength;
    private int frontArmor;
    private int scoreValue;

    public DataAlienDef(String id, String name, int timeUnits, int stamina, int health,
                        int reactions, int firingAccuracy, int strength, int psiStrength,
                        int frontArmor, int scoreValue) {
        this.id = id;
        this.name = name;
        this.timeUnits = timeUnits;
        this.stamina = stamina;
        this.health = health;
        this.reactions = reactions;
        this.firingAccuracy = firingAccuracy;
        this.strength = strength;
        this.psiStrength = psiStrength;
        this.frontArmor = frontArmor;
        this.scoreValue = scoreValue;
    }

    DataAlienDef() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int timeUnits() { return timeUnits; }
    @Override public int stamina() { return stamina; }
    @Override public int health() { return health; }
    @Override public int reactions() { return reactions; }
    @Override public int firingAccuracy() { return firingAccuracy; }
    @Override public int strength() { return strength; }
    @Override public int psiStrength() { return psiStrength; }
    @Override public int frontArmor() { return frontArmor; }
    @Override public int scoreValue() { return scoreValue; }
}

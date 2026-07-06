package com.whim.starcommand.model;

import java.io.Serializable;

/** A ship weapon definition. Beam weapons are accurate; missiles hit harder but can miss. */
public class Weapon implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { BEAM, MISSILE }

    public String name;
    public Type type;
    public int minDamage;
    public int maxDamage;
    public int accuracy;   // 0..100 base hit chance
    public int cost;       // credits

    public Weapon() { }

    public Weapon(String name, Type type, int minDamage, int maxDamage, int accuracy, int cost) {
        this.name = name;
        this.type = type;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.accuracy = accuracy;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return name + "  [" + type + "  dmg " + minDamage + "-" + maxDamage
                + "  acc " + accuracy + "%  " + cost + "cr]";
    }
}

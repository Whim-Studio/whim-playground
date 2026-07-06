package com.whim.starcommand.model;

import java.io.Serializable;

/**
 * A single crew member. In the 1988 original a character's "class" is really a
 * skill loadout accumulated by attending training schools; we model a small,
 * tunable stat block plus a school-derived role label and derived hit points.
 */
public class Character implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Rolled primary stats, each on a 3..18 scale (our tunable interpretation). */
    public int strength;
    public int speed;      // dexterity / initiative
    public int accuracy;   // ranged combat
    public int intellect;  // tech / psi aptitude
    public int leadership; // command / morale
    public int willpower;  // resistance / esper power

    public String name;
    public String role;    // "Pilot", "Marine", "Esper", "Medic", ...
    public int maxHp;
    public int hp;
    public boolean alive = true;

    public Character() { }

    /** Recompute derived hit points from strength/willpower. */
    public void deriveHp() {
        this.maxHp = 10 + strength + (willpower / 2);
        if (hp <= 0 || hp > maxHp) hp = maxHp;
    }

    public int statTotal() {
        return strength + speed + accuracy + intellect + leadership + willpower;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}

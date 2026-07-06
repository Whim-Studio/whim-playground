package com.whim.starcommand.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A starship. Hull can be disabled (reduced to 0) so an enemy vessel can be
 * boarded and captured rather than destroyed — a core Star Command mechanic.
 */
public class Ship implements Serializable {
    private static final long serialVersionUID = 1L;

    public String className;   // "Scout", "Corvette", "Frigate", ...
    public int maxHull;
    public int hull;
    public int maxShield;
    public int shield;
    public int engines;        // affects flee / initiative
    public int weaponSlots;
    public final List<Weapon> weapons = new ArrayList<Weapon>();

    public boolean disabled = false;   // hull at 0 but not exploded -> boardable

    public Ship() { }

    public Ship(String className, int maxHull, int maxShield, int engines, int weaponSlots) {
        this.className = className;
        this.maxHull = maxHull;
        this.hull = maxHull;
        this.maxShield = maxShield;
        this.shield = maxShield;
        this.engines = engines;
        this.weaponSlots = weaponSlots;
    }

    public void repairFull() {
        hull = maxHull;
        shield = maxShield;
        disabled = false;
    }

    /** Apply damage: shields soak first, then hull. Returns true if newly disabled. */
    public boolean takeDamage(int dmg) {
        if (shield > 0) {
            int absorbed = Math.min(shield, dmg);
            shield -= absorbed;
            dmg -= absorbed;
        }
        if (dmg > 0) hull -= dmg;
        if (hull <= 0) {
            hull = 0;
            if (!disabled) { disabled = true; return true; }
        }
        return false;
    }
}

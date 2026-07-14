package com.whim.stars.model.ship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.whim.stars.model.Cargo;
import com.whim.stars.model.TechLevels;

/**
 * A player-authored ship design: one {@link HullType} with a component placed
 * (in some count) in each of its slots. Designs are shared instances referenced
 * by fleets — editing a design is meant to affect every ship built from it,
 * which is why persistence must preserve reference identity.
 *
 * <p>All derived stats (mass, armor, shield, cost, warp, firepower) are computed
 * on demand from the hull and the placed components.
 */
public final class ShipDesign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** One filled slot: which slot definition, the component in it, and count. */
    public static final class Placement implements Serializable {
        private static final long serialVersionUID = 1L;
        private final HullType.SlotDef slot;
        private Component component;
        private int count;

        Placement(HullType.SlotDef slot) {
            this.slot = slot;
        }

        public HullType.SlotDef slot() { return slot; }
        public Component component() { return component; }
        public int count() { return count; }
    }

    private String name;
    private final HullType hull;
    private final List<Placement> placements = new ArrayList<Placement>();

    public ShipDesign(String name, HullType hull) {
        this.name = name;
        this.hull = hull;
        for (HullType.SlotDef s : hull.slots()) {
            placements.add(new Placement(s));
        }
    }

    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public HullType hull() { return hull; }
    public List<Placement> placements() { return placements; }

    /**
     * Fill a slot with a component. The component's category must be accepted by
     * the slot, otherwise an {@link IllegalArgumentException} is thrown. Count is
     * clamped to the slot capacity.
     */
    public void place(int slotIndex, Component component, int count) {
        Placement p = placements.get(slotIndex);
        if (component != null && !p.slot.accepts(component.category())) {
            throw new IllegalArgumentException("Slot " + slotIndex + " (" + p.slot.category()
                    + ") does not accept a " + component.category() + " component");
        }
        p.component = component;
        p.count = component == null ? 0 : Math.max(1, Math.min(count, p.slot.capacity()));
    }

    public void place(int slotIndex, Component component) {
        place(slotIndex, component, 1);
    }

    public int engineCount() {
        int n = 0;
        for (Placement p : placements) {
            if (p.component != null && p.component.category() == ComponentCategory.ENGINE) {
                n += p.count;
            }
        }
        return n;
    }

    /** A design is legal to build only if it carries at least one engine (or is a starbase). */
    public boolean isValid() {
        return hull.isStarbase() || engineCount() >= 1;
    }

    public int totalMass() {
        int mass = hull.baseMass();
        for (Placement p : placements) {
            if (p.component != null) {
                mass += p.component.mass() * p.count;
            }
        }
        return mass;
    }

    public int totalArmor() {
        int armor = hull.baseArmor();
        for (Placement p : placements) {
            if (p.component != null) {
                armor += p.component.armor() * p.count;
            }
        }
        return armor;
    }

    public int totalShield() {
        int shield = 0;
        for (Placement p : placements) {
            if (p.component != null) {
                shield += p.component.shield() * p.count;
            }
        }
        return shield;
    }

    /** Design's safe warp: limited by its slowest engine (0 if it has none). */
    public int maxWarp() {
        int warp = 0;
        boolean seen = false;
        for (Placement p : placements) {
            if (p.component != null && p.component.category() == ComponentCategory.ENGINE) {
                int w = p.component.maxWarp();
                warp = seen ? Math.min(warp, w) : w;
                seen = true;
            }
        }
        return warp;
    }

    public int fuelCapacity() {
        int fuel = hull.fuelCapacity();
        for (Placement p : placements) {
            if (p.component != null) {
                fuel += p.component.fuelCapacity() * p.count;
            }
        }
        return fuel;
    }

    public int cargoCapacity() {
        int cargo = hull.cargoCapacity();
        for (Placement p : placements) {
            if (p.component != null) {
                cargo += p.component.cargoCapacity() * p.count;
            }
        }
        return cargo;
    }

    public int scanRange() {
        int scan = 0;
        for (Placement p : placements) {
            if (p.component != null) {
                scan = Math.max(scan, p.component.scanRange());
            }
        }
        return scan;
    }

    /** Total offensive firepower — the sum of weapon power across all slots. */
    public int totalWeaponPower() {
        int power = 0;
        for (Placement p : placements) {
            if (p.component != null && p.component.category() == ComponentCategory.WEAPON) {
                power += p.component.weaponPower() * p.count;
            }
        }
        return power;
    }

    public boolean isArmed() {
        return totalWeaponPower() > 0;
    }

    public boolean canColonize() {
        for (Placement p : placements) {
            if (p.component != null && p.component.category() == ComponentCategory.MECHANICAL
                    && p.component.name().toLowerCase().contains("colon")) {
                return true;
            }
        }
        return false;
    }

    /** Build cost in minerals (Ironium/Boranium/Germanium), hull + components. */
    public Cargo mineralCost() {
        long ir = hull.ironium();
        long bo = hull.boranium();
        long ge = hull.germanium();
        for (Placement p : placements) {
            if (p.component != null) {
                ir += (long) p.component.ironium() * p.count;
                bo += (long) p.component.boranium() * p.count;
                ge += (long) p.component.germanium() * p.count;
            }
        }
        return new Cargo(ir, bo, ge, 0, 0);
    }

    public int resourceCost() {
        int res = hull.resources();
        for (Placement p : placements) {
            if (p.component != null) {
                res += p.component.resources() * p.count;
            }
        }
        return res;
    }

    public boolean isBuildable(TechLevels tech) {
        if (!hull.isAvailable(tech)) {
            return false;
        }
        for (Placement p : placements) {
            if (p.component != null && !p.component.isAvailable(tech)) {
                return false;
            }
        }
        return isValid();
    }

    @Override
    public String toString() {
        return name + " (" + hull.name() + ")";
    }
}

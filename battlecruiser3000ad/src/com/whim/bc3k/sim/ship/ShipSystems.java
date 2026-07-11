package com.whim.bc3k.sim.ship;

import com.whim.bc3k.api.Enums;

import java.util.EnumMap;
import java.util.Map;

/**
 * Phase 3 ship-systems simulation: the reactor power budget, per-system power
 * allocation, hull/shields, and a damage/repair model. Pure model — no Swing, no
 * randomness — so it is fully unit-testable and deterministic.
 *
 * All magnitudes are labelled design approximations (see BC3K_Phase1_Design.md),
 * not values recovered from the original game.
 */
public final class ShipSystems {

    /** A damageable, powerable subsystem behind one {@link Enums.PowerSystem}. */
    public static final class Subsystem {
        private int power;           // 0..cap reactor units allocated
        private double integrity;    // 0..100 health; effectiveness scales with it
        private boolean breached;    // radiation/hull breach on this system's deck

        Subsystem() { this.integrity = 100.0; }

        public int power() { return power; }
        public int integrity() { return (int) Math.round(integrity); }
        public boolean breached() { return breached; }

        /** 0..1 output multiplier: needs power and integrity; zero if breached. */
        public double effectiveness() {
            if (breached || power <= 0) return 0.0;
            return integrity / 100.0;
        }
    }

    public static final int REACTOR_OUTPUT = 100;   // total power units
    public static final int MAX_PER_SYSTEM  = 30;    // cap per single system
    public static final int MAX_HULL        = 1000;
    public static final int MAX_SHIELDS      = 400;

    private final Map<Enums.PowerSystem, Subsystem> systems =
            new EnumMap<Enums.PowerSystem, Subsystem>(Enums.PowerSystem.class);

    private double hull = MAX_HULL;
    private double shields = MAX_SHIELDS;
    private boolean reactorOnline = true;

    public ShipSystems() {
        for (Enums.PowerSystem s : Enums.PowerSystem.values()) systems.put(s, new Subsystem());
        // Sensible starting allocation (sums to 80, under the 100 budget).
        systems.get(Enums.PowerSystem.SHIELDS).power = 20;
        systems.get(Enums.PowerSystem.WEAPONS).power = 15;
        systems.get(Enums.PowerSystem.ENGINES).power = 20;
        systems.get(Enums.PowerSystem.LIFE_SUPPORT).power = 15;
        systems.get(Enums.PowerSystem.SENSORS).power = 10;
    }

    // ---- reads ----
    public int hull() { return (int) Math.round(hull); }
    public int maxHull() { return MAX_HULL; }
    public int shields() { return (int) Math.round(shields); }
    public int maxShields() { return MAX_SHIELDS; }
    public boolean reactorOnline() { return reactorOnline; }
    public int reactorOutput() { return reactorOnline ? REACTOR_OUTPUT : 0; }
    public Subsystem system(Enums.PowerSystem s) { return systems.get(s); }

    public int reactorUsed() {
        int t = 0;
        for (Subsystem s : systems.values()) t += s.power;
        return t;
    }

    public boolean destroyed() { return hull <= 0; }

    // ---- power management ----

    /** Change one system's power draw by delta, respecting the per-system cap and reactor budget. */
    public boolean allocate(Enums.PowerSystem s, int delta) {
        if (!reactorOnline) return false;
        Subsystem sub = systems.get(s);
        int next = sub.power + delta;
        if (next < 0 || next > MAX_PER_SYSTEM) return false;
        if (delta > 0 && reactorUsed() + delta > REACTOR_OUTPUT) return false;
        sub.power = next;
        return true;
    }

    // ---- save/load restore ----
    public void setHull(int h) { hull = clamp(h, 0, MAX_HULL); }
    public void setShields(int s) { shields = clamp(s, 0, MAX_SHIELDS); }
    public void setReactorOnline(boolean online) { reactorOnline = online; }
    public void setPowerAbsolute(Enums.PowerSystem s, int p) {
        systems.get(s).power = (int) clamp(p, 0, MAX_PER_SYSTEM);
    }

    public boolean restartReactor() {
        if (reactorOnline) return false;
        reactorOnline = true;
        return true;
    }

    /** Forces a reactor shutdown (breach/failure); power output drops to zero until restart. */
    public void shutdownReactor() { reactorOnline = false; }

    // ---- damage & repair ----

    /**
     * Apply incoming damage: shields absorb first, hull takes the remainder. A
     * large hull hit has a chance-free (deterministic) spillover that also degrades
     * a targeted subsystem's integrity when provided.
     */
    public void applyDamage(double amount, Enums.PowerSystem hitSystem) {
        if (amount <= 0) return;
        double toShields = Math.min(shields, amount);
        shields -= toShields;
        double toHull = amount - toShields;
        hull = Math.max(0, hull - toHull);
        if (hitSystem != null && toHull > 0) {
            damageSystem(hitSystem, toHull / 10.0);
        }
    }

    /** Degrade a subsystem's integrity; at zero integrity it breaches (needs repair). */
    public void damageSystem(Enums.PowerSystem s, double amount) {
        Subsystem sub = systems.get(s);
        sub.integrity = clamp(sub.integrity - amount, 0, 100);
        if (sub.integrity <= 0) sub.breached = true;
    }

    /** Engineering repair: restores integrity; clears the breach once integrity recovers. */
    public boolean repairSystem(Enums.PowerSystem s, double amount) {
        Subsystem sub = systems.get(s);
        if (sub.integrity >= 100 && !sub.breached) return false;
        sub.integrity = clamp(sub.integrity + amount, 0, 100);
        if (sub.integrity >= 25) sub.breached = false;
        return true;
    }

    // ---- simulation step ----

    /** Advance shields: regen scales with shield power and the shield system's integrity. */
    public void tick(double dt) {
        double eff = systems.get(Enums.PowerSystem.SHIELDS).effectiveness();
        if (reactorOnline && eff > 0) {
            double regen = 12.0 * systems.get(Enums.PowerSystem.SHIELDS).power * eff * dt;
            shields = clamp(shields + regen, 0, MAX_SHIELDS);
        } else {
            shields = clamp(shields - 20.0 * dt, 0, MAX_SHIELDS);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

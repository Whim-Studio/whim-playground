package com.whim.necromunda.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A single gang member. Holds identity, role, the mutable {@link StatLine},
 * equipped weapons and armour, campaign experience, and transient per-battle
 * state (status, wounds remaining this battle, and action/overwatch flags).
 *
 * <p>The board position is <em>not</em> stored here — placement lives in
 * {@link com.whim.necromunda.model.board.Board} so a fighter object is portable
 * between battles and campaign saves.
 */
public final class Fighter {

    private final String id;
    private String name;
    private FighterType type;
    private final StatLine stats;
    private final List<Weapon> weapons = new ArrayList<Weapon>();
    private Armour armour = Armour.NONE;
    private int experience;

    // Transient per-battle state.
    private FighterStatus status = FighterStatus.ACTIVE;
    private int woundsRemaining;
    private boolean hasActed;
    private boolean onOverwatch;

    public Fighter(String id, String name, FighterType type, StatLine stats) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.stats = stats;
        this.woundsRemaining = stats.effective(Stat.W);
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public FighterType type() { return type; }
    public void setType(FighterType type) { this.type = type; }

    public StatLine stats() { return stats; }

    public List<Weapon> weapons() { return weapons; }
    public void addWeapon(Weapon w) { weapons.add(w); }
    public void clearWeapons() { weapons.clear(); }

    public Armour armour() { return armour; }
    public void setArmour(Armour armour) { this.armour = armour; }

    public int experience() { return experience; }
    public void setExperience(int xp) { this.experience = xp; }
    public void addExperience(int xp) { this.experience += xp; }

    public FighterStatus status() { return status; }
    public void setStatus(FighterStatus status) { this.status = status; }

    public int woundsRemaining() { return woundsRemaining; }
    public void setWoundsRemaining(int w) { this.woundsRemaining = w; }

    public boolean hasActed() { return hasActed; }
    public void setHasActed(boolean acted) { this.hasActed = acted; }

    public boolean isOnOverwatch() { return onOverwatch; }
    public void setOnOverwatch(boolean ow) { this.onOverwatch = ow; }

    /** Convenience accessor for an effective stat value. */
    public int stat(Stat s) {
        return stats.effective(s);
    }

    /** Whether this fighter can still take actions this turn. */
    public boolean canAct() {
        return status.inPlay() && status == FighterStatus.ACTIVE && !hasActed;
    }

    /** Reset per-turn flags at the start of the owning gang's turn. */
    public void beginTurn() {
        this.hasActed = false;
    }

    /** Reset per-battle state to full readiness (used when deploying to a battle). */
    public void resetForBattle() {
        this.status = FighterStatus.ACTIVE;
        this.woundsRemaining = stats.effective(Stat.W);
        this.hasActed = false;
        this.onOverwatch = false;
    }

    /**
     * Rough points value for gang-rating purposes: a base cost by role, plus
     * equipment cost, plus an experience-derived increment.
     */
    public int ratingValue() {
        int base;
        switch (type) {
            case LEADER:   base = 120; break;
            case CHAMPION: base = 80;  break;
            case GANGER:   base = 50;  break;
            case JUVE:     base = 25;  break;
            default:       base = 40;  break;
        }
        int gear = armour.cost();
        for (Weapon w : weapons) {
            gear += w.cost();
        }
        return base + gear + (experience / 5);
    }

    @Override
    public String toString() {
        return name + " (" + type.label() + ")";
    }
}

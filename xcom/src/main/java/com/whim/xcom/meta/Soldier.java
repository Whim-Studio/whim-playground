package com.whim.xcom.meta;

/**
 * A persistent X-COM soldier that survives between missions, accrues experience
 * and can be wounded or killed. Stats mirror the battlescape inputs so a roster
 * member can be deployed directly. Progression is a simplified 1994 model: a
 * survivor of a successful mission gains a little in several stats and ranks up
 * with mission count.
 */
public final class Soldier {

    private final String name;
    private int timeUnits;
    private int health;
    private int firingAccuracy;
    private int reactions;
    private int strength;
    private int rank;         // 0 Rookie .. 5 Commander
    private int missions;
    private int kills;
    private int woundedDays;  // >0 = in the infirmary, not deployable

    public Soldier(String name, int timeUnits, int health, int firingAccuracy,
                   int reactions, int strength) {
        this.name = name;
        this.timeUnits = timeUnits;
        this.health = health;
        this.firingAccuracy = firingAccuracy;
        this.reactions = reactions;
        this.strength = strength;
    }

    public String name() { return name; }
    public int timeUnits() { return timeUnits; }
    public int health() { return health; }
    public int firingAccuracy() { return firingAccuracy; }
    public int reactions() { return reactions; }
    public int strength() { return strength; }
    public int rank() { return rank; }
    public int missions() { return missions; }
    public int kills() { return kills; }
    public int woundedDays() { return woundedDays; }
    public boolean deployable() { return woundedDays <= 0; }

    private static final String[] RANKS = {
        "Rookie", "Squaddie", "Sergeant", "Captain", "Colonel", "Commander"
    };

    public String rankName() {
        return RANKS[Math.max(0, Math.min(RANKS.length - 1, rank))];
    }

    /** Restore progression state from a save. */
    public void restore(int rank, int missions, int kills, int woundedDays) {
        this.rank = rank;
        this.missions = missions;
        this.kills = kills;
        this.woundedDays = woundedDays;
    }

    /** Heal one day in the infirmary. */
    public void restDay() {
        if (woundedDays > 0) {
            woundedDays--;
        }
    }

    public void wound(int days) {
        woundedDays = Math.max(woundedDays, days);
    }

    public void addKills(int k) {
        kills += Math.max(0, k);
    }

    /** Apply post-mission progression after surviving a successful assault. */
    public void onMissionSurvived(boolean victory) {
        missions++;
        if (victory) {
            firingAccuracy += 2;
            reactions += 2;
            timeUnits += 1;
            health += 1;
            strength += 1;
        }
        // Promote roughly every two successful missions.
        int deserved = Math.min(RANKS.length - 1, missions / 2);
        if (deserved > rank) {
            rank = deserved;
        }
    }
}

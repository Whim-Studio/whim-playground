package com.whim.bc3k.sim.crew;

import java.util.EnumMap;
import java.util.Map;

/**
 * One individually-simulated crew member. Tracks health, fatigue and hunger, a
 * skill profile, current ship location, and station assignment. Mechanics follow
 * the brief (crew move between named locations, can be injured, and can die of
 * hunger); exact rates are labelled design approximations.
 */
public final class CrewMember {

    /** Skill axes (0..100). Assignment suitability reads from these. */
    public enum Skill { COMMAND, PILOTING, ENGINEERING, GUNNERY, MEDICAL, COMBAT, SCIENCE }

    private final int id;
    private String name;
    private final Map<Skill, Integer> skills = new EnumMap<Skill, Integer>(Skill.class);

    private double health = 100;   // 0 => dead
    private double fatigue = 0;    // 100 => must rest
    private double hunger = 0;     // 100 => starving (health drains)

    private ShipLocation location;
    private ShipLocation destination;   // where the crew is walking to, or null

    public CrewMember(int id, String name, ShipLocation start) {
        this.id = id;
        this.name = name;
        this.location = start;
        for (Skill s : Skill.values()) skills.put(s, 20);
    }

    // ---- reads ----
    public int id() { return id; }
    public String name() { return name; }
    public void rename(String n) { if (n != null && !n.isEmpty()) name = n; }
    public int health() { return (int) Math.round(health); }
    public int fatigue() { return (int) Math.round(fatigue); }
    public int hunger() { return (int) Math.round(hunger); }
    public boolean alive() { return health > 0; }
    public ShipLocation location() { return location; }
    public ShipLocation destination() { return destination; }
    public int skill(Skill s) { return skills.get(s); }
    public void setSkill(Skill s, int v) { skills.put(s, clampI(v, 0, 100)); }

    /** Best-fit score (0..100) for a role's primary skill — used when auto-assigning craft. */
    public int aptitude(Skill s) { return skills.get(s); }

    // ---- movement between named ship locations ----
    public void orderTo(ShipLocation dest) { this.destination = dest; }

    /** Advance one step: walk toward destination, and update needs. */
    public void tick(double dt) {
        if (!alive()) return;

        // Walking: reaching the destination takes ~2 seconds per hop (approximation).
        if (destination != null && destination != location) {
            // Deterministic arrival model: arrive after accumulating enough dt.
            walkProgress += dt;
            if (walkProgress >= 2.0) { location = destination; destination = null; walkProgress = 0; }
        }

        // Needs. Resting in QUARTERS recovers fatigue; eating in GALLEY clears hunger.
        boolean resting = location == ShipLocation.QUARTERS;
        boolean eating  = location == ShipLocation.GALLEY;

        fatigue = clamp(fatigue + (resting ? -8.0 : 2.0) * dt, 0, 100);
        hunger  = clamp(hunger  + (eating  ? -30.0 : 1.5) * dt, 0, 100);

        // Starvation drains health once hunger is maxed (brief: crew can die of hunger).
        if (hunger >= 100) health = clamp(health - 5.0 * dt, 0, 100);
        // Exhaustion mildly harms health once fatigue is maxed.
        else if (fatigue >= 100) health = clamp(health - 1.0 * dt, 0, 100);
    }

    /** Injury (combat/radiation). */
    public void injure(double amount) { health = clamp(health - amount, 0, 100); }
    public void heal(double amount) { if (alive()) health = clamp(health + amount, 0, 100); }

    private double walkProgress = 0;

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
    private static int clampI(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

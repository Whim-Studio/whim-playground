package com.whim.bc3k.sim.campaign;

/**
 * A deliberately small Advanced Campaign Mode ticker. The original ACM was a
 * sprawling dynamic sim of politics and military movement (see
 * BC3K_Phase1_Design.md §1.2); this v1 slice models the one dynamic the player can
 * feel: a Gammulan threat level that rises over time and a rotating GALCOM
 * objective the player resolves to push it back.
 *
 * Deterministic (no randomness, no wall-clock) so it is fully unit-testable.
 */
public final class Campaign {

    private static final double THREAT_RISE = 0.6;   // threat points per second
    private static final double THREAT_RELIEF = 18;   // drop per objective resolved

    private static final String[] OBJECTIVES = {
        "Patrol the Sol–Centauri lane and clear pirate contacts.",
        "Escort a supply convoy to the Sirius starstation.",
        "Scout Gammulan movement near Vega.",
        "Reinforce the frontier at Gammula Reach."
    };

    private double threat = 20;      // 0..100
    private int objectiveIndex = 0;
    private int resolved = 0;

    /** Advance the campaign clock: Gammulan pressure builds. */
    public void tick(double dt) {
        threat = clamp(threat + THREAT_RISE * dt, 0, 100);
    }

    public int threat() { return (int) Math.round(threat); }
    public boolean critical() { return threat >= 80; }
    public int resolvedCount() { return resolved; }
    public String objective() { return OBJECTIVES[objectiveIndex % OBJECTIVES.length]; }

    /** Resolve the current objective: advance to the next and relieve threat. */
    public void resolveObjective() {
        resolved++;
        objectiveIndex++;
        threat = clamp(threat - THREAT_RELIEF, 0, 100);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

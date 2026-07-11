package com.whim.bc3k.sim.crew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The battlecruiser's crew complement. Owns hiring, death, DNA storage/cloning,
 * and best-fit assignment. Deterministic and Swing-free for unit testing.
 *
 * DNA/cloning models the brief: a dead crew member's DNA is retained and can be
 * cloned into a fresh crew member; new crew can also be hired at starstations.
 */
public final class CrewRoster {

    private final List<CrewMember> crew = new ArrayList<CrewMember>();
    private final List<String> dnaVault = new ArrayList<String>();
    private int nextId = 1;

    /** Hire a new crew member (e.g. at a starstation), placed at the bridge. */
    public CrewMember hire(String name) {
        CrewMember m = new CrewMember(nextId++, name, ShipLocation.BRIDGE);
        crew.add(m);
        return m;
    }

    public List<CrewMember> members() { return Collections.unmodifiableList(crew); }
    public int aliveCount() {
        int n = 0;
        for (CrewMember m : crew) if (m.alive()) n++;
        return n;
    }

    public CrewMember byId(int id) {
        for (CrewMember m : crew) if (m.id() == id) return m;
        return null;
    }

    /** Advance every crew member; harvest DNA from any who died this step. */
    public void tick(double dt) {
        for (CrewMember m : crew) {
            boolean wasAlive = m.alive();
            m.tick(dt);
            if (wasAlive && !m.alive()) dnaVault.add(m.name());
        }
    }

    public int dnaStored() { return dnaVault.size(); }

    /** Clone a stored DNA sample into a new crew member. Returns null if the vault is empty. */
    public CrewMember cloneFromDna() {
        if (dnaVault.isEmpty()) return null;
        String donor = dnaVault.remove(dnaVault.size() - 1);
        return hire(donor + " (clone)");
    }

    /**
     * Assign the best-fit living crew member for a skill to walk to a location.
     * Returns the chosen member, or null if no living crew exist.
     */
    public CrewMember assignBestFit(CrewMember.Skill skill, ShipLocation dest) {
        CrewMember best = null;
        int bestScore = -1;
        for (CrewMember m : crew) {
            if (!m.alive()) continue;
            int score = m.aptitude(skill);
            if (score > bestScore) { bestScore = score; best = m; }
        }
        if (best != null) best.orderTo(dest);
        return best;
    }
}

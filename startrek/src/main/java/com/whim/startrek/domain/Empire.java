package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A playable or AI empire of a given {@link Race}. Owns a treasury per resource,
 * a research level per tech tree (clamped to the race's cap), fleets, and systems.
 */
public class Empire {

    private final Race race;
    private EmpireStatus status = EmpireStatus.PEACE;
    private boolean player;

    private final Map<ResourceType, Long> treasury = new EnumMap<ResourceType, Long>(ResourceType.class);
    private final Map<TechType, Integer> techLevels = new EnumMap<TechType, Integer>(TechType.class);
    private final List<Fleet> fleets = new ArrayList<Fleet>();
    private final List<StarSystem> systems = new ArrayList<StarSystem>();

    public Empire(Race race) {
        this.race = race;
    }

    public Race getRace() {
        return race;
    }

    public EmpireStatus getStatus() {
        return status;
    }

    public void setStatus(EmpireStatus s) {
        this.status = s;
    }

    public long getTreasury(ResourceType r) {
        Long n = treasury.get(r);
        return n == null ? 0L : n.longValue();
    }

    public void setTreasury(ResourceType r, long amt) {
        treasury.put(r, amt);
    }

    public void addTreasury(ResourceType r, long delta) {
        treasury.put(r, getTreasury(r) + delta);
    }

    public int getTechLevel(TechType t) {
        Integer n = techLevels.get(t);
        return n == null ? 0 : n.intValue();
    }

    /** Sets the tech level, clamped to [0, race cap] for the tree. */
    public void setTechLevel(TechType t, int level) {
        int clamped = level;
        if (clamped < 0) {
            clamped = 0;
        }
        int cap = race.getCap(t);
        if (clamped > cap) {
            clamped = cap;
        }
        techLevels.put(t, clamped);
    }

    public List<Fleet> getFleets() {
        return fleets;
    }

    public List<StarSystem> getSystems() {
        return systems;
    }

    public boolean isPlayer() {
        return player;
    }

    public void setPlayer(boolean b) {
        this.player = b;
    }
}

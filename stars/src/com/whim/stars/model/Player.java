package com.whim.stars.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.ShipDesign;

/**
 * A player (human or AI): their race, tech levels, research policy, ship
 * designs, and diplomatic stance toward the other players. Owned planets and
 * fleets live in the {@link Galaxy} and are resolved by this player's id.
 */
public final class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Which field to research next once the current one levels up. */
    public enum NextFieldPolicy {
        SAME("Same field"),
        LOWEST("Lowest field"),
        NEXT_CHEAPEST("Next cheapest field");

        private final String label;
        NextFieldPolicy(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Diplomatic stance toward another player. */
    public enum Relation {
        FRIEND, NEUTRAL, ENEMY
    }

    private final int id;
    private String name;
    private final Race race;
    private final boolean ai;
    private int colorRgb; // packed 0xRRGGBB, used by the UI only

    private final TechLevels tech = new TechLevels();
    private final long[] researchPoints = new long[TechField.values().length];
    private TechField currentResearch = TechField.ENERGY;
    private NextFieldPolicy nextFieldPolicy = NextFieldPolicy.LOWEST;
    private int researchBudgetPercent = 15; // % of resources routed to research

    private final List<ShipDesign> designs = new ArrayList<ShipDesign>();
    private final Map<Integer, Relation> relations = new HashMap<Integer, Relation>();

    public Player(int id, String name, Race race, boolean ai) {
        this.id = id;
        this.name = name;
        this.race = race;
        this.ai = ai;
    }

    public int id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public Race race() { return race; }
    public boolean isAi() { return ai; }
    public int colorRgb() { return colorRgb; }
    public void setColorRgb(int rgb) { this.colorRgb = rgb; }

    public TechLevels tech() { return tech; }

    public long researchPoints(TechField f) { return researchPoints[f.ordinal()]; }
    public void addResearchPoints(TechField f, long points) {
        researchPoints[f.ordinal()] = Math.max(0, researchPoints[f.ordinal()] + points);
    }
    public void setResearchPoints(TechField f, long points) {
        researchPoints[f.ordinal()] = Math.max(0, points);
    }

    public TechField currentResearch() { return currentResearch; }
    public void setCurrentResearch(TechField f) { this.currentResearch = f; }
    public NextFieldPolicy nextFieldPolicy() { return nextFieldPolicy; }
    public void setNextFieldPolicy(NextFieldPolicy p) { this.nextFieldPolicy = p; }
    public int researchBudgetPercent() { return researchBudgetPercent; }
    public void setResearchBudgetPercent(int pct) {
        this.researchBudgetPercent = Math.max(0, Math.min(100, pct));
    }

    public List<ShipDesign> designs() { return designs; }
    public void addDesign(ShipDesign design) { designs.add(design); }

    public Relation relationTo(int otherPlayerId) {
        Relation r = relations.get(otherPlayerId);
        return r == null ? Relation.NEUTRAL : r;
    }
    public void setRelation(int otherPlayerId, Relation relation) {
        relations.put(otherPlayerId, relation);
    }
    public boolean isEnemy(int otherPlayerId) {
        return otherPlayerId != id && relationTo(otherPlayerId) == Relation.ENEMY;
    }

    @Override
    public String toString() {
        return name;
    }
}

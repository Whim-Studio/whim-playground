package com.whim.coda.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The central character state object both the engine and UI read/write. */
public class CharacterSheet {

    private String name = "";
    private Species species;
    private final AttributeSet attributes = new AttributeSet();
    private final List<SkillRank> skills = new ArrayList<SkillRank>();
    private final List<Edge> edges = new ArrayList<Edge>();
    private final List<Flaw> flaws = new ArrayList<Flaw>();

    // Derived values, cached here by the engine after recompute.
    private int health;
    private int defense;
    private int courage;
    private int renown;
    private final Map<Reaction, Integer> reactions = new EnumMap<Reaction, Integer>(Reaction.class);

    public CharacterSheet() {
        for (Reaction r : Reaction.values()) {
            reactions.put(r, 0);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species s) {
        this.species = s;
    }

    /** Never null. */
    public AttributeSet getAttributes() {
        return attributes;
    }

    /** Mutable list. */
    public List<SkillRank> getSkills() {
        return skills;
    }

    /** Mutable list. */
    public List<Edge> getEdges() {
        return edges;
    }

    /** Mutable list. */
    public List<Flaw> getFlaws() {
        return flaws;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int v) {
        this.health = v;
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int v) {
        this.defense = v;
    }

    public int getReaction(Reaction r) {
        Integer v = reactions.get(r);
        return v == null ? 0 : v.intValue();
    }

    public void setReaction(Reaction r, int v) {
        reactions.put(r, v);
    }

    public int getCourage() {
        return courage;
    }

    public void setCourage(int v) {
        this.courage = v;
    }

    public int getRenown() {
        return renown;
    }

    public void setRenown(int v) {
        this.renown = v;
    }
}

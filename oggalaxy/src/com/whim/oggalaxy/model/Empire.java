package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A player or AI empire. Holds its class, difficulty (AI only), empire-wide tech levels,
 * planets, current research, score, dark-matter balance and a per-empire seeded
 * {@link Random} used for AI decisions and moon/expedition rolls. Implements
 * {@link Views.EmpireView}.
 */
public final class Empire implements Views.EmpireView, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String name;
    public boolean ai;
    public boolean player;
    public Ids.PlayerClass playerClass;
    public Ids.Difficulty difficulty;   // null for the human player
    public boolean alive = true;
    public long score;
    public double darkMatter;

    public final Map<Ids.TechType, Integer> tech = new EnumMap<Ids.TechType, Integer>(Ids.TechType.class);
    public final List<Planet> planets = new ArrayList<Planet>();
    public Job research;                 // empire-wide research in progress, or null

    /** Per-empire deterministic RNG (seeded from the master seed at game creation). */
    public Random rng = new Random(0);

    public Empire() {
    }

    public int techLevelOf(Ids.TechType t) {
        Integer v = tech.get(t);
        return v == null ? 0 : v;
    }

    public void setTech(Ids.TechType t, int level) {
        tech.put(t, level);
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public boolean isAI() { return ai; }
    @Override public boolean isPlayer() { return player; }
    @Override public Ids.PlayerClass playerClass() { return playerClass; }
    @Override public Ids.Difficulty difficulty() { return difficulty; }
    @Override public boolean alive() { return alive; }
    @Override public long score() { return score; }
    @Override public int techLevel(Ids.TechType type) { return techLevelOf(type); }
    @Override public List<Views.PlanetView> planets() {
        return new ArrayList<Views.PlanetView>(planets);
    }
    @Override public Views.QueueItemView currentResearch() { return research; }
}

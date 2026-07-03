package com.arpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An explorable zone. Holds weighted enemy spawn definitions and an ordered list
 * of encounters (the "path" through the realm). The engine reads these to build
 * fights; the model only stores the definitions.
 */
public final class Realm implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Kinds of encounter a realm path can contain. */
    public enum EncounterType {
        COMBAT,
        ELITE,
        BOSS,
        TREASURE,
        REST,
        EVENT
    }

    /** A weighted reference to an enemy template that can spawn in this realm. */
    public static final class SpawnDef implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final Enemy template;
        private final int weight;

        public SpawnDef(Enemy template, int weight) {
            if (template == null) {
                throw new IllegalArgumentException("SpawnDef template must not be null");
            }
            this.template = template;
            this.weight = Math.max(1, weight);
        }

        public Enemy getTemplate() {
            return template;
        }

        public int getWeight() {
            return weight;
        }
    }

    /** A single node on the realm path. */
    public static final class EncounterDef implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String name;
        private final String description;
        private final EncounterType type;
        private final String bossEnemyId; // set for BOSS nodes; may be null

        public EncounterDef(String id, String name, String description, EncounterType type,
                            String bossEnemyId) {
            if (type == null) {
                throw new IllegalArgumentException("EncounterDef type must not be null");
            }
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.bossEnemyId = bossEnemyId;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public EncounterType getType() {
            return type;
        }

        public String getBossEnemyId() {
            return bossEnemyId;
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final int difficultyTier;
    private final int recommendedLevel;
    private final List<SpawnDef> spawns;
    private final List<EncounterDef> encounters;

    public Realm(String id, String name, String description, int difficultyTier, int recommendedLevel) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Realm id must not be blank");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficultyTier = Math.max(1, difficultyTier);
        this.recommendedLevel = Math.max(1, recommendedLevel);
        this.spawns = new ArrayList<SpawnDef>();
        this.encounters = new ArrayList<EncounterDef>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDifficultyTier() {
        return difficultyTier;
    }

    public int getRecommendedLevel() {
        return recommendedLevel;
    }

    public Realm addSpawn(Enemy template, int weight) {
        spawns.add(new SpawnDef(template, weight));
        return this;
    }

    public Realm addEncounter(EncounterDef encounter) {
        if (encounter != null) {
            encounters.add(encounter);
        }
        return this;
    }

    public List<SpawnDef> getSpawns() {
        return new ArrayList<SpawnDef>(spawns);
    }

    public List<EncounterDef> getEncounters() {
        return new ArrayList<EncounterDef>(encounters);
    }

    public int getSpawnTotalWeight() {
        int total = 0;
        for (int i = 0; i < spawns.size(); i++) {
            total += spawns.get(i).getWeight();
        }
        return total;
    }

    /** Weighted spawn lookup given a supplied roll in {@code [0, getSpawnTotalWeight())}. */
    public Enemy pickSpawn(int roll) {
        if (spawns.isEmpty()) {
            return null;
        }
        int cursor = roll;
        for (int i = 0; i < spawns.size(); i++) {
            SpawnDef s = spawns.get(i);
            if (cursor < s.getWeight()) {
                return s.getTemplate();
            }
            cursor -= s.getWeight();
        }
        return spawns.get(spawns.size() - 1).getTemplate();
    }

    @Override
    public String toString() {
        return name + " (Tier " + difficultyTier + ", rec. Lv " + recommendedLevel + ")";
    }
}

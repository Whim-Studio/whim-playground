package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Enemy;
import com.whim.ruinlander.domain.EntityType;
import com.whim.ruinlander.domain.Faction;
import com.whim.ruinlander.domain.TerrainType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Decides whether an overworld step triggers a random encounter and, if so,
 * fabricates the hostile group. Difficulty scales mildly with the turn count.
 * All rolls use the shared seeded {@link Random}.
 */
public class EncounterGenerator {

    private final Random rng;

    public EncounterGenerator(Random rng) {
        this.rng = rng;
    }

    /** Returns the enemies for a triggered encounter, or an empty list if none. */
    public List<Enemy> maybeEncounter(TerrainType terrain, int turnCount) {
        List<Enemy> enemies = new ArrayList<Enemy>();
        double chance = baseChance(terrain) + Math.min(0.15, turnCount * 0.004);
        if (rng.nextDouble() >= chance) {
            return enemies; // no encounter
        }

        int tier = 1 + turnCount / 20; // gentle scaling
        int count = 1 + (rng.nextDouble() < 0.35 ? 1 : 0) + (terrain == TerrainType.CITY && rng.nextDouble() < 0.3 ? 1 : 0);
        for (int i = 0; i < count; i++) {
            enemies.add(spawn(terrain, tier));
        }
        return enemies;
    }

    private double baseChance(TerrainType terrain) {
        switch (terrain) {
            case TOXIC:
                return 0.40;
            case CITY:
                return 0.35;
            case WASTELAND:
                return 0.25;
            case FOREST:
                return 0.20;
            case ROAD:
                return 0.10;
            case SETTLEMENT:
                return 0.02;
            default:
                return 0.0;
        }
    }

    private Enemy spawn(TerrainType terrain, int tier) {
        double r = rng.nextDouble();
        if (terrain == TerrainType.TOXIC || (terrain == TerrainType.CITY && r < 0.4)) {
            int hp = 30 + tier * 6;
            return new Enemy(EntityType.MUTANT, "Glow Mutant", hp, 5,
                    10 + tier * 2, 0.55, 0.15, Faction.NEUTRAL, null);
        }
        if (terrain == TerrainType.FOREST || r < 0.35) {
            int hp = 12 + tier * 3;
            return new Enemy(EntityType.ANIMAL, "Wild Dog", hp, 8,
                    5 + tier, 0.62, 0.0, Faction.NEUTRAL, null);
        }
        int hp = 20 + tier * 5;
        return new Enemy(EntityType.RAIDER, "Raider", hp, 6,
                7 + tier * 2, 0.66, 0.05, Faction.RAIDERS, null);
    }
}

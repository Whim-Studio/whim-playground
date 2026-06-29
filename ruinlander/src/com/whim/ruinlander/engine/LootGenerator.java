package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Inventory;
import com.whim.ruinlander.domain.Item;
import com.whim.ruinlander.domain.TerrainType;

import java.util.Random;

/**
 * Fills container loot for a terrain using the shared seeded {@link Random}.
 * Items are pulled from {@link ItemDb}.
 */
public class LootGenerator {

    private final Random rng;

    public LootGenerator(Random rng) {
        this.rng = rng;
    }

    /** Populate {@code loot} with a terrain-appropriate spread of supplies. */
    public void fill(Inventory loot, TerrainType terrain) {
        int rolls = 2 + rng.nextInt(3); // 2..4 picks
        for (int i = 0; i < rolls; i++) {
            String id = pick(terrain);
            Item it = ItemDb.get(id);
            if (it != null) {
                loot.add(it, 1 + rng.nextInt(2));
            }
        }
    }

    private String pick(TerrainType terrain) {
        double r = rng.nextDouble();
        switch (terrain) {
            case CITY:
                if (r < 0.25) return "scrap";
                if (r < 0.45) return "ammo_9mm";
                if (r < 0.60) return "bandage";
                if (r < 0.75) return "canned_food";
                if (r < 0.88) return "gunpowder";
                return "cloth";
            case TOXIC:
                if (r < 0.35) return "rad_away";
                if (r < 0.55) return "scrap";
                if (r < 0.75) return "stimpak";
                return "dirty_water";
            case FOREST:
                if (r < 0.45) return "raw_meat";
                if (r < 0.70) return "cloth";
                if (r < 0.85) return "dirty_water";
                return "bandage";
            default: // WASTELAND, ROAD, etc.
                if (r < 0.30) return "scrap";
                if (r < 0.50) return "dirty_water";
                if (r < 0.68) return "canned_food";
                if (r < 0.82) return "cloth";
                return "bandage";
        }
    }
}

package com.whim.nextrun.engine;

import com.whim.nextrun.domain.Enemy;
import com.whim.nextrun.domain.EntityType;
import com.whim.nextrun.domain.GridMap;
import com.whim.nextrun.domain.Position;
import com.whim.nextrun.domain.Tile;

import java.util.Random;

/**
 * When the doom counter hits zero, a wave of exponentially stronger enemies is
 * dropped onto empty tiles across the map. Each wave raises the enemy tier.
 */
public final class WaveSpawner {

    private static final String[] WEAK   = { "Imp", "Hellhound", "Cinder Wisp" };
    private static final String[] MID    = { "Brimstone Ogre", "Soul Reaver", "Ashen Knight" };
    private static final String[] STRONG = { "Archdemon", "Hell Baron", "Wrath Titan" };

    private WaveSpawner() {}

    /** Build a single enemy scaled to {@code tier} (0 = weakest). */
    public static Enemy makeEnemy(int tier, Random rng) {
        // Exponential-ish scaling: stats grow ~35% per tier.
        double mult = Math.pow(1.35, tier);
        String name;
        if (tier <= 1) name = WEAK[rng.nextInt(WEAK.length)];
        else if (tier <= 3) name = MID[rng.nextInt(MID.length)];
        else name = STRONG[rng.nextInt(STRONG.length)];

        int hp = (int) Math.round((6 + rng.nextInt(5)) * mult);
        int atk = (int) Math.round((3 + rng.nextInt(3)) * mult);
        int def = (int) Math.round((1 + rng.nextInt(3)) * mult);
        int bribe = (int) Math.round((6 + rng.nextInt(8)) * mult);
        int sneakDc = (int) Math.round((4 + rng.nextInt(4)) * mult);
        return new Enemy(name, hp, atk, def, bribe, sneakDc, tier);
    }

    /** Drop {@code count} enemies of the given tier onto random empty tiles. */
    public static int spawnWave(GridMap map, int tier, int count, Random rng,
                                Position avoid) {
        int placed = 0;
        int guard = 0;
        while (placed < count && guard < count * 80) {
            guard++;
            int x = rng.nextInt(map.width);
            int y = rng.nextInt(map.height);
            Position p = new Position(x, y);
            if (p.manhattan(avoid) <= 1) continue; // never spawn on top of the player
            Tile t = map.at(p);
            if (t.type != EntityType.EMPTY) continue;
            t.type = EntityType.ENEMY;
            t.enemy = makeEnemy(tier, rng);
            t.discovered = true; // a wave announces itself
            placed++;
        }
        return placed;
    }
}

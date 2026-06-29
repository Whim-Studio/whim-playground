package com.whim.nextrun.engine;

import com.whim.nextrun.domain.Enemy;
import com.whim.nextrun.domain.EntityType;
import com.whim.nextrun.domain.GridMap;
import com.whim.nextrun.domain.Position;
import com.whim.nextrun.domain.Tile;

import java.util.Random;

/** Procedurally scatters resources, gold, ruins, enemies and the exit portal. */
public final class MapGenerator {

    private final Random rng;

    public MapGenerator(Random rng) {
        this.rng = rng;
    }

    public GridMap generate(int width, int height, Position playerStart) {
        GridMap map = new GridMap(width, height);
        int cells = width * height;

        int resources = cells / 7;
        int golds = cells / 12;
        int ruins = cells / 16;
        int enemies = cells / 10;

        scatter(map, EntityType.RESOURCE, resources, playerStart, 0);
        scatter(map, EntityType.GOLD_PILE, golds, playerStart, 0);
        scatter(map, EntityType.RUIN, ruins, playerStart, 0);
        scatter(map, EntityType.ENEMY, enemies, playerStart, 1);

        // The exit portal: placed far from the start so reaching it is a journey.
        placePortal(map, playerStart);
        return map;
    }

    private void scatter(GridMap map, EntityType type, int count, Position avoid, int tier) {
        int placed = 0;
        int guard = 0;
        while (placed < count && guard < count * 50) {
            guard++;
            int x = rng.nextInt(map.width);
            int y = rng.nextInt(map.height);
            Position p = new Position(x, y);
            Tile t = map.at(p);
            if (t.type != EntityType.EMPTY) continue;
            if (p.manhattan(avoid) <= 2) continue; // keep the start area clear
            t.type = type;
            switch (type) {
                case RESOURCE:  t.payload = 2 + rng.nextInt(3); break;
                case GOLD_PILE: t.payload = 5 + rng.nextInt(15); break;
                case RUIN:      t.payload = 0; break;
                case ENEMY:     t.enemy = WaveSpawner.makeEnemy(tier, rng); break;
                default: break;
            }
            placed++;
        }
    }

    private void placePortal(GridMap map, Position start) {
        int bestX = map.width - 1, bestY = map.height - 1;
        // pick the empty tile farthest from start
        int best = -1;
        for (int x = 0; x < map.width; x++) {
            for (int y = 0; y < map.height; y++) {
                Position p = new Position(x, y);
                if (map.at(p).type != EntityType.EMPTY) continue;
                int d = p.manhattan(start);
                if (d > best) { best = d; bestX = x; bestY = y; }
            }
        }
        Tile t = map.at(bestX, bestY);
        t.type = EntityType.PORTAL;
    }
}

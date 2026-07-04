// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;

/** Builds the first playable level (placeholder content until Task 1 lands). */
public final class LevelBuilder {
    private LevelBuilder() {}

    public static WorldState firstLevel() {
        WorldState w = new WorldState();
        w.levelWidth = Config.LEVEL_W;

        // Continuous lower-plane ground across the whole level.
        w.platforms.add(new Platform(0, Config.GROUND_Y_LOWER, Config.LEVEL_W, 64, Enums.Plane.LOWER));

        // Several raised upper-plane segments (the background path).
        int[][] upper = {
            {0, 640}, {900, 1500}, {1800, 2600}, {3000, 3700}
        };
        for (int i = 0; i < upper.length; i++) {
            int x0 = upper[i][0];
            int x1 = upper[i][1];
            w.platforms.add(new Platform(x0, Config.GROUND_Y_UPPER, x1 - x0, 24, Enums.Plane.UPPER));
        }

        // Player start on the lower plane, feet on ground.
        w.player.plane = Enums.Plane.LOWER;
        w.player.box.x = 60;
        w.player.box.y = Config.GROUND_Y_LOWER - Config.ENTITY_H;
        w.player.grounded = true;

        // Hostages spread across both planes.
        addHostage(w, 520, Enums.Plane.LOWER, Enums.RescueReward.POINTS);
        addHostage(w, 1200, Enums.Plane.UPPER, Enums.RescueReward.WEAPON_UPGRADE);
        addHostage(w, 2200, Enums.Plane.LOWER, Enums.RescueReward.EXTRA_NINJUTSU);
        addHostage(w, 3300, Enums.Plane.UPPER, Enums.RescueReward.WEAPON_UPGRADE);
        w.hostagesTotal = w.hostages.size();

        // Enemy spawns (mix of THUG + NINJA on both planes).
        addEnemy(w, Enums.EnemyType.THUG,  360, Enums.Plane.LOWER, 300, 520);
        addEnemy(w, Enums.EnemyType.NINJA, 780, Enums.Plane.LOWER, 700, 900);
        addEnemy(w, Enums.EnemyType.THUG, 1100, Enums.Plane.UPPER, 950, 1450);
        addEnemy(w, Enums.EnemyType.NINJA, 1600, Enums.Plane.LOWER, 1500, 1800);
        addEnemy(w, Enums.EnemyType.THUG, 2400, Enums.Plane.LOWER, 2200, 2600);
        addEnemy(w, Enums.EnemyType.NINJA, 3100, Enums.Plane.UPPER, 3050, 3650);

        return w;
    }

    private static void addHostage(WorldState w, double x, Enums.Plane p, Enums.RescueReward reward) {
        double groundY = (p == Enums.Plane.LOWER) ? Config.GROUND_Y_LOWER : Config.GROUND_Y_UPPER;
        w.hostages.add(new Hostage(x, groundY - 40, p, reward));
    }

    private static void addEnemy(WorldState w, Enums.EnemyType type, double x, Enums.Plane p,
                                 double patrolMin, double patrolMax) {
        Enemy e = new Enemy(type);
        e.plane = p;
        double groundY = (p == Enums.Plane.LOWER) ? Config.GROUND_Y_LOWER : Config.GROUND_Y_UPPER;
        e.box.x = x;
        e.box.y = groundY - Config.ENTITY_H;
        e.grounded = true;
        e.patrolMinX = patrolMin;
        e.patrolMaxX = patrolMax;
        e.facing = Enums.Facing.LEFT;
        w.enemies.add(e);
    }
}

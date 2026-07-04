package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;

/**
 * Builds the fully-populated first stage. {@link #firstLevel()} returns a live,
 * playable {@link WorldState}: continuous LOWER-plane ground across
 * {@link Config#LEVEL_W}, several raised UPPER-plane segments, 3-5 hostages
 * spread across both planes, and a THUG/NINJA enemy mix.
 *
 * Content only — no behavior. The engine advances whatever this produces.
 */
public final class LevelBuilder {
    private LevelBuilder() {}

    /** Ground strip thickness below the feet line, in world pixels. */
    private static final int GROUND_THICKNESS = 96;
    private static final int LEDGE_THICKNESS = 18;
    private static final int HOSTAGE_W = 20;
    private static final int HOSTAGE_H = 40;

    public static WorldState firstLevel() {
        LevelMap map = new LevelMap(Config.LEVEL_W);

        // --- Continuous LOWER-plane ground across the whole level ---
        map.addPlatform(new Platform(
                new Aabb(0, Config.GROUND_Y_LOWER, Config.LEVEL_W, GROUND_THICKNESS),
                Enums.Plane.LOWER));

        // --- Raised UPPER-plane background segments (the background path) ---
        // Each is a ledge whose TOP surface sits at GROUND_Y_UPPER.
        addUpperLedge(map, 256, 640);
        addUpperLedge(map, 1120, 720);
        addUpperLedge(map, 2080, 560);
        addUpperLedge(map, 2880, 700);
        addUpperLedge(map, 3760, 336);

        // --- Hostages spread across BOTH planes (4 total) ---
        map.addHostage(hostage(map, 520, Enums.Plane.LOWER, Enums.RescueReward.POINTS));
        map.addHostage(hostage(map, 1360, Enums.Plane.UPPER, Enums.RescueReward.WEAPON_UPGRADE));
        map.addHostage(hostage(map, 2600, Enums.Plane.LOWER, Enums.RescueReward.EXTRA_NINJUTSU));
        map.addHostage(hostage(map, 3080, Enums.Plane.UPPER, Enums.RescueReward.WEAPON_UPGRADE));

        // --- Enemy spawns: THUG + NINJA mix over both planes ---
        map.addEnemySpawn(new LevelMap.EnemySpawn(700,  Enums.Plane.LOWER, Enums.EnemyType.THUG));
        map.addEnemySpawn(new LevelMap.EnemySpawn(1180, Enums.Plane.UPPER, Enums.EnemyType.NINJA));
        map.addEnemySpawn(new LevelMap.EnemySpawn(1600, Enums.Plane.LOWER, Enums.EnemyType.THUG));
        map.addEnemySpawn(new LevelMap.EnemySpawn(2150, Enums.Plane.UPPER, Enums.EnemyType.THUG));
        map.addEnemySpawn(new LevelMap.EnemySpawn(2500, Enums.Plane.LOWER, Enums.EnemyType.NINJA));
        map.addEnemySpawn(new LevelMap.EnemySpawn(3000, Enums.Plane.LOWER, Enums.EnemyType.THUG));
        map.addEnemySpawn(new LevelMap.EnemySpawn(3600, Enums.Plane.LOWER, Enums.EnemyType.NINJA));

        return assemble(map);
    }

    /** Turn static map data into a live world with player, enemies, hostages. */
    private static WorldState assemble(LevelMap map) {
        WorldState world = new WorldState(map);

        // Player starts grounded on the LOWER plane near the left edge.
        double startX = 64;
        double startGroundY = map.groundYFor(Enums.Plane.LOWER, startX + Config.ENTITY_W * 0.5);
        Aabb pbox = new Aabb(startX, startGroundY - Config.ENTITY_H, Config.ENTITY_W, Config.ENTITY_H);
        Player player = new Player(pbox, Enums.Plane.LOWER);
        world.setPlayer(player);

        // Platforms (shared references — geometry is immutable).
        for (int i = 0; i < map.platforms().size(); i++) {
            world.platformList().add(map.platforms().get(i));
        }

        // Hostages.
        for (int i = 0; i < map.hostages().size(); i++) {
            world.hostageList().add(map.hostages().get(i));
        }
        world.setHostagesTotal(world.hostageList().size());

        // Instantiate enemies from spawn descriptors, grounded on their plane.
        for (int i = 0; i < map.enemySpawns().size(); i++) {
            LevelMap.EnemySpawn s = map.enemySpawns().get(i);
            double gy = map.groundYFor(s.plane, s.x + Config.ENTITY_W * 0.5);
            Aabb ebox = new Aabb(s.x, gy - Config.ENTITY_H, Config.ENTITY_W, Config.ENTITY_H);
            Enemy e = new Enemy(ebox, s.plane, s.type);
            world.enemyList().add(e);
        }

        return world;
    }

    private static void addUpperLedge(LevelMap map, int x, int width) {
        map.addPlatform(new Platform(
                new Aabb(x, Config.GROUND_Y_UPPER, width, LEDGE_THICKNESS),
                Enums.Plane.UPPER));
    }

    private static Hostage hostage(LevelMap map, double x, Enums.Plane plane, Enums.RescueReward reward) {
        double gy = map.groundYFor(plane, x + HOSTAGE_W * 0.5);
        Aabb box = new Aabb(x, gy - HOSTAGE_H, HOSTAGE_W, HOSTAGE_H);
        return new Hostage(box, plane, reward);
    }
}

package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.AiState;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.NodeType;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.api.Enums.Terrain;
import com.whim.kenshi.api.Enums.WeaponClass;

import java.util.Random;

/**
 * Builds a fully populated {@link WorldState} from a seed: a procedural map with
 * a handful of towns, a 3-member PLAYER squad near the starting town, town
 * guards, several bandit squads roaming the wastes, and wandering drifters.
 *
 * <p>Deterministic: the same seed always yields the same world. Contains no
 * simulation logic — only initial placement and stat seeding.
 */
public final class WorldBuilder {

    private WorldBuilder() {}

    /** Names used to flavour generated characters. */
    private static final String[] FIRST_NAMES = {
        "Beep", "Ruka", "Shryke", "Ells", "Kang", "Hobbs", "Silla", "Mordo",
        "Vain", "Cutter", "Grim", "Sadie", "Nomad", "Cinder", "Ozric", "Tulan"
    };

    public static WorldState build(long seed) {
        Random rng = new Random(seed);

        MapGrid map = new MapGrid(seed);
        FactionMatrix factions = new FactionMatrix();
        WorldState world = new WorldState(map, factions);

        // ---- towns ------------------------------------------------------
        // Place towns on non-water tiles spread across the map. The first is the
        // player's starting town.
        int[][] townTiles = {
            { 24, 60 }, // starting town (Hub-ish, lower-left)
            { 66, 30 }, // Holy Nation town (upper-right)
            { 70, 66 }, // Shek town (lower-right)
            { 40, 40 }  // Trade Guild waystation (centre)
        };
        FactionId[] townOwners = {
            FactionId.TRADE_GUILD, FactionId.HOLY_NATION, FactionId.SHEK, FactionId.TRADE_GUILD
        };
        String[] townNames = { "The Hub", "Bad Teeth", "Squin", "World's End" };

        for (int i = 0; i < townTiles.length; i++) {
            int col = clampTile(townTiles[i][0]);
            int row = clampTile(townTiles[i][1]);
            map.stampTown(col, row, 2);
            double wx = (col + 0.5) * Config.TILE_SIZE;
            double wy = (row + 0.5) * Config.TILE_SIZE;
            String tid = "town" + i;
            world.addNode(new WorldNode(tid, townNames[i], NodeType.TOWN,
                    townOwners[i], wx, wy, Config.TILE_SIZE * 2.5));
            // each town gets a bar just off-centre
            world.addNode(new WorldNode("bar" + i, townNames[i] + " Bar", NodeType.BAR,
                    townOwners[i], wx + Config.TILE_SIZE, wy - Config.TILE_SIZE,
                    Config.TILE_SIZE * 0.8));
        }

        // a lonely ruin and a bandit camp for flavour
        double ruinX = (clampTile(52) + 0.5) * Config.TILE_SIZE;
        double ruinY = (clampTile(14) + 0.5) * Config.TILE_SIZE;
        world.addNode(new WorldNode("ruin0", "Sunken Ruins", NodeType.RUIN,
                FactionId.DRIFTERS, ruinX, ruinY, Config.TILE_SIZE * 1.5));

        double campX = (clampTile(14) + 0.5) * Config.TILE_SIZE;
        double campY = (clampTile(30) + 0.5) * Config.TILE_SIZE;
        world.addNode(new WorldNode("camp0", "Dust Bandit Camp", NodeType.CAMP,
                FactionId.DUST_BANDITS, campX, campY, Config.TILE_SIZE * 1.5));

        // ---- player squad ----------------------------------------------
        double startX = (clampTile(townTiles[0][0]) + 0.5) * Config.TILE_SIZE;
        double startY = (clampTile(townTiles[0][1]) + 0.5) * Config.TILE_SIZE;
        Squad player = new Squad("squad_player", "Wanderers", FactionId.PLAYER);
        world.addSquad(player);
        String[] heroNames = { "Beep", "Ruka", "Shryke" };
        WeaponClass[] heroWeapons = { WeaponClass.ONE_HANDED, WeaponClass.TWO_HANDED, WeaponClass.UNARMED };
        for (int i = 0; i < 3; i++) {
            Character c = new Character("player_" + i, heroNames[i], FactionId.PLAYER,
                    startX + spread(rng, 60), startY + spread(rng, 60));
            c.setWeapon(heroWeapons[i]);
            seedSkills(c, rng, 8, 22);
            c.setAiState(AiState.IDLE);
            world.addCharacter(c);
            player.addMember(c.id());
        }

        // ---- town guards -----------------------------------------------
        int guardId = 0;
        for (int t = 0; t < townTiles.length; t++) {
            FactionId owner = townOwners[t];
            if (owner == FactionId.TRADE_GUILD || owner == FactionId.HOLY_NATION
                    || owner == FactionId.SHEK) {
                double tx = (clampTile(townTiles[t][0]) + 0.5) * Config.TILE_SIZE;
                double ty = (clampTile(townTiles[t][1]) + 0.5) * Config.TILE_SIZE;
                Squad gs = new Squad("squad_guard" + t, townNames[t] + " Guard", owner);
                world.addSquad(gs);
                int n = 2 + rng.nextInt(2);
                for (int i = 0; i < n; i++) {
                    Character g = new Character("guard_" + (guardId++), pick(rng, FIRST_NAMES),
                            owner, tx + spread(rng, 120), ty + spread(rng, 120));
                    g.setWeapon(rng.nextBoolean() ? WeaponClass.ONE_HANDED : WeaponClass.TWO_HANDED);
                    seedSkills(g, rng, 18, 40);
                    g.setAiState(AiState.PATROL);
                    world.addCharacter(g);
                    gs.addMember(g.id());
                }
            }
        }

        // ---- bandit squads ---------------------------------------------
        int banditId = 0;
        FactionId[] banditFactions = { FactionId.DUST_BANDITS, FactionId.HUNGRY_BANDITS };
        for (int s = 0; s < 3; s++) {
            FactionId bf = banditFactions[s % banditFactions.length];
            double bx = randomLandX(rng, map);
            double by = randomLandY(rng, map, bx);
            Squad bsquad = new Squad("squad_bandit" + s, bf.label() + " " + (s + 1), bf);
            world.addSquad(bsquad);
            int n = 3 + rng.nextInt(3);
            for (int i = 0; i < n; i++) {
                Character b = new Character("bandit_" + (banditId++), pick(rng, FIRST_NAMES),
                        bf, bx + spread(rng, 100), by + spread(rng, 100));
                b.setWeapon(rng.nextBoolean() ? WeaponClass.ONE_HANDED : WeaponClass.UNARMED);
                seedSkills(b, rng, 10, 30);
                // hungry bandits are, well, hungry
                if (bf == FactionId.HUNGRY_BANDITS) {
                    b.setHunger(Config.HUNGER_MAX * (0.15 + rng.nextDouble() * 0.25));
                }
                b.setAiState(AiState.WANDER);
                world.addCharacter(b);
                bsquad.addMember(b.id());
            }
        }

        // ---- drifters ---------------------------------------------------
        Squad drifters = new Squad("squad_drifters", "Drifters", FactionId.DRIFTERS);
        world.addSquad(drifters);
        int nd = 4 + rng.nextInt(3);
        for (int i = 0; i < nd; i++) {
            double dx = randomLandX(rng, map);
            double dy = randomLandY(rng, map, dx);
            Character d = new Character("drifter_" + i, pick(rng, FIRST_NAMES),
                    FactionId.DRIFTERS, dx, dy);
            d.setWeapon(WeaponClass.UNARMED);
            seedSkills(d, rng, 3, 15);
            d.setAiState(AiState.WANDER);
            world.addCharacter(d);
            drifters.addMember(d.id());
        }

        world.log("World seeded (" + seed + "): " + world.characterCount()
                + " characters, " + world.nodesList().size() + " nodes.");
        return world;
    }

    // ---- helpers --------------------------------------------------------

    private static void seedSkills(Character c, Random rng, int lo, int hi) {
        for (SkillType s : SkillType.values()) {
            int lvl = lo + rng.nextInt(Math.max(1, hi - lo + 1));
            c.skills().setLevel(s, lvl);
        }
    }

    /** A symmetric random offset in [-half, +half]. */
    private static double spread(Random rng, double half) {
        return (rng.nextDouble() * 2.0 - 1.0) * half;
    }

    private static String pick(Random rng, String[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    private static int clampTile(int t) {
        if (t < 0) return 0;
        if (t >= Config.MAP_TILES) return Config.MAP_TILES - 1;
        return t;
    }

    /** A random non-water world X (paired with {@link #randomLandY}). */
    private static double randomLandX(Random rng, MapGrid map) {
        return (2 + rng.nextInt(Config.MAP_TILES - 4) + 0.5) * Config.TILE_SIZE;
    }

    /** A random non-water world Y near the given X; falls back after tries. */
    private static double randomLandY(Random rng, MapGrid map, double worldX) {
        int col = map.colOf(worldX);
        for (int attempt = 0; attempt < 24; attempt++) {
            int row = 2 + rng.nextInt(Config.MAP_TILES - 4);
            if (map.terrain(col, row) != Terrain.WATER) {
                return (row + 0.5) * Config.TILE_SIZE;
            }
        }
        return (Config.MAP_TILES / 2 + 0.5) * Config.TILE_SIZE;
    }
}

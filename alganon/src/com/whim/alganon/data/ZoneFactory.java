package com.whim.alganon.data;

import com.whim.alganon.api.Defs.ZoneMeta;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GridPos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authors the three v1 zone blueprints (start / frontier / dungeon) as ASCII maps
 * plus placement specs. Kept in the data package so the content registry and model
 * can both read from it without touching the engine or UI.
 *
 * <p>[Gap — my design] Zone layouts, entity placements and the shared tutorial start
 * zone are original decisions per DESIGN.md §5 (2–3 explorable zones).</p>
 *
 * <p>Tile legend: '#'=WALL '.'=GRASS ','=DIRT '='=ROAD '~'=WATER 'o'=STONE 'F'=FLOOR
 * 's'=SAND ' '=VOID</p>
 */
public final class ZoneFactory {
    private ZoneFactory() {}

    public static final String START_ID = "zone_start";
    public static final String FRONTIER_ID = "zone_frontier";
    public static final String DUNGEON_ID = "zone_dungeon";

    /** Build all blueprints keyed by zone id (insertion order preserved). */
    public static Map<String, ZoneBlueprint> buildAll() {
        Map<String, ZoneBlueprint> m = new LinkedHashMap<String, ZoneBlueprint>();
        ZoneBlueprint start = start();
        ZoneBlueprint frontier = frontier();
        ZoneBlueprint dungeon = dungeon();
        m.put(start.meta.id, start);
        m.put(frontier.meta.id, frontier);
        m.put(dungeon.meta.id, dungeon);
        return m;
    }

    // ---------------------------------------------------------------- start zone

    private static ZoneBlueprint start() {
        String[] rows = {
            "########################",
            "#..........,,..........#",
            "#..o.......,,.......o...#",
            "#..........,,..........#",
            "#....~~....==....o.o....#",
            "#....~~....==..........,#",
            "#..........==..........,#",
            "#....o.....==.....~~....#",
            "#..........==.....~~....#",
            "#..,,......==..........,#",
            "#..,,......==......o....#",
            "#..........,,..........#",
            "#..........,,..........#",
            "########################",
        };
        TileType[][] tiles = parse(rows);
        ZoneMeta meta = new ZoneMeta(START_ID, "Wanderer's Rest", rows[0].length(), rows.length,
                null, false, 1,
                "A quiet waystation where new adventurers of both banners are given a road, a "
                + "meal, and a first task. The war feels very far away here.");
        ZoneBlueprint z = new ZoneBlueprint(meta, tiles);
        z.spawn = new GridPos(11, 3);
        // NPCs: an elder (quest giver), a supply vendor, a trainer.
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_elder", "Elder Maren", new GridPos(11, 2), true, false, "npc_elder"));
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_vendor", "Quartermaster Brole", new GridPos(4, 6), true, true, "npc_vendor"));
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_trainer", "Trainer Ost", new GridPos(18, 7), false, false, "npc_trainer"));
        // A few passive critters to hunt for the first quest.
        z.mobs.add(new ZoneBlueprint.MobSpec("m_rabbit_1", "mob_rabbit", new GridPos(6, 3)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_rabbit_2", "mob_rabbit", new GridPos(19, 3)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_rabbit_3", "mob_rabbit", new GridPos(7, 10)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_rabbit_4", "mob_rabbit", new GridPos(20, 10)));
        // Starter gather nodes.
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_copper", new GridPos(3, 2)));
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_bloodroot", new GridPos(19, 2)));
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_copper", new GridPos(5, 7)));
        // Portal south along the road to the frontier.
        z.portals.add(new ZoneBlueprint.PortalSpec(new GridPos(11, 12), FRONTIER_ID, "To the Ashen Frontier"));
        return z;
    }

    // ------------------------------------------------------------- frontier zone

    private static ZoneBlueprint frontier() {
        String[] rows = {
            "############################",
            "#....,,.........o.o........#",
            "#..==........~~~~~.........#",
            "#..==....o...~~~~~....,,,..#",
            "#..==........~~~~.....,,,..#",
            "#..==...............o......#",
            "#..==....o.......o.........#",
            "#..======================..#",
            "#.......,,,..........==....#",
            "#..o....,,,..........==..o.#",
            "#.......,,,..........==....#",
            "#....o..........o....==....#",
            "#..s.s..............,==,...#",
            "#..s.s...o.....o....,==,...#",
            "#...................,,==,,.#",
            "############################",
        };
        TileType[][] tiles = parse(rows);
        ZoneMeta meta = new ZoneMeta(FRONTIER_ID, "Ashen Frontier", rows[0].length(), rows.length,
                null, false, 3,
                "Contested scrubland between the two realms. Beasts and bandits roam the tree "
                + "line, ore and bloodroot are plentiful, and a ruined watchtower changes hands "
                + "as the distant war ebbs and flows.");
        ZoneBlueprint z = new ZoneBlueprint(meta, tiles);
        z.spawn = new GridPos(4, 2);
        // A scout who hands out frontier bounties, and a travelling family vendor.
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_scout", "Scout Hedda", new GridPos(3, 1), true, false, "npc_scout"));
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_famvendor", "Peddler Yann", new GridPos(24, 3), false, true, "npc_famvendor"));
        // A contested watchtower marker NPC (flavor for the faction-war sim).
        z.npcs.add(new ZoneBlueprint.NpcSpec("npc_tower", "Watchtower Warden", new GridPos(16, 6), false, false, "npc_tower"));
        // Mobs across all three behaviours.
        z.mobs.add(new ZoneBlueprint.MobSpec("m_boar_1", "mob_boar", new GridPos(5, 3)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_boar_2", "mob_boar", new GridPos(9, 6)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_wolf_1", "mob_wolf", new GridPos(8, 9)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_wolf_2", "mob_wolf", new GridPos(14, 11)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_wolf_3", "mob_wolf", new GridPos(6, 13)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_spider_1", "mob_spider", new GridPos(24, 9)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_bandit_1", "mob_bandit", new GridPos(16, 13)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_bandit_2", "mob_bandit", new GridPos(11, 12)));
        // Richer gather nodes.
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_copper", new GridPos(9, 3)));
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_richcopper", new GridPos(9, 6)));
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_bloodroot", new GridPos(1, 9)));
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_bloodroot", new GridPos(25, 9)));
        // Portals: back to the start road, and down into the dungeon.
        z.portals.add(new ZoneBlueprint.PortalSpec(new GridPos(3, 1), START_ID, "Back to Wanderer's Rest"));
        z.portals.add(new ZoneBlueprint.PortalSpec(new GridPos(21, 14), DUNGEON_ID, "Descend into the Sunken Vault"));
        return z;
    }

    // -------------------------------------------------------------- dungeon zone

    private static ZoneBlueprint dungeon() {
        String[] rows = {
            "####################",
            "#FFFF####FFFFFF#####",
            "#FFFF####FFFFFF#####",
            "#FFFF####FFFFFF#####",
            "#FFFFFFFFFFFFFF#####",
            "####FF####FF########",
            "####FF####FF########",
            "#FFFFF####FFFFFFFF##",
            "#FFFFF####FFFFFFFF##",
            "#FFFFF####FFFFFFFF##",
            "#FFFFFFFFFFFFFFFFF##",
            "#FFFFF####FFFFFFFF##",
            "#FFFFF####FFFFFFFF##",
            "####################",
        };
        TileType[][] tiles = parse(rows);
        ZoneMeta meta = new ZoneMeta(DUNGEON_ID, "The Sunken Vault", rows[0].length(), rows.length,
                null, true, 6,
                "An instanced ruin beneath the frontier. Its wardens still guard a shard of "
                + "old power long after the hands that set them here turned to dust.");
        ZoneBlueprint z = new ZoneBlueprint(meta, tiles);
        z.spawn = new GridPos(2, 4);
        // Guardians and a final warden.
        z.mobs.add(new ZoneBlueprint.MobSpec("m_guard_1", "mob_vaultguard", new GridPos(2, 2)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_guard_2", "mob_vaultguard", new GridPos(11, 2)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_guard_3", "mob_vaultguard", new GridPos(3, 8)));
        z.mobs.add(new ZoneBlueprint.MobSpec("m_warden_boss", "mob_vaultwarden", new GridPos(13, 10)));
        // A single ore vein deep inside.
        z.nodes.add(new ZoneBlueprint.NodeSpec("node_richcopper", new GridPos(15, 8)));
        // Exit back to the frontier.
        z.portals.add(new ZoneBlueprint.PortalSpec(new GridPos(2, 4), FRONTIER_ID, "Climb back to the Frontier"));
        return z;
    }

    // ------------------------------------------------------------------- parsing

    private static TileType[][] parse(String[] rows) {
        int h = rows.length;
        int w = rows[0].length();
        TileType[][] tiles = new TileType[h][w];
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w; x++) {
                char c = x < row.length() ? row.charAt(x) : '#';
                tiles[y][x] = fromChar(c);
            }
        }
        return tiles;
    }

    private static TileType fromChar(char c) {
        switch (c) {
            case '#': return TileType.WALL;
            case '.': return TileType.GRASS;
            case ',': return TileType.DIRT;
            case '=': return TileType.ROAD;
            case '~': return TileType.WATER;
            case 'o': return TileType.STONE;
            case 'F': return TileType.FLOOR;
            case 's': return TileType.SAND;
            case ' ': return TileType.VOID;
            default:  return TileType.GRASS;
        }
    }
}

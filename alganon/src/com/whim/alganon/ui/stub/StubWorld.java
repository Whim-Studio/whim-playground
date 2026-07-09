package com.whim.alganon.ui.stub;

import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A small hand-built frontier zone so the world renderer + minimap have something to draw. */
final class StubWorld implements Views.WorldView {

    final String zoneId = "frontier";
    final String zoneName = "Ashen Frontier";
    final int width = 34, height = 26;
    private final TileType[][] tiles = new TileType[height][width];

    private final List<Views.NpcView> npcs = new ArrayList<Views.NpcView>();
    private final List<Views.MobView> mobs = new ArrayList<Views.MobView>();
    private final List<Views.GatherView> nodes = new ArrayList<Views.GatherView>();
    private final List<Views.PortalView> portals = new ArrayList<Views.PortalView>();
    private final Views.FactionWarView war;

    StubWorld() {
        buildTiles();
        npcs.add(new Npc("npc_aldric", "Aldric", new GridPos(14, 13), true, false, "npc.giver"));
        npcs.add(new Npc("npc_vendor", "Quartermaster Vos", new GridPos(18, 11), false, true, "npc.vendor"));
        npcs.add(new Npc("npc_scout", "Scout Rell", new GridPos(12, 16), true, false, "npc.giver"));

        mobs.add(new Mob("mob_boar1", "Frontier Boar", new GridPos(22, 15), 34, 40, 3, false, "mob.beast.boar"));
        mobs.add(new Mob("mob_boar2", "Frontier Boar", new GridPos(24, 17), 40, 40, 3, false, "mob.beast.boar"));
        mobs.add(new Mob("mob_brute", "Ridge Brute", new GridPos(27, 9), 55, 90, 5, true, "mob.brute.ridge"));
        mobs.add(new Mob("mob_shade", "Wandering Shade", new GridPos(8, 20), 20, 60, 4, false, "mob.undead.shade"));

        nodes.add(new Node("node_copper1", "Copper Vein", new GridPos(20, 8), false));
        nodes.add(new Node("node_copper2", "Copper Vein", new GridPos(26, 19), true));
        nodes.add(new Node("node_herb1", "Frontier Herb", new GridPos(10, 9), false));

        portals.add(new Portal(new GridPos(2, 12), "start_haven", "To Haven"));
        portals.add(new Portal(new GridPos(31, 6), "dungeon_crypt", "Crypt Depths"));

        war = new War();
    }

    private void buildTiles() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                TileType t = TileType.GRASS;
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) t = TileType.WALL;
                else if (y >= 11 && y <= 13) t = TileType.ROAD;
                else if (x > 6 && x < 12 && y > 18 && y < 23) t = TileType.WATER;
                else if (x > 24 && y < 8) t = TileType.STONE;
                else if (((x * 7 + y * 3) % 11) == 0) t = TileType.DIRT;
                tiles[y][x] = t;
            }
    }

    // ---- WorldView ----
    public String zoneId() { return zoneId; }
    public String zoneName() { return zoneName; }
    public int width() { return width; }
    public int height() { return height; }
    public TileType tileAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return TileType.VOID;
        return tiles[y][x];
    }
    public List<Views.NpcView> npcs() { return npcs; }
    public List<Views.MobView> mobs() { return mobs; }
    public List<Views.GatherView> gatherNodes() { return nodes; }
    public List<Views.PortalView> portals() { return portals; }
    public Views.FactionWarView factionWar() { return war; }

    // ---- entity views ----
    private static final class Npc implements Views.NpcView {
        private final String id, name, sprite; private final GridPos pos; private final boolean giver, vendor;
        Npc(String id, String name, GridPos pos, boolean giver, boolean vendor, String sprite) {
            this.id = id; this.name = name; this.pos = pos; this.giver = giver; this.vendor = vendor; this.sprite = sprite; }
        public String id() { return id; } public String name() { return name; } public GridPos pos() { return pos; }
        public boolean questGiver() { return giver; } public boolean vendor() { return vendor; } public String spriteKey() { return sprite; }
    }
    private static final class Mob implements Views.MobView {
        private final String id, name, sprite; private final GridPos pos; private final int hp, maxHp, level; private final boolean combat;
        Mob(String id, String name, GridPos pos, int hp, int maxHp, int level, boolean combat, String sprite) {
            this.id = id; this.name = name; this.pos = pos; this.hp = hp; this.maxHp = maxHp; this.level = level; this.combat = combat; this.sprite = sprite; }
        public String id() { return id; } public String name() { return name; } public GridPos pos() { return pos; }
        public int hp() { return hp; } public int maxHp() { return maxHp; } public int level() { return level; }
        public boolean inCombat() { return combat; } public String spriteKey() { return sprite; }
    }
    private static final class Node implements Views.GatherView {
        private final String id, name; private final GridPos pos; private final boolean depleted;
        Node(String id, String name, GridPos pos, boolean depleted) { this.id = id; this.name = name; this.pos = pos; this.depleted = depleted; }
        public String id() { return id; } public String name() { return name; } public GridPos pos() { return pos; } public boolean depleted() { return depleted; }
    }
    private static final class Portal implements Views.PortalView {
        private final GridPos pos; private final String target, label;
        Portal(GridPos pos, String target, String label) { this.pos = pos; this.target = target; this.label = label; }
        public GridPos pos() { return pos; } public String targetZoneId() { return target; } public String label() { return label; }
    }
    private static final class War implements Views.FactionWarView {
        private final List<ObjectiveView> objs = Arrays.<ObjectiveView>asList(
                new Obj("North Tower", ControlState.ASHARR),
                new Obj("Ridge Keep", ControlState.CONTESTED),
                new Obj("South Bastion", ControlState.KUJIX));
        public List<ObjectiveView> objectives() { return objs; }
        public int asharrScore() { return 1420; }
        public int kujixScore() { return 1185; }
        private static final class Obj implements ObjectiveView {
            private final String name; private final ControlState c;
            Obj(String name, ControlState c) { this.name = name; this.c = c; }
            public String name() { return name; } public ControlState control() { return c; }
        }
    }
}

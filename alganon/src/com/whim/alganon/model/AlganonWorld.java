package com.whim.alganon.model;

import com.whim.alganon.api.Combatant;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.MobDef;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.data.ZoneBlueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Live, mutable loaded-zone model built from a {@link ZoneBlueprint}. Owns the tile
 * grid and the live NPC / mob / node / portal entities. The engine steps the player,
 * fights and removes mobs, and depletes/respawns nodes through this interface.
 */
public final class AlganonWorld implements WorldModel {

    private final ZoneBlueprint bp;
    private final List<NpcEntity> npcs = new ArrayList<NpcEntity>();
    private final List<MobEntity> mobs = new ArrayList<MobEntity>();
    private final List<NodeEntity> nodes = new ArrayList<NodeEntity>();
    private final List<Portal> portals = new ArrayList<Portal>();

    public AlganonWorld(ZoneBlueprint bp, Content content) {
        this.bp = bp;
        for (ZoneBlueprint.NpcSpec n : bp.npcs) {
            npcs.add(new NpcImpl(n.id, n.name, n.pos, n.questGiver, n.vendor, n.spriteKey));
        }
        for (ZoneBlueprint.MobSpec m : bp.mobs) {
            MobDef def = content.mob(m.mobDefId);
            if (def != null) mobs.add(new MobImpl(m.instanceId, def, m.pos));
        }
        for (ZoneBlueprint.NodeSpec nd : bp.nodes) {
            com.whim.alganon.api.Defs.GatherNodeDef gd = content.gatherNode(nd.nodeDefId);
            String name = gd != null ? gd.name : nd.nodeDefId;
            nodes.add(new NodeImpl(nd.nodeDefId, name, nd.pos));
        }
        for (ZoneBlueprint.PortalSpec p : bp.portals) {
            portals.add(new PortalImpl(p.pos, p.targetZoneId, p.label));
        }
    }

    /** The zone's default arrival tile (used when a portal has no explicit mate). */
    public GridPos spawn() { return bp.spawn != null ? bp.spawn : new GridPos(1, 1); }

    @Override public String zoneId() { return bp.meta.id; }
    @Override public String zoneName() { return bp.meta.name; }
    @Override public int width() { return bp.meta.width; }
    @Override public int height() { return bp.meta.height; }

    @Override public TileType tileAt(int x, int y) {
        if (x < 0 || y < 0 || y >= bp.tiles.length || x >= bp.tiles[y].length) return TileType.VOID;
        return bp.tiles[y][x];
    }

    @Override public boolean walkable(int x, int y) {
        TileType t = tileAt(x, y);
        return t != TileType.WALL && t != TileType.WATER && t != TileType.VOID;
    }

    @Override public List<NpcEntity> npcs() { return npcs; }
    @Override public List<MobEntity> mobs() { return mobs; }
    @Override public List<NodeEntity> nodes() { return nodes; }
    @Override public List<Portal> portals() { return portals; }

    // ------------------------------------------------------------- entities

    private static final class NpcImpl implements NpcEntity {
        private final String id, name, spriteKey;
        private final GridPos pos;
        private final boolean questGiver, vendor;
        NpcImpl(String id, String name, GridPos pos, boolean q, boolean v, String sprite) {
            this.id = id; this.name = name; this.pos = pos; this.questGiver = q; this.vendor = v; this.spriteKey = sprite;
        }
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public GridPos pos() { return pos; }
        @Override public boolean questGiver() { return questGiver; }
        @Override public boolean vendor() { return vendor; }
        @Override public String spriteKey() { return spriteKey; }
    }

    /** A live mob: a {@link Combatant} with a world position and coarse combat flag. */
    static final class MobImpl implements MobEntity {
        private final String instanceId;
        private final MobDef def;
        private GridPos pos;
        private int hp;
        private boolean inCombat;

        MobImpl(String instanceId, MobDef def, GridPos pos) {
            this.instanceId = instanceId; this.def = def; this.pos = pos; this.hp = def.maxHp;
        }

        @Override public String id() { return instanceId; }
        @Override public String defId() { return def.id; }
        @Override public GridPos pos() { return pos; }
        @Override public void setPos(GridPos p) { if (p != null) this.pos = p; }
        @Override public int level() { return def.level; }
        @Override public String spriteKey() { return def.spriteKey; }
        @Override public boolean inCombat() { return inCombat; }
        @Override public void setInCombat(boolean v) { this.inCombat = v; }

        @Override public String name() { return def.name; }
        @Override public boolean isPlayer() { return false; }
        @Override public int hp() { return hp; }
        @Override public int maxHp() { return def.maxHp; }
        @Override public boolean alive() { return hp > 0; }
        @Override public int attackPower() { return def.attackPower; }
        @Override public int defense() { return def.defense; }

        @Override public int takeDamage(int amount, DamageType type) {
            int dealt = Math.max(1, amount - def.defense / 2);
            hp = Math.max(0, hp - dealt);
            return dealt;
        }
        @Override public void heal(int amount) {
            if (amount <= 0) return;
            hp = Math.min(def.maxHp, hp + amount);
        }
    }

    private static final class NodeImpl implements NodeEntity {
        private final String id, name;
        private final GridPos pos;
        private boolean depleted;
        private double respawnRemaining;
        NodeImpl(String id, String name, GridPos pos) { this.id = id; this.name = name; this.pos = pos; }
        @Override public String id() { return id; }
        @Override public String name() { return name; }
        @Override public GridPos pos() { return pos; }
        @Override public boolean depleted() { return depleted; }
        @Override public void setDepleted(boolean v) { this.depleted = v; }
        @Override public double respawnRemaining() { return respawnRemaining; }
        @Override public void setRespawnRemaining(double sec) { this.respawnRemaining = Math.max(0, sec); }
    }

    private static final class PortalImpl implements Portal {
        private final GridPos pos;
        private final String targetZoneId, label;
        PortalImpl(GridPos pos, String target, String label) { this.pos = pos; this.targetZoneId = target; this.label = label; }
        @Override public GridPos pos() { return pos; }
        @Override public String targetZoneId() { return targetZoneId; }
        @Override public String label() { return label; }
    }
}

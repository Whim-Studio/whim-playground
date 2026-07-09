package com.whim.alganon.data;

import com.whim.alganon.api.Defs.ZoneMeta;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GridPos;

import java.util.ArrayList;
import java.util.List;

/**
 * A static layout template for one loadable zone: metadata, a tile grid, and the
 * placement specs for NPCs, mobs, gather nodes and portals. Owned by Task 1 (data
 * package). The model layer turns a blueprint into a live {@code WorldModel}; the
 * blueprint itself holds no mutable per-run state.
 *
 * <p>Tiles are stored row-major as {@code tiles[y][x]}.</p>
 */
public final class ZoneBlueprint {
    public final ZoneMeta meta;
    public final TileType[][] tiles;   // [y][x]
    /** Default arrival tile (character-creation spawn / fallback when a portal lacks a mate). */
    public GridPos spawn;
    public final List<NpcSpec> npcs = new ArrayList<NpcSpec>();
    public final List<MobSpec> mobs = new ArrayList<MobSpec>();
    public final List<NodeSpec> nodes = new ArrayList<NodeSpec>();
    public final List<PortalSpec> portals = new ArrayList<PortalSpec>();

    public ZoneBlueprint(ZoneMeta meta, TileType[][] tiles) {
        this.meta = meta;
        this.tiles = tiles;
    }

    public int width() { return meta.width; }
    public int height() { return meta.height; }

    /** Placed NPC. spriteKey drives procedural rendering; flags drive interactions. */
    public static final class NpcSpec {
        public final String id, name, spriteKey;
        public final GridPos pos;
        public final boolean questGiver, vendor;
        public NpcSpec(String id, String name, GridPos pos, boolean questGiver, boolean vendor, String spriteKey) {
            this.id = id; this.name = name; this.pos = pos;
            this.questGiver = questGiver; this.vendor = vendor; this.spriteKey = spriteKey;
        }
    }

    /** A mob spawn: unique instance id + the MobDef id to resolve stats/loot from Content. */
    public static final class MobSpec {
        public final String instanceId, mobDefId;
        public final GridPos pos;
        public MobSpec(String instanceId, String mobDefId, GridPos pos) {
            this.instanceId = instanceId; this.mobDefId = mobDefId; this.pos = pos;
        }
    }

    /** A gather node: the GatherNodeDef id (also used as the live node's id) + position. */
    public static final class NodeSpec {
        public final String nodeDefId;
        public final GridPos pos;
        public NodeSpec(String nodeDefId, GridPos pos) {
            this.nodeDefId = nodeDefId; this.pos = pos;
        }
    }

    /** A walk-on portal linking to another zone. */
    public static final class PortalSpec {
        public final GridPos pos;
        public final String targetZoneId, label;
        public PortalSpec(GridPos pos, String targetZoneId, String label) {
            this.pos = pos; this.targetZoneId = targetZoneId; this.label = label;
        }
    }
}

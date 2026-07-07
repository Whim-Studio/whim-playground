package com.whim.albion.world;

import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.TileType;
import com.whim.albion.api.GridPos;
import com.whim.albion.api.Views.NpcView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A uniform grid map: a {@link Tile} array plus overlaid NPCs, transitions,
 * interactables and encounter cells. Used for both {@code OUTDOOR_2D} towns and
 * {@code INDOOR_3D} dungeons; the {@link MapType} tells the UI how to render it.
 */
public final class TileMap {

    private static final Tile VOID_TILE = Tile.of(TileType.VOID);

    private final String id;
    private final String name;
    private final MapType type;
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    private final List<NpcImpl> npcs = new ArrayList<NpcImpl>();
    private final Map<GridPos, TransitionImpl> transitions = new HashMap<GridPos, TransitionImpl>();
    private final Map<GridPos, InteractableImpl> interactables = new HashMap<GridPos, InteractableImpl>();
    private final Map<GridPos, String> encounters = new HashMap<GridPos, String>();
    private final Set<GridPos> clearedEncounters = new HashSet<GridPos>();

    public TileMap(String id, String name, MapType type, int width, int height, TileType fill) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        Tile base = Tile.of(fill);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) tiles[y][x] = base;
        }
    }

    public String id() { return id; }
    public String name() { return name; }
    public MapType type() { return type; }
    public int width() { return width; }
    public int height() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public Tile tileAt(int x, int y) {
        return inBounds(x, y) ? tiles[y][x] : VOID_TILE;
    }

    // ------------------------------------------------------------- authoring

    public void set(int x, int y, Tile t) {
        if (inBounds(x, y)) tiles[y][x] = t;
    }

    public void set(int x, int y, TileType type) { set(x, y, Tile.of(type)); }

    /** Fill a rectangular border with the given tile type (walls around a room). */
    public void border(TileType type) {
        for (int x = 0; x < width; x++) { set(x, 0, type); set(x, height - 1, type); }
        for (int y = 0; y < height; y++) { set(0, y, type); set(width - 1, y, type); }
    }

    public void rect(int x0, int y0, int x1, int y1, TileType type) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) set(x, y, type);
        }
    }

    public void addNpc(NpcImpl npc) { npcs.add(npc); }

    public void addTransition(int x, int y, TransitionImpl t) {
        transitions.put(new GridPos(x, y), t);
    }

    public void addInteractable(int x, int y, InteractableImpl i) {
        interactables.put(new GridPos(x, y), i);
    }

    public void addEncounter(int x, int y, String encounterId) {
        encounters.put(new GridPos(x, y), encounterId);
    }

    // --------------------------------------------------------------- queries

    public List<NpcView> npcViews() {
        return new ArrayList<NpcView>(npcs);
    }

    public boolean npcAt(int x, int y) {
        for (NpcImpl n : npcs) {
            if (n.x() == x && n.y() == y) return true;
        }
        return false;
    }

    public TransitionImpl transitionAt(int x, int y) {
        return transitions.get(new GridPos(x, y));
    }

    public InteractableImpl interactableAt(int x, int y) {
        return interactables.get(new GridPos(x, y));
    }

    public String encounterAt(int x, int y) {
        GridPos p = new GridPos(x, y);
        if (clearedEncounters.contains(p)) return null;
        return encounters.get(p);
    }

    public void clearEncounter(int x, int y) {
        clearedEncounters.add(new GridPos(x, y));
    }
}

package com.whim.albion.world;

import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.WorldModel;
import com.whim.albion.api.Views.NpcView;
import com.whim.albion.api.Views.PlayerView;
import com.whim.albion.api.Views.TileView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Navigable world state: a registry of {@link TileMap}s plus the player's
 * position and facing on the current map. Drives movement, map transitions,
 * interactables and encounter triggers for the engine while serving the
 * read-only {@link com.whim.albion.api.Views.WorldView} the UI renders.
 */
public final class WorldModelImpl implements WorldModel, PlayerView {

    private final Map<String, TileMap> maps = new HashMap<String, TileMap>();
    private TileMap current;

    private int px;
    private int py;
    private Direction facing = Direction.SOUTH;

    public void registerMap(TileMap map) { maps.put(map.id(), map); }

    // ------------------------------------------------------------- WorldView

    @Override public String mapId() { return current == null ? "" : current.id(); }
    @Override public String mapName() { return current == null ? "" : current.name(); }
    @Override public MapType mapType() { return current == null ? MapType.OUTDOOR_2D : current.type(); }
    @Override public int width() { return current == null ? 0 : current.width(); }
    @Override public int height() { return current == null ? 0 : current.height(); }

    @Override public TileView tileAt(int x, int y) {
        return current == null ? null : current.tileAt(x, y);
    }

    @Override public PlayerView player() { return this; }

    @Override public List<NpcView> npcs() {
        return current == null ? java.util.Collections.<NpcView>emptyList() : current.npcViews();
    }

    // ------------------------------------------------------------ PlayerView

    @Override public int x() { return px; }
    @Override public int y() { return py; }
    @Override public Direction facing() { return facing; }

    // ------------------------------------------------------------ navigation

    @Override public boolean stepPlayer(Direction dir) {
        facing = dir;
        if (current == null) return false;
        int nx = px + dir.dx();
        int ny = py + dir.dy();
        if (!current.tileAt(nx, ny).walkable()) return false;
        if (current.npcAt(nx, ny)) return false;
        px = nx;
        py = ny;
        return true;
    }

    @Override public void turnPlayer(Direction newFacing) { this.facing = newFacing; }

    @Override public void loadMap(String mapId, int x, int y, Direction facing) {
        TileMap m = maps.get(mapId);
        if (m == null) return;
        this.current = m;
        this.px = x;
        this.py = y;
        this.facing = facing == null ? Direction.SOUTH : facing;
    }

    @Override public Transition transitionAt(int x, int y) {
        return current == null ? null : current.transitionAt(x, y);
    }

    @Override public Interactable interactableAt(int x, int y) {
        return current == null ? null : current.interactableAt(x, y);
    }

    @Override public String encounterAt(int x, int y) {
        return current == null ? null : current.encounterAt(x, y);
    }

    @Override public void clearEncounter(int x, int y) {
        if (current != null) current.clearEncounter(x, y);
    }

    /** The current map (used by the model bundle for save/debug); may be null. */
    public TileMap currentMap() { return current; }
}

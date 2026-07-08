package com.whim.necromunda.model.board;

import java.util.HashMap;
import java.util.Map;

import com.whim.necromunda.model.Fighter;

/**
 * The battlefield: a {@code width x height} grid of {@link Tile}s plus a mapping
 * of fighter placements keyed by {@link Position}. Pure data — no rendering and
 * no rules logic lives here.
 */
public final class Board {

    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private final Map<Position, Fighter> placements = new HashMap<Position, Fighter>();
    private final Map<Fighter, Position> reverse = new HashMap<Fighter, Position>();

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(TerrainType.OPEN, 0);
            }
        }
    }

    public int width() { return width; }
    public int height() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Tile tile(int x, int y) {
        return tiles[x][y];
    }

    public Tile tile(Position p) {
        return tiles[p.x()][p.y()];
    }

    /** Place (or move) a fighter onto a position, clearing any previous cell. */
    public void place(Fighter fighter, Position pos) {
        Position old = reverse.get(fighter);
        if (old != null) {
            placements.remove(old);
        }
        placements.put(pos, fighter);
        reverse.put(fighter, pos);
    }

    public void remove(Fighter fighter) {
        Position old = reverse.remove(fighter);
        if (old != null) {
            placements.remove(old);
        }
    }

    public Fighter fighterAt(Position pos) {
        return placements.get(pos);
    }

    public Position positionOf(Fighter fighter) {
        return reverse.get(fighter);
    }

    public Map<Position, Fighter> placements() {
        return placements;
    }
}

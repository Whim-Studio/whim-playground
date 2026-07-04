package com.rampart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A hardcoded level layout plus the factory that decodes it into live model
 * objects. Each level is an ASCII map (one {@code String} per row) using the
 * legend below; {@link #LEVELS} holds the built-in set (at least three).
 *
 * <p>Map legend:
 * <ul>
 *   <li>{@code ~} &rarr; {@link TileType#WATER}</li>
 *   <li>{@code .} &rarr; {@link TileType#LAND}</li>
 *   <li>{@code #} &rarr; {@link TileType#WALL}</li>
 *   <li>{@code C} &rarr; {@link TileType#CASTLE} (also records a castle spawn)</li>
 * </ul>
 *
 * <p>This class only decodes data into a {@link Grid}/{@link GameState}; it contains
 * no game rules. The engine (Task 2) mutates whatever this factory produces.
 */
public final class LevelData {

    /** Water cell marker. */
    public static final char WATER_CH = '~';
    /** Land cell marker. */
    public static final char LAND_CH = '.';
    /** Wall cell marker. */
    public static final char WALL_CH = '#';
    /** Castle cell marker. */
    public static final char CASTLE_CH = 'C';

    private final String name;
    private final String[] map;

    /**
     * Creates a level from a display name and its ASCII map.
     *
     * @param name a short human-readable name
     * @param map  the row strings (must be non-empty and rectangular)
     */
    public LevelData(String name, String[] map) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (map == null || map.length == 0) throw new IllegalArgumentException("map must be non-empty");
        int w = map[0].length();
        for (int r = 0; r < map.length; r++) {
            if (map[r] == null || map[r].length() != w) {
                throw new IllegalArgumentException(
                        "map row " + r + " has wrong width (expected " + w + ")");
            }
        }
        this.name = name;
        this.map = map.clone();
    }

    /** @return the level's display name */
    public String name() { return name; }

    /** @return the number of columns in this level's map */
    public int cols() { return map[0].length(); }

    /** @return the number of rows in this level's map */
    public int rows() { return map.length; }

    /** @return a defensive copy of the raw ASCII map rows */
    public String[] map() { return map.clone(); }

    /**
     * Decodes this level's ASCII map into a fresh {@link Grid}.
     *
     * @return a newly built grid populated from the map
     */
    public Grid buildGrid() {
        Grid grid = new Grid(cols(), rows());
        for (int r = 0; r < rows(); r++) {
            String line = map[r];
            for (int c = 0; c < cols(); c++) {
                grid.setType(c, r, decode(line.charAt(c)));
            }
        }
        return grid;
    }

    /**
     * @return the castle spawn coordinates declared by this level's {@code C} cells,
     *         in row-major order
     */
    public List<Coord> castleSpawns() {
        List<Coord> spawns = new ArrayList<Coord>();
        for (int r = 0; r < rows(); r++) {
            String line = map[r];
            for (int c = 0; c < cols(); c++) {
                if (line.charAt(c) == CASTLE_CH) spawns.add(new Coord(c, r));
            }
        }
        return spawns;
    }

    /** Maps a map character to its {@link TileType}. */
    private static TileType decode(char ch) {
        switch (ch) {
            case WATER_CH:  return TileType.WATER;
            case LAND_CH:   return TileType.LAND;
            case WALL_CH:   return TileType.WALL;
            case CASTLE_CH: return TileType.CASTLE;
            default:
                throw new IllegalArgumentException("unknown map char: '" + ch + "'");
        }
    }

    // ---- Factory: build a fresh game from a level ----

    /**
     * Builds a fresh {@link GameState} for round 1 from this level: decodes the
     * grid, spawns the castles, and seeds phase/round/lives/cannon-pool from
     * {@link Rules}. No timers or ships are started — that is the engine's job.
     *
     * @return a ready-to-start game state at {@link Phase#TITLE}
     */
    public GameState newGameState() {
        Grid grid = buildGrid();
        GameState state = new GameState(grid);
        List<Coord> spawns = castleSpawns();
        for (int i = 0; i < spawns.size(); i++) {
            state.castleList().add(new Castle(spawns.get(i)));
        }
        state.setPhase(Phase.TITLE);
        state.setRound(1);
        state.setLives(Math.max(1, spawns.size()));
        state.setCannonsRemainingToPlace(Rules.cannonPoolForRound(1));
        state.setTimerRemainingMillis(0L);
        return state;
    }

    /**
     * Convenience factory building a fresh game from a level index into
     * {@link #LEVELS}.
     *
     * @param index the level index
     * @return a fresh game state for that level
     */
    public static GameState newGameState(int index) {
        return LEVELS.get(index).newGameState();
    }

    // ---- Built-in levels (30 x 22, matching Rules.GRID_COLS x GRID_ROWS) ----

    private static final String W30 = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";

    /** Level 1 — a small island with one castle already ringed by a starting wall. */
    private static final String[] MAP_1 = {
        W30,
        W30,
        W30,
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~......########......~~~~~",
        "~~~~~......#......#......~~~~~",
        "~~~~~......#..C...#......~~~~~",
        "~~~~~......#......#......~~~~~",
        "~~~~~......#......#......~~~~~",
        "~~~~~......########......~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        "~~~~~....................~~~~~",
        W30,
        W30,
        W30,
    };

    /** Level 2 — a wider island with two open castles for the player to wall in. */
    private static final String[] MAP_2 = {
        W30,
        W30,
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......C...............~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~...............C......~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        "~~~~......................~~~~",
        W30,
        W30,
    };

    /** Level 3 — a large landmass with three castles spread across the coast. */
    private static final String[] MAP_3 = {
        W30,
        W30,
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~.....C..................~~~",
        "~~~........................~~~",
        "~~~...................C....~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~............C...........~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        "~~~........................~~~",
        W30,
        W30,
    };

    /**
     * The built-in level set, in play order. Unmodifiable; at least three levels as
     * required by the build contract.
     */
    public static final List<LevelData> LEVELS;
    static {
        List<LevelData> levels = new ArrayList<LevelData>();
        levels.add(new LevelData("Lonely Keep", MAP_1));
        levels.add(new LevelData("Twin Shores", MAP_2));
        levels.add(new LevelData("Three Crowns", MAP_3));
        LEVELS = Collections.unmodifiableList(levels);
    }
}

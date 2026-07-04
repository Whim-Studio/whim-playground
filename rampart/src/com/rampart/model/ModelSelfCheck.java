package com.rampart.model;

import java.util.List;

/**
 * Standalone smoke test for the model layer — no engine or UI required. Builds each
 * built-in level, prints grid dimensions and castle counts, and runs a rotation
 * demo of a {@link WallPiece} to prove the pure data types instantiate and behave.
 * Run: {@code java -cp out com.rampart.model.ModelSelfCheck}.
 */
public final class ModelSelfCheck {
    private ModelSelfCheck() {}

    public static void main(String[] args) {
        System.out.println("=== Rampart model self-check ===");
        System.out.println("grid constant   : " + Rules.GRID_COLS + " x " + Rules.GRID_ROWS);
        System.out.println("phase millis    : BUILD=" + Rules.BUILD_PHASE_MILLIS
                + " BATTLE=" + Rules.BATTLE_PHASE_MILLIS
                + " REPAIR=" + Rules.REPAIR_PHASE_MILLIS);
        System.out.println("cannon pool r1  : " + Rules.cannonPoolForRound(1)
                + " (max " + Rules.CANNON_MAX + ")");
        System.out.println("survive rule    : >= " + Rules.MIN_ENCLOSED_CASTLES_TO_SURVIVE
                + " enclosed castle(s)");
        System.out.println();

        require(LevelData.LEVELS.size() >= 3, "at least 3 built-in levels");

        for (int i = 0; i < LevelData.LEVELS.size(); i++) {
            LevelData lvl = LevelData.LEVELS.get(i);
            GameState state = lvl.newGameState();
            Grid grid = state.gridModel();

            int walls = countType(grid, TileType.WALL);
            int land = countType(grid, TileType.LAND);
            int water = countType(grid, TileType.WATER);

            System.out.println("Level " + (i + 1) + ": \"" + lvl.name() + "\"");
            System.out.println("  grid         : " + grid.cols() + " x " + grid.rows());
            System.out.println("  castles      : " + state.castles().size()
                    + " at " + spawnsToString(lvl.castleSpawns()));
            System.out.println("  tiles        : land=" + land + " water=" + water + " wall=" + walls);
            System.out.println("  lives        : " + state.lives()
                    + "  cannonsToPlace=" + state.cannonsRemainingToPlace()
                    + "  phase=" + state.phase());

            require(grid.cols() == Rules.GRID_COLS, "level " + (i + 1) + " width == Rules.GRID_COLS");
            require(grid.rows() == Rules.GRID_ROWS, "level " + (i + 1) + " height == Rules.GRID_ROWS");
            require(state.castles().size() >= 1, "level " + (i + 1) + " has a castle");
            require(land > 0, "level " + (i + 1) + " has buildable land");
            // Every castle spawn cell decodes to a CASTLE tile.
            List<Coord> spawns = lvl.castleSpawns();
            for (int s = 0; s < spawns.size(); s++) {
                Coord cc = spawns.get(s);
                require(grid.typeAt(cc.col(), cc.row()) == TileType.CASTLE,
                        "castle cell " + cc + " is CASTLE tile");
            }
            System.out.println();
        }

        // --- WallPiece rotation demo ---
        System.out.println("WallPiece rotation demo (L-piece anchored at (10,5)):");
        WallPiece piece = new WallPiece(WallShape.L, new Coord(10, 5));
        for (int r = 0; r < Rules.PIECE_ROTATIONS; r++) {
            System.out.println("  rot " + piece.rotation() + " cells=" + piece.absoluteCells());
            require(piece.size() == 4, "L-piece has 4 cells");
            piece.rotate();
        }
        require(piece.rotation() == 0, "rotation wraps back to 0 after 4 turns");

        // Confirm every shape builds four rotation states of the right cell count.
        int[] expectedSizes = { 1, 4, 4, 4, 4, 4, 4, 4 };
        WallShape[] shapes = WallShape.values();
        for (int i = 0; i < shapes.length; i++) {
            WallPiece p = new WallPiece(shapes[i], new Coord(0, 0));
            require(p.size() == expectedSizes[i], shapes[i] + " has " + expectedSizes[i] + " cells");
            require(p.offsets().size() == p.size(), shapes[i] + " offsets match size");
        }
        System.out.println("  all " + shapes.length + " shapes built 4 rotation states");

        // Cannon / Ship state sanity.
        Cannon cannon = new Cannon(new Coord(14, 10));
        require(cannon.ready(), "fresh cannon is ready");
        cannon.setReloadRemainingMillis(Rules.CANNON_RELOAD_MILLIS);
        require(!cannon.ready(), "reloading cannon not ready");
        cannon.decReload(Rules.CANNON_RELOAD_MILLIS);
        require(cannon.ready(), "cannon ready after reload elapses");

        Ship ship = new Ship(ShipType.FRIGATE, 2.5, 3.0);
        require(ship.health() == ShipType.FRIGATE.baseHealth(), "ship starts at base health");
        boolean sunk = ship.damage(ShipType.FRIGATE.baseHealth());
        require(sunk && !ship.alive(), "ship sinks when health depleted");

        System.out.println("\nALL INVARIANTS PASSED");
    }

    private static int countType(Grid grid, TileType type) {
        int n = 0;
        for (int c = 0; c < grid.cols(); c++) {
            for (int r = 0; r < grid.rows(); r++) {
                if (grid.typeAt(c, r) == type) n++;
            }
        }
        return n;
    }

    private static String spawnsToString(List<Coord> spawns) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < spawns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(spawns.get(i));
        }
        return sb.append("]").toString();
    }

    private static void require(boolean cond, String what) {
        if (!cond) throw new AssertionError("FAILED invariant: " + what);
        System.out.println("  ok: " + what);
    }
}

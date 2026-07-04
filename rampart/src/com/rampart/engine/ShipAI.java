package com.rampart.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.rampart.model.Castle;
import com.rampart.model.Coord;
import com.rampart.model.Direction;
import com.rampart.model.GameState;
import com.rampart.model.Grid;
import com.rampart.model.Rules;
import com.rampart.model.Ship;
import com.rampart.model.ShipType;
import com.rampart.model.TileType;

/**
 * Drives the enemy fleet during the BATTLE phase. Consumes and mutates a
 * {@link GameState}: {@link #spawnWave} repopulates {@link GameState#shipList()} with
 * {@link Ship}s of {@link ShipType}s scaled by round; {@link #tick} steers each
 * {@link Ship}'s sub-cell position toward the coast and, on its firing cadence,
 * blasts the nearest {@link com.rampart.model.Grid} {@link TileType#WALL} into
 * {@link TileType#RUBBLE}.
 */
public final class ShipAI {

    /** Distance (in cells) at which a ship stops closing and begins bombarding. */
    private static final double FIRE_RANGE = 5.0;
    /** Cooldown between a ship's wall bombardments, in milliseconds. */
    private static final long FIRE_COOLDOWN_MILLIS = 1_200L;
    /** Fractional per-round movement-speed increase applied to each ship. */
    private static final double ROUND_SPEED_SCALE = 0.10;

    /**
     * Spawns the wave for the given round: {@link Rules#shipsForRound(int)} ships,
     * placed on open water and each assigned a target castle. Replaces the entire
     * {@link GameState#shipList()}.
     *
     * @param state the live {@link GameState}
     * @param round the 1-based round number
     * @param rng   the deterministic {@link Random} used for placement/type jitter
     */
    public void spawnWave(GameState state, int round, Random rng) {
        state.shipList().clear();
        Grid grid = state.gridModel();
        int count = Rules.shipsForRound(round);
        List<Castle> castles = state.castleList();

        for (int i = 0; i < count; i++) {
            ShipType type = typeFor(round, i);
            double[] spot = waterSpawn(grid, rng, i);
            Ship ship = new Ship(type, spot[0], spot[1]);
            Castle target = castles.isEmpty() ? null : castles.get(i % castles.size());
            if (target != null) ship.addWaypoint(target.position());
            state.shipList().add(ship);
        }
    }

    /**
     * Advances the fleet by {@code dtMillis}: moves each living {@link Ship} toward
     * its target castle at its (round-scaled) speed until within {@link #FIRE_RANGE}
     * of a wall, then bombards the nearest {@link TileType#WALL} into
     * {@link TileType#RUBBLE} on cooldown. Dead ships are pruned from
     * {@link GameState#shipList()}.
     *
     * @param state    the live {@link GameState}
     * @param dtMillis elapsed milliseconds since the previous tick
     */
    public void tick(GameState state, long dtMillis) {
        Grid grid = state.gridModel();
        double round = state.round();
        double speedScale = 1.0 + ROUND_SPEED_SCALE * (round - 1.0);

        for (int i = 0; i < state.shipList().size(); i++) {
            Ship ship = state.shipList().get(i);
            if (!ship.alive()) continue;
            ship.decFireCooldown(dtMillis);

            int[] nearestWall = nearestWall(grid, ship);
            double wallDist = nearestWall == null ? Double.MAX_VALUE
                    : dist(ship.x(), ship.y(), nearestWall[0], nearestWall[1]);

            if (wallDist <= FIRE_RANGE) {
                if (ship.fireCooldownMillis() <= 0L && nearestWall != null) {
                    grid.setType(nearestWall[0], nearestWall[1], TileType.RUBBLE);
                    ship.setFireCooldownMillis(FIRE_COOLDOWN_MILLIS);
                }
            } else {
                moveToward(ship, targetCell(ship), speedScale * ship.type().baseSpeed(), dtMillis);
            }
        }
        pruneDead(state);
    }

    /** Chooses this ship's steering target: its first waypoint, or its own cell. */
    private Coord targetCell(Ship ship) {
        List<Coord> path = ship.path();
        if (!path.isEmpty()) return path.get(0);
        return new Coord((int) Math.round(ship.x()), (int) Math.round(ship.y()));
    }

    /** Steps a ship toward a target cell at {@code speed} cells/sec, setting heading. */
    private void moveToward(Ship ship, Coord target, double speed, long dtMillis) {
        double dx = target.col() - ship.x();
        double dy = target.row() - ship.y();
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return;
        double step = speed * (dtMillis / 1000.0);
        if (step > len) step = len;
        double nx = ship.x() + dx / len * step;
        double ny = ship.y() + dy / len * step;
        ship.setPosition(nx, ny);
        ship.setHeading(headingFor(dx, dy));
    }

    /** Classifies a movement vector into the nearest cardinal {@link Direction}. */
    private Direction headingFor(double dx, double dy) {
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dy >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Finds the grid cell of the wall nearest a ship, or {@code null} if none. */
    private int[] nearestWall(Grid grid, Ship ship) {
        int sc = (int) Math.round(ship.x());
        int sr = (int) Math.round(ship.y());
        int[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (int c = 0; c < grid.cols(); c++) {
            for (int r = 0; r < grid.rows(); r++) {
                if (grid.typeAt(c, r) != TileType.WALL) continue;
                double d = dist(sc, sr, c, r);
                if (d < bestDist) { bestDist = d; best = new int[] { c, r }; }
            }
        }
        return best;
    }

    /** Euclidean distance between two cell centres. */
    private static double dist(double ax, double ay, double bx, double by) {
        double dx = ax - bx;
        double dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Removes sunk ships from the live list. */
    private void pruneDead(GameState state) {
        List<Ship> ships = state.shipList();
        for (int i = ships.size() - 1; i >= 0; i--) {
            if (!ships.get(i).alive()) ships.remove(i);
        }
    }

    /** Round-scaled ship class: tougher classes appear in later rounds. */
    private ShipType typeFor(int round, int index) {
        if (round >= 3 && index % 3 == 0) return ShipType.GALLEON;
        if (round >= 2 && index % 2 == 0) return ShipType.FRIGATE;
        return ShipType.SLOOP;
    }

    /** Picks a spawn point in open water along the map's left or right margin. */
    private double[] waterSpawn(Grid grid, Random rng, int index) {
        boolean left = (index % 2 == 0);
        int col = left ? 1 : grid.cols() - 2;
        // Search a water cell in that column near a jittered row.
        int startRow = 1 + rng.nextInt(Math.max(1, grid.rows() - 2));
        for (int off = 0; off < grid.rows(); off++) {
            int r = (startRow + off) % grid.rows();
            if (grid.typeAt(col, r) == TileType.WATER) {
                return new double[] { col, r };
            }
        }
        return new double[] { col, startRow };
    }
}

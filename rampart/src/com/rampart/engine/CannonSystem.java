package com.rampart.engine;

import com.rampart.model.Cannon;
import com.rampart.model.Coord;
import com.rampart.model.GameState;
import com.rampart.model.Grid;
import com.rampart.model.Phase;
import com.rampart.model.Rules;
import com.rampart.model.Ship;
import com.rampart.model.TileType;

/**
 * Validates cannon placement and resolves cannon fire. Consumes and mutates a
 * {@link GameState}: it reads/writes the {@link Grid} (LAND &rarr; CANNON on
 * placement, WALL &rarr; RUBBLE on blast), the {@link Cannon} list (placement,
 * reload counters via {@link Cannon#decReload(long)}), and the {@link Ship} list
 * (blast damage). Firing scores ship kills through the supplied {@link ScoreSystem}.
 */
public final class CannonSystem {

    /** Hit points a single cannon blast removes from each ship it catches. */
    private static final int BLAST_DAMAGE = 2;

    /**
     * Attempts to place a cannon during the BUILD phase.
     *
     * <p>Legal only when: the phase is {@link Phase#BUILD}; cannons remain in the
     * pool ({@link GameState#cannonsRemainingToPlace()} &gt; 0); the target cell is
     * in bounds; and the cell is empty {@link TileType#LAND} flagged enclosed by the
     * {@link TerritoryCalculator}. On success it flips the {@link Grid} cell to
     * {@link TileType#CANNON}, appends a {@link Cannon} to
     * {@link GameState#cannonList()}, and decrements the remaining pool.</p>
     *
     * @param state the live {@link GameState}
     * @param col   target column
     * @param row   target row
     * @return {@code true} if a {@link Cannon} was placed
     */
    public boolean placeCannon(GameState state, int col, int row) {
        if (state.phase() != Phase.BUILD) return false;
        if (state.cannonsRemainingToPlace() <= 0) return false;
        Grid grid = state.gridModel();
        if (!grid.inBounds(col, row)) return false;
        // Footprint is Rules.CANNON_FOOTPRINT (1) cell: it must be empty enclosed land.
        if (grid.typeAt(col, row) != TileType.LAND) return false;
        if (!grid.tile(col, row).enclosed()) return false;

        grid.setType(col, row, TileType.CANNON);
        state.cannonList().add(new Cannon(new Coord(col, row)));
        state.setCannonsRemainingToPlace(state.cannonsRemainingToPlace() - 1);
        return true;
    }

    /**
     * Fires one ready cannon at a target cell during the BATTLE phase, resolving a
     * lobbed shot's blast of radius {@link Rules#CANNON_BLAST_RADIUS}.
     *
     * <p>Picks the first {@link Cannon#ready()} cannon, puts it on cooldown
     * ({@link Rules#CANNON_RELOAD_MILLIS}), and within the blast footprint damages
     * every {@link Ship} standing on a struck cell (scoring kills via
     * {@code score}) and turns each struck {@link TileType#WALL} into
     * {@link TileType#RUBBLE}. Recomputes territory afterward since walls may have
     * fallen.</p>
     *
     * @param state the live {@link GameState}
     * @param col   target column
     * @param row   target row
     * @param score the {@link ScoreSystem} credited for any ship kills
     * @return {@code true} if a loaded cannon fired
     */
    public boolean fireCannonAt(GameState state, int col, int row, ScoreSystem score) {
        if (state.phase() != Phase.BATTLE) return false;
        Grid grid = state.gridModel();
        if (!grid.inBounds(col, row)) return false;

        Cannon shooter = null;
        for (int i = 0; i < state.cannonList().size(); i++) {
            Cannon c = state.cannonList().get(i);
            if (c.ready()) { shooter = c; break; }
        }
        if (shooter == null) return false;

        shooter.setReloadRemainingMillis(Rules.CANNON_RELOAD_MILLIS);
        if (!shooter.unlimitedAmmo()) shooter.setAmmo(shooter.ammo() - 1);

        int r = Rules.CANNON_BLAST_RADIUS;
        for (int dc = -r; dc <= r; dc++) {
            for (int dr = -r; dr <= r; dr++) {
                int bc = col + dc;
                int br = row + dr;
                if (!grid.inBounds(bc, br)) continue;
                applyBlast(state, grid, bc, br, score);
            }
        }
        TerritoryCalculator.recompute(state);
        return true;
    }

    /** Applies a single blast cell: damages ships on it and rubbles a struck wall. */
    private void applyBlast(GameState state, Grid grid, int col, int row, ScoreSystem score) {
        for (int i = 0; i < state.shipList().size(); i++) {
            Ship s = state.shipList().get(i);
            if (!s.alive()) continue;
            if ((int) Math.round(s.x()) == col && (int) Math.round(s.y()) == row) {
                boolean sank = s.damage(BLAST_DAMAGE);
                if (sank) score.onShipSank(state, s);
            }
        }
        if (grid.typeAt(col, row) == TileType.WALL) {
            grid.setType(col, row, TileType.RUBBLE);
        }
    }

    /**
     * Decrements every cannon's reload timer by the elapsed time (called each BATTLE
     * tick). Mutates {@link Cannon#decReload(long)} on {@link GameState#cannonList()}.
     *
     * @param state    the live {@link GameState}
     * @param dtMillis elapsed milliseconds since the previous tick
     */
    public void tickReload(GameState state, long dtMillis) {
        for (int i = 0; i < state.cannonList().size(); i++) {
            state.cannonList().get(i).decReload(dtMillis);
        }
    }
}

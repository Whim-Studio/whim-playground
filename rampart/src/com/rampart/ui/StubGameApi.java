package com.rampart.ui;

import com.rampart.engine.GameApi;
import com.rampart.model.Cannon;
import com.rampart.model.Coord;
import com.rampart.model.GameState;
import com.rampart.model.GameStateView;
import com.rampart.model.Grid;
import com.rampart.model.LevelData;
import com.rampart.model.Phase;
import com.rampart.model.Rules;
import com.rampart.model.Ship;
import com.rampart.model.ShipType;
import com.rampart.model.TileType;
import com.rampart.model.WallPiece;
import com.rampart.model.WallPieceView;
import com.rampart.model.WallShape;

import java.util.List;

/**
 * A lightweight, dev-only fake {@link GameApi} so the Swing UI (title screen, the
 * playfield canvas, HUD, ghost preview, and the CardLayout screen transitions) can
 * be seen and driven before the real {@code com.rampart.engine.GameEngine} lands.
 *
 * <p>This is NOT the game engine and deliberately fakes just enough: it cycles the
 * three-phase loop on the phase timer, drifts a few ships during BATTLE, deals a
 * simple wall-piece sequence during REPAIR, and treats player input as simple grid
 * mutations. It intentionally does no real territory flood-fill, collision, or
 * scoring — that all belongs to Task 2's engine. The production {@code app.Main}
 * wires the real engine in this class's place.
 */
public final class StubGameApi implements GameApi {

    private GameState state;
    private int levelIndex;
    private int shapeCursor;

    private static final WallShape[] DEAL_ORDER = {
        WallShape.I, WallShape.L, WallShape.T, WallShape.O,
        WallShape.S, WallShape.Z, WallShape.J, WallShape.DOT
    };

    public StubGameApi() {
        newGame();
    }

    // ---- Lifecycle -----------------------------------------------------------

    @Override
    public void newGame() {
        levelIndex = 0;
        shapeCursor = 0;
        state = LevelData.LEVELS.get(levelIndex).newGameState();
        state.setPhase(Phase.TITLE);
        state.setTimerRemainingMillis(0L);
    }

    @Override
    public void startRound() {
        enterBuild();
    }

    @Override
    public GameStateView state() {
        return state;
    }

    @Override
    public boolean isGameOver() {
        return state.gameOver();
    }

    // ---- Tick / phase machine ------------------------------------------------

    @Override
    public void tick(long dtMillis) {
        Phase phase = state.phase();
        if (phase == Phase.TITLE || phase == Phase.GAME_OVER) {
            return;
        }

        // Advance per-phase simulation.
        if (phase == Phase.BATTLE) {
            driftShips(dtMillis);
            reloadCannons(dtMillis);
        }

        long remaining = state.timerRemainingMillis() - Math.max(0L, dtMillis);
        if (remaining > 0L) {
            state.setTimerRemainingMillis(remaining);
            return;
        }
        state.setTimerRemainingMillis(0L);
        advancePhase();
    }

    private void advancePhase() {
        switch (state.phase()) {
            case BUILD:
                enterBattle();
                break;
            case BATTLE:
                enterRepair();
                break;
            case REPAIR:
                enterRoundTransition();
                break;
            case ROUND_TRANSITION:
                enterBuild(); // next round
                break;
            default:
                break;
        }
    }

    private void enterBuild() {
        state.setPhase(Phase.BUILD);
        state.setTimerRemainingMillis(Rules.BUILD_PHASE_MILLIS);
        state.setCannonsRemainingToPlace(Rules.cannonPoolForRound(state.round()));
        state.setCurrentPiece(null);
        state.shipList().clear();
    }

    private void enterBattle() {
        state.setPhase(Phase.BATTLE);
        state.setTimerRemainingMillis(Rules.BATTLE_PHASE_MILLIS);
        spawnShips();
        // Cosmetic HUD reading so territory isn't always 0 in the stub.
        state.setTerritoryFraction(Math.min(1.0, 0.30 + 0.05 * (state.round() - 1)));
    }

    private void enterRepair() {
        state.setPhase(Phase.REPAIR);
        state.setTimerRemainingMillis(Rules.REPAIR_PHASE_MILLIS);
        state.shipList().clear();
        dealPieces();
    }

    private void enterRoundTransition() {
        state.setPhase(Phase.ROUND_TRANSITION);
        state.setTimerRemainingMillis(Rules.ROUND_TRANSITION_MILLIS);
        state.setCurrentPiece(null);
        state.queuedPieceList().clear();
        state.addScore(Rules.SCORE_ROUND_SURVIVAL_BONUS);
        state.setRound(state.round() + 1);
    }

    // ---- BATTLE-phase fake simulation ---------------------------------------

    private void spawnShips() {
        state.shipList().clear();
        int n = Rules.shipsForRound(state.round());
        ShipType[] types = ShipType.values();
        int rows = state.grid().rows();
        for (int i = 0; i < n; i++) {
            ShipType type = types[i % types.length];
            double y = 3 + (i * (rows - 6.0) / Math.max(1, n));
            // Alternate spawns between the left and right water margins.
            boolean fromLeft = (i % 2 == 0);
            double x = fromLeft ? 1.0 : state.grid().cols() - 2.0;
            Ship ship = new Ship(type, x, y);
            ship.setHeading(fromLeft ? com.rampart.model.Direction.EAST
                                     : com.rampart.model.Direction.WEST);
            state.shipList().add(ship);
        }
    }

    private void driftShips(long dtMillis) {
        int cols = state.grid().cols();
        List<Ship> ships = state.shipList();
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            if (!s.alive()) continue;
            double dir = (s.heading() == com.rampart.model.Direction.WEST) ? -1.0 : 1.0;
            double dx = s.type().baseSpeed() * (dtMillis / 1000.0) * dir;
            double nx = s.x() + dx;
            // Bounce within the water margins so ships stay visible.
            if (nx < 1.0) {
                nx = 1.0;
                s.setHeading(com.rampart.model.Direction.EAST);
            } else if (nx > cols - 2.0) {
                nx = cols - 2.0;
                s.setHeading(com.rampart.model.Direction.WEST);
            }
            s.setX(nx);
        }
    }

    private void reloadCannons(long dtMillis) {
        List<Cannon> cannons = state.cannonList();
        for (int i = 0; i < cannons.size(); i++) {
            cannons.get(i).decReload(dtMillis);
        }
    }

    // ---- REPAIR-phase pieces -------------------------------------------------

    private void dealPieces() {
        state.queuedPieceList().clear();
        Coord anchor = new Coord(state.grid().cols() / 2, state.grid().rows() / 2);
        state.setCurrentPiece(new WallPiece(nextShape(), anchor));
        for (int i = 1; i < Rules.REPAIR_QUEUE_SIZE; i++) {
            state.queuedPieceList().add(new WallPiece(nextShape(), anchor));
        }
    }

    private WallShape nextShape() {
        WallShape s = DEAL_ORDER[shapeCursor % DEAL_ORDER.length];
        shapeCursor++;
        return s;
    }

    // ---- Input (simple mutations, not real rules) ---------------------------

    @Override
    public boolean placeCannon(int col, int row) {
        if (state.phase() != Phase.BUILD) return false;
        if (state.cannonsRemainingToPlace() <= 0) return false;
        Grid grid = state.gridModel();
        if (!grid.inBounds(col, row)) return false;
        if (grid.typeAt(col, row) != TileType.LAND) return false;
        grid.setType(col, row, TileType.CANNON);
        state.cannonList().add(new Cannon(new Coord(col, row)));
        state.setCannonsRemainingToPlace(state.cannonsRemainingToPlace() - 1);
        return true;
    }

    @Override
    public boolean fireCannonAt(int col, int row) {
        if (state.phase() != Phase.BATTLE) return false;
        Cannon ready = null;
        List<Cannon> cannons = state.cannonList();
        for (int i = 0; i < cannons.size(); i++) {
            if (cannons.get(i).ready()) { ready = cannons.get(i); break; }
        }
        if (ready == null) return false;
        ready.setReloadRemainingMillis(Rules.CANNON_RELOAD_MILLIS);
        // Damage the nearest ship to the target cell (fake blast).
        Ship nearest = null;
        double best = Double.MAX_VALUE;
        List<Ship> ships = state.shipList();
        for (int i = 0; i < ships.size(); i++) {
            Ship s = ships.get(i);
            if (!s.alive()) continue;
            double d = Math.hypot(s.x() - col, s.y() - row);
            if (d < best) { best = d; nearest = s; }
        }
        if (nearest != null && best <= Rules.CANNON_BLAST_RADIUS + 1.5) {
            if (nearest.damage(1)) {
                state.addScore(nearest.type().scoreValue());
            }
        }
        return true;
    }

    @Override
    public void rotatePiece() {
        WallPiece p = state.currentPieceModel();
        if (p != null) p.rotate();
    }

    @Override
    public boolean placePieceAt(int col, int row) {
        if (state.phase() != Phase.REPAIR) return false;
        WallPiece p = state.currentPieceModel();
        if (p == null) return false;
        p.setAnchor(new Coord(col, row));
        Grid grid = state.gridModel();
        List<Coord> cells = p.absoluteCells();
        // Fake placement: only accept if every cell is buildable terrain.
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            if (!grid.inBounds(c.col(), c.row())) return false;
            TileType t = grid.typeAt(c.col(), c.row());
            if (t != TileType.LAND && t != TileType.RUBBLE) return false;
        }
        for (int i = 0; i < cells.size(); i++) {
            Coord c = cells.get(i);
            grid.setType(c.col(), c.row(), TileType.WALL);
        }
        advancePieceQueue();
        return true;
    }

    private void advancePieceQueue() {
        List<WallPiece> queue = state.queuedPieceList();
        if (queue.isEmpty()) {
            Coord anchor = new Coord(state.grid().cols() / 2, state.grid().rows() / 2);
            state.setCurrentPiece(new WallPiece(nextShape(), anchor));
            return;
        }
        state.setCurrentPiece(queue.remove(0));
        queue.add(new WallPiece(nextShape(),
                new Coord(state.grid().cols() / 2, state.grid().rows() / 2)));
    }

    @Override
    public WallPieceView currentPiece() {
        return state.currentPieceModel();
    }

    @Override
    public void endPhaseEarly() {
        Phase phase = state.phase();
        if (phase == Phase.TITLE || phase == Phase.GAME_OVER) return;
        state.setTimerRemainingMillis(0L);
    }
}

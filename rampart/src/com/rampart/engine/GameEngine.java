package com.rampart.engine;

import java.util.Random;

import com.rampart.model.Cannon;
import com.rampart.model.GameState;
import com.rampart.model.GameStateView;
import com.rampart.model.LevelData;
import com.rampart.model.Phase;
import com.rampart.model.Rules;
import com.rampart.model.WallPieceView;

/**
 * The concrete {@link GameApi} implementation and the single owner of the live
 * {@link GameState}. It wires together the {@link PhaseController},
 * {@link TerritoryCalculator}, {@link CannonSystem}, {@link WallBuildSystem},
 * {@link ShipAI} and {@link ScoreSystem}, delegating each responsibility to them and
 * exposing the live {@link GameState} (which implements {@link GameStateView}) to the
 * UI through {@link #state()}.
 *
 * <p>All public {@link GameApi} methods consume primitive/UI input and return
 * {@code com.rampart.model} read-only view types ({@link GameStateView},
 * {@link WallPieceView}); the engine holds zero Swing/AWT references.</p>
 */
public final class GameEngine implements GameApi {

    /** Default deterministic seed so headless runs are reproducible. */
    private static final long DEFAULT_SEED = 0x52414D50415254L; // "RAMPART" in ASCII hex

    private final Random rng;
    private final PhaseController phases;
    private final CannonSystem cannons;
    private final WallBuildSystem walls;
    private final ShipAI shipAi;
    private final ScoreSystem score;

    /** The level this game is played on (rounds escalate on the same map). */
    private int levelIndex;
    /** The live game state; replaced wholesale by {@link #newGame()}. */
    private GameState state;

    /** Creates an engine with the default deterministic seed and a fresh game. */
    public GameEngine() {
        this(DEFAULT_SEED);
    }

    /**
     * Creates an engine with an explicit RNG seed (for reproducible headless runs)
     * and starts a fresh game at {@link Phase#TITLE}.
     *
     * @param seed the seed for the deterministic {@link Random}
     */
    public GameEngine(long seed) {
        this.rng = new Random(seed);
        this.phases = new PhaseController(this);
        this.cannons = new CannonSystem();
        this.walls = new WallBuildSystem();
        this.shipAi = new ShipAI();
        this.score = new ScoreSystem();
        newGame();
    }

    // ---- GameApi lifecycle ---------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Rebuilds the live {@link GameState} from the first {@link LevelData} level
     * (round 1, {@link Phase#TITLE}) and runs an initial {@link TerritoryCalculator}
     * pass so the UI's first frame shows correct enclosure.</p>
     */
    @Override
    public void newGame() {
        this.levelIndex = 0;
        this.state = LevelData.LEVELS.get(levelIndex).newGameState();
        TerritoryCalculator.recompute(state);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Enters the BUILD phase for the current round via the
     * {@link PhaseController}.</p>
     */
    @Override
    public void startRound() {
        phases.startRound();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Advances the {@link PhaseController} timer, then — while in
     * {@link Phase#BATTLE} — ticks cannon reloads ({@link CannonSystem}), the fleet
     * ({@link ShipAI}), and re-runs the {@link TerritoryCalculator} so enclosure
     * reflects walls the ships have blasted.</p>
     */
    @Override
    public void tick(long dtMillis) {
        if (state.phase() == Phase.GAME_OVER) return;
        phases.tick(state, dtMillis);
        if (state.phase() == Phase.BATTLE) {
            cannons.tickReload(state, dtMillis);
            shipAi.tick(state, dtMillis);
            TerritoryCalculator.recompute(state);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the live {@link GameState} (an immutable-for-the-frame
     *         {@link GameStateView})
     */
    @Override
    public GameStateView state() {
        return state;
    }

    // ---- GameApi input -------------------------------------------------------

    /** {@inheritDoc} Delegates to {@link CannonSystem#placeCannon}. */
    @Override
    public boolean placeCannon(int col, int row) {
        return cannons.placeCannon(state, col, row);
    }

    /** {@inheritDoc} Delegates to {@link CannonSystem#fireCannonAt}. */
    @Override
    public boolean fireCannonAt(int col, int row) {
        return cannons.fireCannonAt(state, col, row, score);
    }

    /** {@inheritDoc} Delegates to {@link WallBuildSystem#rotatePiece}. */
    @Override
    public void rotatePiece() {
        walls.rotatePiece(state);
    }

    /** {@inheritDoc} Delegates to {@link WallBuildSystem#placePiece}. */
    @Override
    public boolean placePieceAt(int col, int row) {
        return walls.placePiece(state, col, row, rng);
    }

    /**
     * {@inheritDoc}
     *
     * @return the current {@link WallPieceView}, or {@code null} outside REPAIR
     */
    @Override
    public WallPieceView currentPiece() {
        return state.currentPieceModel();
    }

    /** {@inheritDoc} Delegates to {@link PhaseController#endPhaseEarly}. */
    @Override
    public void endPhaseEarly() {
        phases.endPhaseEarly(state);
    }

    /** {@inheritDoc} @return {@code true} once the phase is {@link Phase#GAME_OVER} */
    @Override
    public boolean isGameOver() {
        return state.phase() == Phase.GAME_OVER;
    }

    // ---- Phase-entry hooks (called by PhaseController) -----------------------

    /**
     * Enters the BUILD phase: recomputes territory, refills the cannon pool for the
     * current round, and arms the phase timer. Mutates the live {@link GameState}.
     */
    void enterBuild() {
        state.setPhase(Phase.BUILD);
        state.setTimerRemainingMillis(PhaseController.durationFor(Phase.BUILD));
        TerritoryCalculator.recompute(state);
        state.setCannonsRemainingToPlace(Rules.cannonPoolForRound(state.round()));
    }

    /**
     * Enters the BATTLE phase: spawns the round's wave ({@link ShipAI}), readies all
     * cannons, and arms the phase timer. Mutates the live {@link GameState}.
     */
    void enterBattle() {
        state.setPhase(Phase.BATTLE);
        state.setTimerRemainingMillis(PhaseController.durationFor(Phase.BATTLE));
        shipAi.spawnWave(state, state.round(), rng);
        for (int i = 0; i < state.cannonList().size(); i++) {
            Cannon c = state.cannonList().get(i);
            c.setReloadRemainingMillis(0L);
        }
    }

    /**
     * Enters the REPAIR phase: clears the spent fleet, deals the repair pieces
     * ({@link WallBuildSystem}), and arms the phase timer. Mutates the live
     * {@link GameState}.
     */
    void enterRepair() {
        state.setPhase(Phase.REPAIR);
        state.setTimerRemainingMillis(PhaseController.durationFor(Phase.REPAIR));
        state.shipList().clear();
        walls.startRepair(state, rng);
    }

    /**
     * Ends REPAIR: recomputes territory, and if at least
     * {@link Rules#MIN_ENCLOSED_CASTLES_TO_SURVIVE} castles are enclosed, awards the
     * survival score ({@link ScoreSystem}) and advances the round. Clears the repair
     * pieces either way. Mutates the live {@link GameState}.
     *
     * @return {@code true} if the round was survived
     */
    boolean endRepairAndScore() {
        TerritoryCalculator.recompute(state);
        boolean survived = state.enclosedCastleCount() >= Rules.MIN_ENCLOSED_CASTLES_TO_SURVIVE;
        state.setCurrentPiece(null);
        state.queuedPieceList().clear();
        if (survived) {
            score.scoreRoundSurvival(state);
            state.setRound(state.round() + 1);
        }
        return survived;
    }

    /**
     * Enters the between-rounds ROUND_TRANSITION splash and arms its timer. Mutates
     * the live {@link GameState}.
     */
    void enterRoundTransition() {
        state.setPhase(Phase.ROUND_TRANSITION);
        state.setTimerRemainingMillis(PhaseController.durationFor(Phase.ROUND_TRANSITION));
    }

    /** Enters the terminal GAME_OVER phase. Mutates the live {@link GameState}. */
    void enterGameOver() {
        state.setPhase(Phase.GAME_OVER);
        state.setTimerRemainingMillis(0L);
    }

    // ---- Engine-internal test seam -------------------------------------------

    /**
     * @return the concrete live {@link GameState} (package-private, for
     *         {@link EngineSelfCheck} and sibling systems — never handed to the UI)
     */
    GameState stateModel() {
        return state;
    }
}

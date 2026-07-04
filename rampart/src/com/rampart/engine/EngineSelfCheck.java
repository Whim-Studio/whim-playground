package com.rampart.engine;

import java.util.List;

import com.rampart.model.Coord;
import com.rampart.model.GameState;
import com.rampart.model.GameStateView;
import com.rampart.model.Phase;
import com.rampart.model.ShipView;
import com.rampart.model.TileType;
import com.rampart.model.WallPiece;
import com.rampart.model.WallShape;

/**
 * Headless self-check that drives a scripted Rampart round entirely through the
 * engine (no Swing/AWT) and prints {@code PASS}/{@code FAIL} for each assertion.
 *
 * <p>It consumes the engine's {@link GameApi} surface plus the concrete
 * {@link GameState} (via the package-private test seam) and the
 * {@code com.rampart.model} types ({@link GameStateView}, {@link Phase},
 * {@link ShipView}, {@link WallPiece}, {@link WallShape}, {@link Coord},
 * {@link TileType}). The scripted flow: new game &rarr; BUILD (assert the level-1
 * castle is pre-enclosed) &rarr; place a cannon &rarr; BATTLE (assert a wave spawned,
 * fire a cannon) &rarr; simulate a wall breach and assert enclosure is lost &rarr;
 * REPAIR the gap and assert enclosure returns &rarr; end REPAIR and assert the round
 * is survived and scored.</p>
 */
public final class EngineSelfCheck {

    private EngineSelfCheck() {}

    private static int checks;
    private static int failures;

    /**
     * Runs the scripted headless round and exits non-zero if any assertion failed.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        GameEngine engine = new GameEngine(12345L);
        GameState state = engine.stateModel();

        // 1. Fresh game sits at TITLE with the level-1 castle present.
        check("new game at TITLE", state.phase() == Phase.TITLE);
        check("one castle in level 1", state.castleList().size() == 1);

        // 2. BUILD phase: level 1's castle is pre-ringed, so it starts enclosed.
        engine.startRound();
        check("phase is BUILD", state.phase() == Phase.BUILD);
        check("castle pre-enclosed", state.enclosedCastleCount() == 1);
        check("territory fraction > 0", state.territoryFraction() > 0.0);
        check("cannon pool seeded", state.cannonsRemainingToPlace() > 0);

        // 3. Place a cannon on enclosed land inside the ring (castle is at (14,10)).
        int poolBefore = state.cannonsRemainingToPlace();
        boolean placed = engine.placeCannon(12, 10);
        check("cannon placed on enclosed land", placed);
        check("cannon list grew", state.cannonList().size() == 1);
        check("cannon pool decremented", state.cannonsRemainingToPlace() == poolBefore - 1);
        check("cannon cell is CANNON", state.gridModel().typeAt(12, 10) == TileType.CANNON);
        check("cannot place on water", !engine.placeCannon(0, 0));

        // 4. BATTLE phase: a wave spawns and a ready cannon can fire.
        engine.endPhaseEarly();
        check("phase is BATTLE", state.phase() == Phase.BATTLE);
        check("wave spawned", !state.shipList().isEmpty());
        for (int i = 0; i < 10; i++) {
            engine.tick(100L);
        }
        List<? extends ShipView> ships = engine.state().ships();
        boolean fired = false;
        if (!ships.isEmpty()) {
            ShipView s = ships.get(0);
            fired = engine.fireCannonAt((int) Math.round(s.x()), (int) Math.round(s.y()));
        }
        check("a loaded cannon fired", fired);

        // 5. Simulate a battle breach: knock a ring wall to RUBBLE -> enclosure lost.
        state.gridModel().setType(12, 8, TileType.RUBBLE);
        TerritoryCalculator.recompute(state);
        check("breach loses enclosure", state.enclosedCastleCount() == 0);

        // 6. REPAIR phase: seal the gap with a forced DOT piece -> enclosure returns.
        engine.endPhaseEarly();
        check("phase is REPAIR", state.phase() == Phase.REPAIR);
        state.setCurrentPiece(new WallPiece(WallShape.DOT, new Coord(0, 0)));
        boolean repaired = engine.placePieceAt(12, 8);
        check("repair piece placed", repaired);
        check("gap is WALL again", state.gridModel().typeAt(12, 8) == TileType.WALL);
        check("enclosure restored", state.enclosedCastleCount() == 1);

        // 7. End REPAIR: survival satisfied -> transition to next round, scored.
        long scoreBefore = state.score();
        int roundBefore = state.round();
        engine.endPhaseEarly();
        check("survived to ROUND_TRANSITION", state.phase() == Phase.ROUND_TRANSITION);
        check("round advanced", state.round() == roundBefore + 1);
        check("survival scored", state.score() > scoreBefore);
        check("not game over", !engine.isGameOver());

        // Summary
        System.out.println("----");
        if (failures == 0) {
            System.out.println("PASS (" + checks + "/" + checks + " checks)");
            System.exit(0);
        } else {
            System.out.println("FAIL (" + (checks - failures) + "/" + checks + " checks, "
                    + failures + " failed)");
            System.exit(1);
        }
    }

    /** Records and prints a single assertion result. */
    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    }
}

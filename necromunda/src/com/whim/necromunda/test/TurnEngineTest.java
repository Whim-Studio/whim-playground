package com.whim.necromunda.test;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.Phase;
import com.whim.necromunda.engine.TurnManager;
import com.whim.necromunda.engine.setup.DemoSetup;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterStatus;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.board.Board;

/**
 * Milestone 4 — turn/phase state machine tests. Verifies the RECOVERY→MOVEMENT→
 * SHOOTING→CLOSE_COMBAT→END cycle, that automatic phases are skipped through,
 * that turns alternate between the two gangs, and that the generic Gang-Fight
 * scenario ends when a gang is wiped out.
 */
public final class TurnEngineTest {

    public static void main(String[] args) {
        Assert a = new Assert();

        a.section("Phase.next() cycle");
        a.that("RECOVERY -> MOVEMENT", Phase.RECOVERY.next() == Phase.MOVEMENT);
        a.that("MOVEMENT -> SHOOTING", Phase.MOVEMENT.next() == Phase.SHOOTING);
        a.that("SHOOTING -> CLOSE_COMBAT", Phase.SHOOTING.next() == Phase.CLOSE_COMBAT);
        a.that("CLOSE_COMBAT -> END", Phase.CLOSE_COMBAT.next() == Phase.END);
        a.that("END wraps to RECOVERY", Phase.END.next() == Phase.RECOVERY);
        a.that("RECOVERY is automatic", Phase.RECOVERY.isAutomatic());
        a.that("MOVEMENT is player-driven", !Phase.MOVEMENT.isAutomatic());
        a.that("END is last in turn", Phase.END.isLastInTurn());

        a.section("Battle start lands on Movement");
        TurnManager turns = freshBattle();
        GameState state = turns.state();
        turns.startBattle(0);
        a.that("active player is gang A", state.activePlayerIndex() == 0);
        a.equalsInt("turn number is 1", 1, state.turnNumber());
        a.that("phase is MOVEMENT (recovery auto-resolved)", state.phase() == Phase.MOVEMENT);
        a.that("log recorded the opening", !state.log().isEmpty());

        a.section("Advancing player-driven phases");
        turns.advancePhase();
        a.that("MOVEMENT -> SHOOTING", state.phase() == Phase.SHOOTING);
        turns.advancePhase();
        a.that("SHOOTING -> CLOSE_COMBAT", state.phase() == Phase.CLOSE_COMBAT);

        a.section("End of turn passes to opponent");
        turns.advancePhase(); // CLOSE_COMBAT -> END -> pass
        a.that("active player is now gang B", state.activePlayerIndex() == 1);
        a.equalsInt("turn number advanced to 2", 2, state.turnNumber());
        a.that("opponent lands on MOVEMENT", state.phase() == Phase.MOVEMENT);

        a.section("End Turn shortcut");
        turns.endTurn(); // from B's MOVEMENT straight to pass
        a.that("back to gang A", state.activePlayerIndex() == 0);
        a.equalsInt("turn number is 3", 3, state.turnNumber());
        a.that("gang A on MOVEMENT again", state.phase() == Phase.MOVEMENT);

        a.section("Recovery clears the acted flag each turn");
        boolean allFresh = true;
        for (Fighter f : state.activeGang().roster()) {
            if (f.hasActed()) {
                allFresh = false;
            }
        }
        a.that("active gang fighters start un-acted", allFresh);

        a.section("Scenario ends when a gang is wiped out");
        TurnManager t2 = freshBattle();
        GameState s2 = t2.state();
        t2.startBattle(0);
        // Wipe gang B.
        Gang b = s2.gangs().get(1);
        for (Fighter f : b.roster()) {
            f.setStatus(FighterStatus.OUT_OF_ACTION);
        }
        t2.endTurn(); // completing gang A's turn triggers the scenario check
        a.that("battle is over", t2.isBattleOver());
        a.that("winner is gang A", t2.winner() == s2.gangs().get(0));

        a.finish();
    }

    private static TurnManager freshBattle() {
        Board board = DemoSetup.demoBoard();
        Gang a = DemoSetup.gangA();
        Gang b = DemoSetup.gangB();
        DemoSetup.placeGangs(board, a, b);
        GameState state = new GameState(board, a, b, 123L);
        return new TurnManager(state);
    }
}

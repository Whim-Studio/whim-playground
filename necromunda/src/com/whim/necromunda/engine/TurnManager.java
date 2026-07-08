package com.whim.necromunda.engine;

import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterStatus;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.Stat;

/**
 * Drives the turn/phase state machine over a {@link GameState}.
 *
 * <p>The cycle is strictly one-directional:
 * <pre>RECOVERY → MOVEMENT → SHOOTING → CLOSE_COMBAT → END → (swap player) → RECOVERY …</pre>
 *
 * RECOVERY and END are engine-automatic: the manager resolves them and never
 * rests there, so the player only ever sits in MOVEMENT / SHOOTING /
 * CLOSE_COMBAT. {@link #advancePhase()} steps forward one player-driven phase;
 * {@link #endTurn()} jumps straight to the end and passes the turn. This is
 * hotseat (both gangs human) — per-fighter actions arrive in Milestone 5.
 */
public final class TurnManager {

    /** Bottle test triggers once at least this fraction of the gang is Down/Out. */
    public static final double BOTTLE_THRESHOLD = 0.25;

    private final GameState state;
    private boolean battleOver;
    private Gang bottledGang;

    public TurnManager(GameState state) {
        this.state = state;
    }

    public GameState state() {
        return state;
    }

    public boolean isBattleOver() {
        return battleOver;
    }

    // ------------------------------------------------------------ battle start

    /**
     * Begin the battle. Both gangs are reset to full readiness, the first player
     * takes priority (from the initial roll-off), and their RECOVERY phase is
     * auto-resolved so play lands on MOVEMENT.
     */
    public void startBattle(int firstPlayerIndex) {
        for (Gang g : state.gangs()) {
            g.resetForBattle();
        }
        state.setActivePlayerIndex(firstPlayerIndex);
        state.setTurnNumber(1);
        state.setPhase(Phase.RECOVERY);
        state.log("Battle begins — " + state.activeGang().name() + " has priority.");
        beginActivePlayerTurn();
        resolveRecovery();
        state.setPhase(Phase.MOVEMENT);
        state.log(state.activeGang().name() + " enters the Movement phase.");
        state.fireChanged();
    }

    // --------------------------------------------------------- phase advancing

    /** Step forward one player-driven phase (the "Next Phase" button). */
    public void advancePhase() {
        if (battleOver) {
            return;
        }
        Phase cur = state.phase();
        if (cur == Phase.MOVEMENT) {
            enter(Phase.SHOOTING);
        } else if (cur == Phase.SHOOTING) {
            enter(Phase.CLOSE_COMBAT);
        } else {
            // CLOSE_COMBAT (or any resting state) → wrap up and pass the turn.
            completeTurn();
        }
        state.fireChanged();
    }

    /** Jump straight to the end of the turn and pass to the opponent ("End Turn"). */
    public void endTurn() {
        if (battleOver) {
            return;
        }
        completeTurn();
        state.fireChanged();
    }

    private void enter(Phase phase) {
        state.setPhase(phase);
        state.log(state.activeGang().name() + " enters the " + phase.label() + " phase.");
    }

    /** Resolve the END phase, then hand the turn to the opponent. */
    private void completeTurn() {
        state.setPhase(Phase.END);
        resolveEnd();
        if (checkScenarioEnd()) {
            return;
        }
        passToOpponent();
    }

    private void passToOpponent() {
        state.setActivePlayerIndex(1 - state.activePlayerIndex());
        state.setTurnNumber(state.turnNumber() + 1);
        state.setPhase(Phase.RECOVERY);
        state.log("Turn passes to " + state.activeGang().name() + ".");
        beginActivePlayerTurn();
        resolveRecovery();
        if (checkScenarioEnd()) {
            return;
        }
        state.setPhase(Phase.MOVEMENT);
        state.log(state.activeGang().name() + " enters the Movement phase.");
    }

    // ----------------------------------------------------- automatic phases

    private void beginActivePlayerTurn() {
        for (Fighter f : state.activeGang().roster()) {
            f.beginTurn();
        }
    }

    /** RECOVERY: pinned fighters test to recover; then the gang bottle test. */
    private void resolveRecovery() {
        Gang gang = state.activeGang();
        for (Fighter f : gang.roster()) {
            if (f.status() == FighterStatus.PINNED) {
                int roll = state.dice().d6();
                if (roll <= f.stat(Stat.I)) {
                    f.setStatus(FighterStatus.ACTIVE);
                    state.log(f.name() + " recovers from being pinned (I test " + roll
                            + " vs " + f.stat(Stat.I) + ").");
                } else {
                    state.log(f.name() + " stays pinned (I test " + roll
                            + " vs " + f.stat(Stat.I) + ").");
                }
            }
        }
        maybeBottleTest(gang);
    }

    private void maybeBottleTest(Gang gang) {
        int size = gang.roster().size();
        if (size == 0) {
            return;
        }
        int downOrOut = gang.downOrOutCount();
        if ((double) downOrOut / (double) size < BOTTLE_THRESHOLD) {
            return;
        }
        int ld = leadershipOf(gang);
        int roll = state.dice().roll2d6();
        if (roll <= ld) {
            state.log(gang.name() + " holds its nerve (bottle test " + roll
                    + " vs Ld " + ld + ").");
        } else {
            state.log(gang.name() + " BOTTLES OUT and flees the field (test " + roll
                    + " vs Ld " + ld + ")!");
            for (Fighter f : gang.roster()) {
                if (f.status().inPlay()) {
                    f.setStatus(FighterStatus.FLED);
                }
            }
            bottledGang = gang;
        }
    }

    private int leadershipOf(Gang gang) {
        // Use the Leader's Leadership if present, else the best in the gang.
        int best = 0;
        for (Fighter f : gang.roster()) {
            if (f.status().inPlay()) {
                best = Math.max(best, f.stat(Stat.LD));
            }
        }
        return best;
    }

    /** END: clear per-turn flags (overwatch) and tidy up. */
    private void resolveEnd() {
        for (Fighter f : state.activeGang().roster()) {
            f.setOnOverwatch(false);
        }
        state.log("End phase — the field is tidied and the turn closes.");
    }

    // ------------------------------------------------------------- scenario

    /**
     * Generic "Gang Fight" end check: the battle ends if a gang has bottled out
     * or has no fighters left in play. Sets {@link #battleOver} and logs the
     * winner.
     */
    private boolean checkScenarioEnd() {
        Gang a = state.gangs().get(0);
        Gang b = state.gangs().get(1);
        Gang loser = null;
        if (bottledGang != null) {
            loser = bottledGang;
        } else if (a.inPlay().isEmpty()) {
            loser = a;
        } else if (b.inPlay().isEmpty()) {
            loser = b;
        }
        if (loser == null) {
            return false;
        }
        Gang winner = (loser == a) ? b : a;
        battleOver = true;
        state.log("Battle over — " + winner.name() + " wins the field.");
        return true;
    }

    public Gang winner() {
        if (!battleOver) {
            return null;
        }
        Gang a = state.gangs().get(0);
        Gang b = state.gangs().get(1);
        if (bottledGang != null) {
            return bottledGang == a ? b : a;
        }
        if (a.inPlay().isEmpty()) {
            return b;
        }
        if (b.inPlay().isEmpty()) {
            return a;
        }
        return null;
    }
}

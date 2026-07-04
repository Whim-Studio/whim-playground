package com.rampart.engine;

import com.rampart.model.GameState;
import com.rampart.model.Phase;
import com.rampart.model.Rules;

/**
 * Owns the phase state machine and per-phase countdown timers for one game. Drives
 * BUILD &rarr; BATTLE &rarr; REPAIR &rarr; (round-survival check) &rarr;
 * ROUND_TRANSITION &rarr; next BUILD, or ends at GAME_OVER when survival fails.
 *
 * <p>Reads and writes the {@link com.rampart.model.Phase} and timer on the live
 * {@link GameState} via its owning {@link GameEngine}; the phase-entry side effects
 * (territory recompute, ship spawning, piece dealing, scoring) are delegated back to
 * the {@link GameEngine} so this class holds only sequencing and timing.</p>
 */
public final class PhaseController {

    private final GameEngine engine;

    /**
     * @param engine the {@link GameEngine} whose {@link GameState} this controller
     *               sequences (must be non-null)
     */
    public PhaseController(GameEngine engine) {
        if (engine == null) throw new IllegalArgumentException("engine must not be null");
        this.engine = engine;
    }

    /** Begins the current round by entering the BUILD (cannon-placement) phase. */
    public void startRound() {
        engine.enterBuild();
    }

    /**
     * Advances the current phase's timer by {@code dtMillis}, auto-advancing to the
     * next phase when it reaches zero. No-op for the untimed {@link Phase#TITLE} and
     * {@link Phase#GAME_OVER} states.
     *
     * @param state    the live {@link GameState}
     * @param dtMillis elapsed milliseconds since the previous tick
     */
    public void tick(GameState state, long dtMillis) {
        if (!isTimed(state.phase())) return;
        long remaining = state.timerRemainingMillis() - Math.max(0L, dtMillis);
        if (remaining <= 0L) {
            state.setTimerRemainingMillis(0L);
            advance(state);
        } else {
            state.setTimerRemainingMillis(remaining);
        }
    }

    /**
     * Ends the current timed phase immediately, as if its timer had expired. No-op
     * outside a timed phase.
     *
     * @param state the live {@link GameState}
     */
    public void endPhaseEarly(GameState state) {
        if (isTimed(state.phase())) advance(state);
    }

    /** Advances from the current phase to the next one in the loop. */
    private void advance(GameState state) {
        switch (state.phase()) {
            case BUILD:
                engine.enterBattle();
                break;
            case BATTLE:
                engine.enterRepair();
                break;
            case REPAIR:
                if (engine.endRepairAndScore()) {
                    engine.enterRoundTransition();
                } else {
                    engine.enterGameOver();
                }
                break;
            case ROUND_TRANSITION:
                engine.enterBuild();
                break;
            default:
                break;
        }
    }

    /** @return {@code true} for the four phases that run a countdown timer */
    private static boolean isTimed(Phase phase) {
        return phase == Phase.BUILD || phase == Phase.BATTLE
                || phase == Phase.REPAIR || phase == Phase.ROUND_TRANSITION;
    }

    /**
     * The default remaining-time for a phase, drawn from {@link Rules}. Exposed so
     * {@link GameEngine} seeds the same durations this controller counts down.
     *
     * @param phase the phase to size
     * @return the phase length in milliseconds (0 for untimed phases)
     */
    public static long durationFor(Phase phase) {
        switch (phase) {
            case BUILD:            return Rules.BUILD_PHASE_MILLIS;
            case BATTLE:           return Rules.BATTLE_PHASE_MILLIS;
            case REPAIR:           return Rules.REPAIR_PHASE_MILLIS;
            case ROUND_TRANSITION: return Rules.ROUND_TRANSITION_MILLIS;
            default:               return 0L;
        }
    }
}

package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Job;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

import java.util.Random;

/**
 * The colony tick engine. {@link #tick(ColonyState)} advances the world by
 * exactly one game-tick and is completely decoupled from rendering / frame rate:
 * the UI (Task 3) only ever reads {@link ColonyState}, never drives it.
 *
 * <p>Each tick, in order:
 * <ol>
 *   <li>advance the tick counter;</li>
 *   <li>decay every colonist's needs (hunger &amp; rest fall, mood drifts with
 *       how well those needs are met);</li>
 *   <li>run the AI decision step so idle/finished colonists pick a new job and
 *       get a path planted;</li>
 *   <li>advance each colonist's current job one step (move along its path, or do
 *       one tick of work);</li>
 *   <li>consult the {@link Storyteller}, which may fire an event.</li>
 * </ol>
 *
 * <p>{@link #tick} runs its work unconditionally (so a UI "step once" button can
 * force a tick even while paused); it is the {@link SimulationClock} that honours
 * {@link ColonyState#isPaused()} for the automatic cadence.
 */
public final class Simulation {

    /** Hunger points lost per tick. */
    private static final double HUNGER_DECAY = 0.15;
    /** Rest points lost per tick. */
    private static final double REST_DECAY = 0.10;
    /** Mood recovered per tick when all needs are comfortable. */
    private static final double MOOD_RECOVERY = 0.10;
    /** Mood lost per tick per need that is below its LOW threshold. */
    private static final double MOOD_PENALTY_LOW = 0.20;
    /** Extra mood lost per tick per need that is CRITICAL. */
    private static final double MOOD_PENALTY_CRITICAL = 0.30;

    private final AiController ai;
    private final Storyteller storyteller;

    /** Build a simulation with a random (non-deterministic) seed. */
    public Simulation() {
        this(new Random());
    }

    /** Build a simulation whose AI and storyteller share the given RNG (seedable). */
    public Simulation(Random rng) {
        this.ai = new AiController(new Pathfinder(), rng);
        this.storyteller = new Storyteller(rng);
    }

    /**
     * Advance the world by one game-tick. Safe to call directly to force a single
     * step (e.g. from a paused UI); the automatic clock gates on pause itself.
     */
    public void tick(ColonyState state) {
        if (state == null) {
            return;
        }
        state.incrementTick();

        for (Colonist c : state.getColonists()) {
            decayNeeds(c);
        }
        for (Colonist c : state.getColonists()) {
            ai.assignIfIdle(state, c);
            advanceJob(state, c);
        }
        storyteller.maybeFire(state);
    }

    /** @return the storyteller, e.g. for the UI to show the next-event countdown. */
    public Storyteller getStoryteller() {
        return storyteller;
    }

    private void decayNeeds(Colonist c) {
        Needs needs = c.getNeeds();
        needs.setHunger(needs.getHunger() - HUNGER_DECAY);
        needs.setRest(needs.getRest() - REST_DECAY);
        needs.setMood(needs.getMood() + moodDelta(needs));
    }

    /** Mood drifts up when fed and rested, down when hungry or tired. */
    private double moodDelta(Needs needs) {
        double delta = 0.0;
        boolean hungerLow = needs.getHunger() < Needs.LOW_THRESHOLD;
        boolean restLow = needs.getRest() < Needs.LOW_THRESHOLD;
        if (hungerLow) {
            delta -= MOOD_PENALTY_LOW;
        }
        if (restLow) {
            delta -= MOOD_PENALTY_LOW;
        }
        if (needs.getHunger() < Needs.CRITICAL_THRESHOLD) {
            delta -= MOOD_PENALTY_CRITICAL;
        }
        if (needs.getRest() < Needs.CRITICAL_THRESHOLD) {
            delta -= MOOD_PENALTY_CRITICAL;
        }
        if (!hungerLow && !restLow) {
            delta += MOOD_RECOVERY;
        }
        return delta;
    }

    /** Drive the colonist's current job forward by one tick, if it has one. */
    private void advanceJob(ColonyState state, Colonist c) {
        Job job = c.getCurrentJob();
        if (job instanceof AbstractColonyJob) {
            ((AbstractColonyJob) job).tick(state, c);
        }
    }
}

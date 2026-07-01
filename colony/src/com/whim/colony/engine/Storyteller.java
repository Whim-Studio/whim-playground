package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Event;

import java.util.Random;

/**
 * The colony's narrator. On a randomized cadence it decides whether to fire an
 * {@link Event} (a cold snap, a raid, or a supply drop), builds a concrete event
 * instance sized by a random severity, applies it, and lets the event push a
 * line into the colony message log.
 *
 * <p>Cadence is expressed in ticks: after each fired event the storyteller picks
 * the next fire tick uniformly in {@code [MIN_GAP, MAX_GAP]}. Seed the supplied
 * {@link Random} for deterministic runs.
 */
public final class Storyteller {

    /** Minimum ticks between events. */
    public static final int MIN_GAP_TICKS = 120;
    /** Maximum ticks between events. */
    public static final int MAX_GAP_TICKS = 360;

    private final Random rng;
    private long nextEventTick;

    public Storyteller(Random rng) {
        this.rng = rng;
        this.nextEventTick = scheduleGap(0L);
    }

    /**
     * Called by the {@link Simulation} each tick. If the current tick has reached
     * the scheduled fire time, an event is generated, applied to {@code state},
     * and the next fire time is scheduled. Returns the fired event (or
     * {@code null} if nothing happened this tick) so callers can react if needed.
     */
    public Event maybeFire(ColonyState state) {
        long tick = state.getTick();
        if (tick < nextEventTick) {
            return null;
        }
        Event event = generate(state);
        event.apply(state);
        nextEventTick = scheduleGap(tick);
        return event;
    }

    /** @return the tick at which the next event is currently scheduled to fire. */
    public long getNextEventTick() {
        return nextEventTick;
    }

    /** Build a random event, biased slightly toward benign supply drops. */
    private Event generate(ColonyState state) {
        int roll = rng.nextInt(100);
        if (roll < 40) {
            // Supply drop (40%).
            int food = 8 + rng.nextInt(15);
            int steel = 3 + rng.nextInt(10);
            int wood = 3 + rng.nextInt(10);
            return new ResourceDropEvent(food, steel, wood);
        } else if (roll < 75) {
            // Cold snap (35%).
            return new ColdSnapEvent(1 + rng.nextInt(6));
        } else {
            // Raid (25%).
            int victim = -1;
            int count = state.getColonists().size();
            if (count > 0) {
                victim = rng.nextInt(count);
            }
            return new RaidEvent(2 + rng.nextInt(6), victim);
        }
    }

    private long scheduleGap(long fromTick) {
        int span = MAX_GAP_TICKS - MIN_GAP_TICKS + 1;
        return fromTick + MIN_GAP_TICKS + rng.nextInt(span);
    }
}

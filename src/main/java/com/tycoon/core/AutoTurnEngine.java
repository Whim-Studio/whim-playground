package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ascension-style interrupt-driven auto-turn loop. Repeatedly advances the
 * game one in-game hour at a time, halting the moment any {@link Interrupt} is
 * produced so control can return to the player's paused building phase.
 */
public class AutoTurnEngine {
    private final GameState state;
    private final TurnProcessor processor;

    public AutoTurnEngine(GameState state, TurnProcessor processor) {
        this.state = state;
        this.processor = processor;
    }

    /**
     * Auto-advance hour-by-hour until an Interrupt is produced OR maxHours
     * elapse. On each hour: lock the FloorPlan, call processor.processHour,
     * advance the counter. Stops and returns as soon as any interrupt is
     * produced (control returns to the player). If the turn budget is exhausted
     * with no interrupt, returns a single MANUAL_PAUSE interrupt. The FloorPlan
     * is unlocked before returning.
     */
    public List<Interrupt> run(int maxHours) {
        try {
            for (int i = 0; i < maxHours; i++) {
                if (state.isGameOver()) {
                    return one(InterruptType.MANUAL_PAUSE, "Game over.");
                }
                List<Interrupt> produced = advanceOneHour();
                if (produced != null && !produced.isEmpty()) {
                    return produced;
                }
            }
            return one(InterruptType.MANUAL_PAUSE, "Turn budget exhausted (" + maxHours + " hours).");
        } finally {
            state.player().floorPlan().unlock();
        }
    }

    /**
     * Advance exactly one hour regardless of interrupts (used by UI single-step).
     * The FloorPlan is unlocked before returning.
     */
    public List<Interrupt> step() {
        try {
            return advanceOneHour();
        } finally {
            state.player().floorPlan().unlock();
        }
    }

    /** Lock layout, resolve one hour, advance the counter. */
    private List<Interrupt> advanceOneHour() {
        FloorPlan plan = state.player().floorPlan();
        plan.lock();
        List<Interrupt> produced = processor.processHour(state);
        state.advanceHourCounter();
        if (produced == null) {
            return new ArrayList<Interrupt>();
        }
        return produced;
    }

    private List<Interrupt> one(InterruptType type, String message) {
        List<Interrupt> list = new ArrayList<Interrupt>();
        list.add(new Interrupt(type, state.hour(), message));
        return list;
    }

    public GameState state() {
        return state;
    }
}

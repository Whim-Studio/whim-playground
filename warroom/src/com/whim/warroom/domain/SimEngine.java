package com.whim.warroom.domain;

/**
 * The simulation engine contract, implemented by Task 2's {@code SimEngineImpl}.
 * Simulation is deterministic from the loaded {@link SandboxState} + seed:
 * forward simulation advances integer ticks at {@code TICKS_PER_SECOND * speed},
 * and {@link #seek(int)}/{@link #snapshotAt(int)} return the correct recorded (or
 * deterministically replayed) frame — this is what makes rewind/scrub correct.
 */
public interface SimEngine {
    int TICKS_PER_SECOND = 60;

    /** Load a scenario (treated as the tick-0 baseline). */
    void loadScenario(SandboxState state);

    void addListener(SimListener l);

    /** Start/resume advancing wall-clock into ticks. */
    void play();

    /** Freeze at the current tick (the editor is still viewable). */
    void pause();

    boolean isPlaying();

    /** Playback speed multiplier only (0.25/0.5/1/2/4); never changes tick math. */
    void setSpeed(double multiplier);

    double getSpeed();

    /** Rewind or fast-forward to an absolute tick. */
    void seek(int tick);

    /** The tick currently presented. */
    int getCurrentTick();

    /** Furthest tick simulated so far (timeline length). */
    int getMaxSimTick();

    /** Deterministic frame for scrubbing; may simulate forward up to {@code tick}. */
    SimSnapshot snapshotAt(int tick);

    /** Return to tick 0 with the loaded scenario. */
    void reset();

    /** Stop the background thread; called on window close. */
    void shutdown();
}

package com.whim.babylon5.domain;

/**
 * A {@link GameListener} that ignores every event. Useful as a default so the
 * engine can run head-less (e.g. in tests) without a UI attached.
 */
public final class NoOpGameListener implements GameListener {

    @Override
    public void onPhaseChanged(Phase phase, int activeIndex) { }

    @Override
    public void onConflictResolved(com.whim.babylon5.engine.ConflictResult result) { }

    @Override
    public void onStateChanged() { }

    @Override
    public void onLog(String message) { }
}

package com.whim.babylon5.domain;

/**
 * Engine -> UI event sink. The UI (Task 3) implements this; the engine (Task 2)
 * fires the callbacks.
 *
 * <p>{@link #onConflictResolved} references {@code engine.ConflictResult}. This
 * is the single permitted domain -> engine reference and exists only so the UI
 * can receive conflict outcomes. The {@code engine.ConflictResult} type is
 * authored by Task 2; until that task merges this is the only unresolved symbol
 * in the {@code domain} package.</p>
 */
public interface GameListener {

    void onPhaseChanged(Phase phase, int activeIndex);

    void onConflictResolved(com.whim.babylon5.engine.ConflictResult result);

    void onStateChanged();

    void onLog(String message);
}

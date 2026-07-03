package com.arpg.ui;

import com.arpg.model.GameStateSnapshot;

/**
 * Anything that can refresh itself from a {@link GameStateSnapshot}.
 * {@link MainFrame} implements this so the event listener can push a fresh
 * snapshot into the whole UI with one call.
 */
public interface SnapshotConsumer {
    void refresh(GameStateSnapshot snapshot);
}

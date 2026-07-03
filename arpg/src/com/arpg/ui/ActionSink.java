package com.arpg.ui;

import com.arpg.model.PlayerAction;

/**
 * The one seam through which every panel submits a {@link PlayerAction}.
 * {@link MainFrame} implements this by forwarding to
 * {@code GameEngine.processPlayerAction} and then refreshing from a fresh
 * snapshot, keeping panels ignorant of the engine.
 */
public interface ActionSink {
    void submit(PlayerAction action);
}

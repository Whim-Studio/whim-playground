package com.whim.alganon.api;

import com.whim.alganon.api.Enums.ChatChannel;

/**
 * Callback surface the engine hands to model/content when it needs to emit side
 * effects (log lines, toasts) without the model depending on the engine. Keeps the
 * model layer free of UI/engine imports.
 */
public interface GameContext {
    void log(ChatChannel channel, String text);
    void toast(String text);
    /** Deterministic RNG shared across the run (seeded at newGame). */
    java.util.Random rng();
}

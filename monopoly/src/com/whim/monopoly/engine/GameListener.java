package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Player;

// UI registers one listener; engine pushes all updates. All callbacks fire on the EDT-safe path
// the engine guarantees by invoking listeners synchronously from the calling (UI) thread.
public interface GameListener {
    void onLog(String message);          // append to the scrolling log
    void onStateChanged();               // re-render board + panels from GameState
    void onGameOver(Player winner);
}

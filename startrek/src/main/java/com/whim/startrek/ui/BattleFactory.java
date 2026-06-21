package com.whim.startrek.ui;

import com.whim.startrek.domain.GameState;
import com.whim.startrek.engine.BattleSimulator;

/**
 * Pluggable hook the orchestrator can supply so {@link MainFrame} can spin up a
 * live {@link BattleSimulator} from the current {@link GameState} (e.g. when a TBS
 * encounter is opened as a real-time battle). Kept as a tiny SAM so the exact
 * battle-construction policy stays outside the UI.
 */
public interface BattleFactory {
    BattleSimulator create(GameState state);
}

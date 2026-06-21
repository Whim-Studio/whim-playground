package com.tycoon.ui;

import com.tycoon.core.GameState;
import com.tycoon.core.Interrupt;
import com.tycoon.core.TurnProcessor;

import java.util.Collections;
import java.util.List;

/**
 * Placeholder {@link TurnProcessor} that produces no interrupts.
 *
 * <p>Exists so the UI compiles and boots before the real sim module is merged.
 * TODO(consolidation): delete once {@code com.tycoon.sim.SimTurnProcessor} is wired in
 * {@link TycoonApp#resolveProcessor()}.</p>
 */
public class NoOpTurnProcessor implements TurnProcessor {
    @Override
    public List<Interrupt> processHour(GameState state) {
        return Collections.emptyList();
    }
}

package com.whim.starcraft8.ui;

import java.util.LinkedHashSet;
import java.util.Set;

import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.engine.Command;
import com.whim.starcraft8.engine.Simulation;

/**
 * Shared, UI-owned interaction state threaded between {@link GamePanel} and {@link Hud}.
 * Holds the latest {@link RenderState} snapshot, the current selection (UI-side only —
 * the engine never tracks "selected"), the interaction mode, and a thin enqueue wrapper.
 * NEVER mutates domain state directly.
 */
final class UiContext {

    static final int MODE_NORMAL = 0;
    static final int MODE_PLACING = 1;     // placing a building footprint
    static final int MODE_ATTACK_MOVE = 2; // next click issues attack-move

    final Simulation sim;
    final int humanId;
    final Camera camera = new Camera();

    volatile RenderState render;

    // selection (UI-owned). Either a set of own units OR a single own building.
    final Set<Long> selectedUnits = new LinkedHashSet<Long>();
    long selectedBuilding = -1;

    int mode = MODE_NORMAL;
    BuildingType placingType = null;
    long placingWorker = -1;

    // hover tile (for footprint preview), in tile units
    int hoverTileX = -1;
    int hoverTileY = -1;

    UiContext(Simulation sim, int humanId) {
        this.sim = sim;
        this.humanId = humanId;
    }

    void enqueue(Command c) {
        if (c != null) sim.enqueue(c);
    }

    Race humanRace() {
        RenderState rs = render;
        return rs != null ? rs.humanRace : null;
    }

    void clearSelection() {
        selectedUnits.clear();
        selectedBuilding = -1;
    }

    void cancelMode() {
        mode = MODE_NORMAL;
        placingType = null;
        placingWorker = -1;
    }

    /** Begin building placement with the given type, using the first selected worker. */
    void beginPlacement(BuildingType type, long workerId) {
        mode = MODE_PLACING;
        placingType = type;
        placingWorker = workerId;
    }
}

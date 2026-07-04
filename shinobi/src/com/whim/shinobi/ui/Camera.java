package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Views;

/**
 * Maps world coordinates to screen coordinates by translating by -cameraX.
 *
 * The engine may drive the camera authoritatively via {@link Views.GameStateView#cameraX()};
 * when that value is nonzero we follow it. Otherwise (e.g. very start of level, or a
 * stub that doesn't set it) we lock onto the player's X ourselves and clamp to the
 * scrollable range [0, LEVEL_W - VIEW_W].
 */
public final class Camera {
    private double cameraX;

    /** Recompute the camera left-edge for this frame. */
    public void update(Views.GameStateView state) {
        double stateCam = state.cameraX();
        if (stateCam > 0.0) {
            cameraX = clamp(stateCam, state.levelWidth());
            return;
        }
        // Fall back to following the player centered in the viewport.
        Views.PlayerView p = state.player();
        double target = (p.x() + p.w() / 2.0) - Config.VIEW_W / 2.0;
        cameraX = clamp(target, state.levelWidth());
    }

    private static double clamp(double x, int levelWidth) {
        double max = levelWidth - Config.VIEW_W;
        if (max < 0) max = 0;
        if (x < 0) return 0;
        if (x > max) return max;
        return x;
    }

    public double cameraX() { return cameraX; }

    /** World X -> screen X. */
    public int sx(double worldX) { return (int) Math.round(worldX - cameraX); }

    /** World Y -> screen Y (no vertical scroll). */
    public int sy(double worldY) { return (int) Math.round(worldY); }
}

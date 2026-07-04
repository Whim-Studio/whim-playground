package com.whim.shinobi.api;

import java.util.List;

/**
 * Read-only snapshot interfaces. The UI (Task 3) renders ONLY through these and
 * NEVER casts a *View to a concrete domain/engine class. The engine (Task 2)
 * exposes its live domain objects (Task 1) as these views. All coordinates are
 * world pixels; (x,y) is the entity's top-left collision-box corner.
 *
 * DO NOT modify — the seam between engine and UI.
 */
public final class Views {
    private Views() {}

    /** Axis-aligned bounding box in world space. */
    public interface BoxView {
        double x();
        double y();
        double w();
        double h();
    }

    public interface EntityView extends BoxView {
        Enums.Plane plane();
        Enums.Facing facing();
        Enums.EntityState state();
        boolean alive();
    }

    public interface PlayerView extends EntityView {
        int lives();
        int score();
        Enums.Weapon weapon();
        int ninjutsu();
        /** True during post-attack recovery, for pose rendering. */
        Enums.AttackMode lastAttack();
    }

    public interface EnemyView extends EntityView {
        Enums.EnemyType type();
        /** True while actively blocking (ninja deflect pose). */
        boolean blocking();
    }

    public interface ProjectileView extends EntityView {
        boolean fromPlayer();
        Enums.Weapon weapon();
    }

    public interface HostageView extends BoxView {
        Enums.Plane plane();
        boolean rescued();
    }

    /** Solid terrain segment on a given plane. */
    public interface PlatformView extends BoxView {
        Enums.Plane plane();
    }

    /**
     * Immutable-per-frame snapshot the UI reads each repaint. The engine returns
     * a consistent view; the UI must not mutate anything reachable from here.
     */
    public interface GameStateView {
        PlayerView player();
        List<EnemyView> enemies();
        List<ProjectileView> projectiles();
        List<HostageView> hostages();
        List<PlatformView> platforms();

        /** Camera left edge in world pixels; UI translates by -cameraX(). */
        double cameraX();
        int levelWidth();

        Enums.Phase phase();
        int secondsRemaining();
        int hostagesRescued();
        int hostagesTotal();

        /**
         * 0.0 when idle, ramps 0->1 during the Ninjutsu screen-clear animation so
         * the UI can drive a full-screen flash. -1 when not active.
         */
        double ninjutsuFlash();
    }
}

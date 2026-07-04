package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Static level geometry + spawn data for a stage: platforms on both planes,
 * hostages, and lightweight enemy spawn descriptors, plus the level width.
 * Offers query helpers the engine uses for placement and grounding.
 *
 * This is pure data — no behavior. {@link LevelBuilder} populates it and turns it
 * into a live {@link WorldState}.
 */
public class LevelMap {

    /** Lightweight descriptor for where/what enemy to spawn. */
    public static final class EnemySpawn {
        public final double x;
        public final Enums.Plane plane;
        public final Enums.EnemyType type;

        public EnemySpawn(double x, Enums.Plane plane, Enums.EnemyType type) {
            this.x = x;
            this.plane = plane;
            this.type = type;
        }
    }

    private final List<Platform> platforms = new ArrayList<Platform>();
    private final List<Hostage> hostages = new ArrayList<Hostage>();
    private final List<EnemySpawn> enemySpawns = new ArrayList<EnemySpawn>();
    private final int levelWidth;

    public LevelMap(int levelWidth) {
        this.levelWidth = levelWidth;
    }

    // ---- Population (used by LevelBuilder) ----
    public void addPlatform(Platform p) { platforms.add(p); }
    public void addHostage(Hostage h) { hostages.add(h); }
    public void addEnemySpawn(EnemySpawn s) { enemySpawns.add(s); }

    // ---- Accessors ----
    public int levelWidth() { return levelWidth; }
    public List<Platform> platforms() { return platforms; }
    public List<Hostage> hostages() { return hostages; }
    public List<EnemySpawn> enemySpawns() { return enemySpawns; }

    /** All platforms on the given plane, in insertion order. */
    public List<Platform> platformsOnPlane(Enums.Plane plane) {
        List<Platform> out = new ArrayList<Platform>();
        for (int i = 0; i < platforms.size(); i++) {
            Platform p = platforms.get(i);
            if (p.plane() == plane) out.add(p);
        }
        return out;
    }

    /**
     * Feet-Y (top surface) an entity on {@code plane} stands on at world X. Uses
     * the highest platform (smallest Y) on that plane covering X; falls back to
     * the plane's default ground line from {@link Config} if none covers X.
     */
    public double groundYFor(Enums.Plane plane, double x) {
        double best = defaultGroundY(plane);
        boolean found = false;
        for (int i = 0; i < platforms.size(); i++) {
            Platform p = platforms.get(i);
            if (p.plane() != plane) continue;
            if (!p.spansX(x)) continue;
            double top = p.topY();
            if (!found || top < best) {
                best = top;
                found = true;
            }
        }
        return best;
    }

    /** The default ground feet-Y for a plane, per {@link Config}. */
    public static double defaultGroundY(Enums.Plane plane) {
        return plane == Enums.Plane.UPPER ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER;
    }
}

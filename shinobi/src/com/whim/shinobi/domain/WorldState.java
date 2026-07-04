// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import java.util.ArrayList;
import java.util.List;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** Live, mutable world; implements the per-frame {@link Views.GameStateView}. */
public class WorldState implements Views.GameStateView {
    public Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<Enemy>();
    public final List<Projectile> projectiles = new ArrayList<Projectile>();
    public final List<Hostage> hostages = new ArrayList<Hostage>();
    public final List<Platform> platforms = new ArrayList<Platform>();

    public double cameraX = 0;
    public int levelWidth = Config.LEVEL_W;
    public Enums.Phase phase = Enums.Phase.PLAYING;
    public int secondsRemaining = Config.LEVEL_TIME_SECONDS;
    public int hostagesTotal = 0;
    public int hostagesRescued = 0;
    public double ninjutsuFlash = -1.0;

    @Override public Views.PlayerView player() { return player; }

    @Override public List<Views.EnemyView> enemies() {
        return new ArrayList<Views.EnemyView>(enemies);
    }
    @Override public List<Views.ProjectileView> projectiles() {
        return new ArrayList<Views.ProjectileView>(projectiles);
    }
    @Override public List<Views.HostageView> hostages() {
        return new ArrayList<Views.HostageView>(hostages);
    }
    @Override public List<Views.PlatformView> platforms() {
        return new ArrayList<Views.PlatformView>(platforms);
    }

    @Override public double cameraX() { return cameraX; }
    @Override public int levelWidth() { return levelWidth; }
    @Override public Enums.Phase phase() { return phase; }
    @Override public int secondsRemaining() { return secondsRemaining; }
    @Override public int hostagesRescued() { return hostagesRescued; }
    @Override public int hostagesTotal() { return hostagesTotal; }
    @Override public double ninjutsuFlash() { return ninjutsuFlash; }
}

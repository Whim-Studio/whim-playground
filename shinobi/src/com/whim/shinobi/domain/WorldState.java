package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

import java.util.ArrayList;
import java.util.List;

/**
 * The full mutable world the engine advances and hands to the UI. Aggregates the
 * player, live entity/hostage/platform lists, camera position, phase, timer, and
 * rescue tallies. Implements {@link Views.GameStateView}; the getters return the
 * live domain lists typed as the {@code *View} interfaces the UI reads.
 *
 * The engine (Task 2) owns all mutation on its tick thread; the UI only reads.
 */
public class WorldState implements Views.GameStateView {

    private Player player;

    private final List<Enemy> enemies = new ArrayList<Enemy>();
    private final List<Projectile> projectiles = new ArrayList<Projectile>();
    private final List<Hostage> hostages = new ArrayList<Hostage>();
    private final List<Platform> platforms = new ArrayList<Platform>();

    private final LevelMap map;

    private double cameraX = 0.0;
    private int levelWidth = Config.LEVEL_W;

    private Enums.Phase phase = Enums.Phase.PLAYING;
    private int secondsRemaining = Config.LEVEL_TIME_SECONDS;
    private int hostagesRescued = 0;
    private int hostagesTotal = 0;
    private double ninjutsuFlash = -1.0;

    public WorldState(LevelMap map) {
        this.map = map;
        this.levelWidth = map != null ? map.levelWidth() : Config.LEVEL_W;
    }

    // ---- Views.GameStateView ----
    @Override public Views.PlayerView player() { return player; }

    // These lists hold concrete domain types that implement the *View interfaces.
    // We build a correctly-typed List<*View> view over the live elements so the UI
    // sees a snapshot without ever casting to a concrete class.
    @Override
    public List<Views.EnemyView> enemies() {
        List<Views.EnemyView> out = new ArrayList<Views.EnemyView>(enemies.size());
        for (int i = 0; i < enemies.size(); i++) out.add(enemies.get(i));
        return out;
    }

    @Override
    public List<Views.ProjectileView> projectiles() {
        List<Views.ProjectileView> out = new ArrayList<Views.ProjectileView>(projectiles.size());
        for (int i = 0; i < projectiles.size(); i++) out.add(projectiles.get(i));
        return out;
    }

    @Override
    public List<Views.HostageView> hostages() {
        List<Views.HostageView> out = new ArrayList<Views.HostageView>(hostages.size());
        for (int i = 0; i < hostages.size(); i++) out.add(hostages.get(i));
        return out;
    }

    @Override
    public List<Views.PlatformView> platforms() {
        List<Views.PlatformView> out = new ArrayList<Views.PlatformView>(platforms.size());
        for (int i = 0; i < platforms.size(); i++) out.add(platforms.get(i));
        return out;
    }

    @Override public double cameraX() { return cameraX; }
    @Override public int levelWidth() { return levelWidth; }
    @Override public Enums.Phase phase() { return phase; }
    @Override public int secondsRemaining() { return secondsRemaining; }
    @Override public int hostagesRescued() { return hostagesRescued; }
    @Override public int hostagesTotal() { return hostagesTotal; }
    @Override public double ninjutsuFlash() { return ninjutsuFlash; }

    // ---- Live domain accessors (engine-side, concrete types) ----
    public Player playerEntity() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public List<Enemy> enemyList() { return enemies; }
    public List<Projectile> projectileList() { return projectiles; }
    public List<Hostage> hostageList() { return hostages; }
    public List<Platform> platformList() { return platforms; }
    public LevelMap map() { return map; }

    // ---- Mutators the engine drives ----
    public void setCameraX(double cameraX) { this.cameraX = cameraX; }
    public void setLevelWidth(int levelWidth) { this.levelWidth = levelWidth; }
    public void setPhase(Enums.Phase phase) { this.phase = phase; }
    public void setSecondsRemaining(int s) { this.secondsRemaining = s; }
    public void setHostagesRescued(int n) { this.hostagesRescued = n; }
    public void incHostagesRescued() { this.hostagesRescued++; }
    public void setHostagesTotal(int n) { this.hostagesTotal = n; }
    public void setNinjutsuFlash(double v) { this.ninjutsuFlash = v; }
}

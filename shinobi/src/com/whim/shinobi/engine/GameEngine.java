package com.whim.shinobi.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.GameController;
import com.whim.shinobi.api.Views;
import com.whim.shinobi.domain.Enemy;
import com.whim.shinobi.domain.Hostage;
import com.whim.shinobi.domain.LevelBuilder;
import com.whim.shinobi.domain.Player;
import com.whim.shinobi.domain.WorldState;

/**
 * The simulation engine — sole implementation of {@link GameController}. Owns a
 * {@link WorldState}, wires {@link Physics}/{@link CombatSystem}/{@link ThugAI}/
 * {@link NinjaAI}/{@link NinjutsuSystem}, and advances them on a background
 * {@link TickLoop} at {@link Config#TICK_HZ}, completely decoupled from Swing.
 *
 * Threading contract: input methods run on the Swing EDT and only record intent
 * via {@code volatile}/{@link AtomicBoolean} flags consumed once per tick. All
 * mutation happens on the tick thread under {@code stateLock}; {@link #state()}
 * takes the same lock and returns a per-frame snapshot whose entity lists are
 * fresh copies (crash-free iteration on the EDT).
 */
public final class GameEngine implements GameController {

    private final Object stateLock = new Object();
    private final Random rnd = new Random(1987L); // once-seeded for determinism (Shinobi, 1987)

    private WorldState world;
    private final TickLoop loop;

    // ---- Held input (EDT writes, tick reads) ----
    private volatile boolean leftHeld = false;
    private volatile boolean rightHeld = false;
    private volatile boolean crouchHeld = false;

    // ---- Discrete input intents (consumed once per tick) ----
    private final AtomicBoolean pendingJump = new AtomicBoolean(false);
    private final AtomicBoolean pendingShift = new AtomicBoolean(false);
    private final AtomicBoolean pendingAttack = new AtomicBoolean(false);
    private final AtomicBoolean pendingNinjutsu = new AtomicBoolean(false);

    private volatile boolean paused = false;
    private Enums.Phase prePausePhase = Enums.Phase.PLAYING;
    private int subTick = 0; // counts ticks toward one second

    public GameEngine() {
        this.loop = new TickLoop(new Runnable() {
            @Override public void run() { tick(); }
        });
    }

    // ================= GameController: lifecycle =================

    @Override public void newGame() {
        synchronized (stateLock) {
            world = LevelBuilder.firstLevel();
            world.cameraX = 0;
            world.phase = Enums.Phase.PLAYING;
            subTick = 0;
            paused = false;
            leftHeld = rightHeld = crouchHeld = false;
            pendingJump.set(false);
            pendingShift.set(false);
            pendingAttack.set(false);
            pendingNinjutsu.set(false);
            loop.setPaused(false);
        }
    }

    @Override public void start() {
        synchronized (stateLock) {
            if (world == null) {
                world = LevelBuilder.firstLevel();
            }
        }
        loop.start();
    }

    @Override public void stop() {
        loop.stop();
    }

    @Override public void togglePause() {
        synchronized (stateLock) {
            if (world == null) return;
            paused = !paused;
            if (paused) {
                prePausePhase = world.phase;
                world.phase = Enums.Phase.PAUSED;
            } else {
                world.phase = prePausePhase;
            }
            loop.setPaused(paused);
        }
    }

    // ================= GameController: input =================

    @Override public void setLeft(boolean held) { leftHeld = held; }
    @Override public void setRight(boolean held) { rightHeld = held; }
    @Override public void setCrouch(boolean held) { crouchHeld = held; }
    @Override public void jump() { pendingJump.set(true); }
    @Override public void shiftPlane() { pendingShift.set(true); }
    @Override public void attack() { pendingAttack.set(true); }
    @Override public void ninjutsu() { pendingNinjutsu.set(true); }

    // ================= Simulation =================

    /** One 60 Hz tick. Runs only on the {@link TickLoop} thread. */
    void tick() {
        synchronized (stateLock) {
            if (world == null) return;
            Enums.Phase phase = world.phase;
            if (phase == Enums.Phase.PAUSED
                    || phase == Enums.Phase.GAME_OVER
                    || phase == Enums.Phase.LEVEL_CLEAR) {
                // Frozen: drain discrete intents so they don't fire on resume.
                pendingJump.set(false);
                pendingShift.set(false);
                pendingAttack.set(false);
                pendingNinjutsu.set(false);
                return;
            }

            // Ninjutsu request can fire from normal play.
            if (pendingNinjutsu.getAndSet(false)) {
                NinjutsuSystem.trigger(world);
            }

            if (world.phase == Enums.Phase.NINJUTSU) {
                NinjutsuSystem.update(world);
                updateCamera();
                // Ignore other intents while the screen-clear plays out.
                pendingJump.set(false);
                pendingShift.set(false);
                pendingAttack.set(false);
                return;
            }

            stepPlayer();
            stepEnemies();
            CombatSystem.updateProjectiles(world);
            checkRescues();
            updateCamera();
            advanceTimer();
            checkEndConditions();
        }
    }

    private void stepPlayer() {
        Player p = world.player;
        if (!p.alive) return;

        double dir = 0;
        if (leftHeld) { dir -= 1; p.facing = Enums.Facing.LEFT; }
        if (rightHeld) { dir += 1; p.facing = Enums.Facing.RIGHT; }
        p.crouch = crouchHeld;
        // Crouching halts horizontal movement (classic guard/duck).
        Physics.setWalk(p, p.crouch ? 0 : dir);

        if (pendingShift.getAndSet(false)) {
            Physics.shiftPlane(p);
        }
        if (pendingJump.getAndSet(false) && !p.crouch) {
            Physics.jump(p);
        }
        if (pendingAttack.getAndSet(false)) {
            CombatSystem.playerAttack(world);
        }

        Physics.step(p, world.platforms, world.levelWidth);

        if (p.attackCooldown > 0) p.attackCooldown--;
        if (p.invuln > 0) p.invuln--;

        // Derive animation state (attack recovery wins for pose).
        if (p.attackCooldown > 0) {
            p.state = Enums.EntityState.ATTACK;
        } else if (!p.grounded) {
            p.state = Enums.EntityState.JUMP;
        } else if (p.crouch) {
            p.state = Enums.EntityState.BLOCK;
        } else if (dir != 0) {
            p.state = Enums.EntityState.WALK;
        } else {
            p.state = Enums.EntityState.IDLE;
        }
    }

    private void stepEnemies() {
        List<Enemy> enemies = world.enemies;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (!e.alive) continue;
            if (e.type == Enums.EnemyType.THUG) {
                ThugAI.update(e, world);
            } else {
                NinjaAI.update(e, world, rnd);
            }
            Physics.step(e, world.platforms, world.levelWidth);
            // Contact damage: touching the player costs a life.
            Player p = world.player;
            if (p.alive && p.invuln <= 0 && e.plane == p.plane && e.box.overlaps(p.box)) {
                CombatSystem.hitPlayer(world);
            }
        }
    }

    private void checkRescues() {
        Player p = world.player;
        List<Hostage> hs = world.hostages;
        for (int i = 0; i < hs.size(); i++) {
            Hostage h = hs.get(i);
            if (h.rescued || h.plane != p.plane) continue;
            if (p.box.overlaps(h.box)) {
                h.rescued = true;
                world.hostagesRescued++;
                applyReward(h.reward);
            }
        }
    }

    private void applyReward(Enums.RescueReward reward) {
        Player p = world.player;
        switch (reward) {
            case POINTS:
                p.score += 1000;
                break;
            case WEAPON_UPGRADE:
                p.weapon = p.weapon.upgrade();
                p.score += 500;
                break;
            case EXTRA_NINJUTSU:
                p.ninjutsu++;
                p.score += 500;
                break;
            default:
                p.score += 500;
                break;
        }
    }

    private void updateCamera() {
        double target = world.player.box.cx() - Config.VIEW_W / 2.0;
        double maxCam = Math.max(0, world.levelWidth - Config.VIEW_W);
        if (target < 0) target = 0;
        if (target > maxCam) target = maxCam;
        // Forward-biased follow (classic side-scroller: camera doesn't retreat).
        if (target > world.cameraX) {
            world.cameraX = target;
        } else if (world.cameraX > maxCam) {
            world.cameraX = maxCam;
        }
    }

    private void advanceTimer() {
        subTick++;
        if (subTick >= Config.TICK_HZ) {
            subTick = 0;
            if (world.secondsRemaining > 0) {
                world.secondsRemaining--;
            }
        }
    }

    private void checkEndConditions() {
        Player p = world.player;
        if (p.lives <= 0 || !p.alive) {
            world.phase = Enums.Phase.GAME_OVER;
            return;
        }
        if (world.secondsRemaining <= 0) {
            world.phase = Enums.Phase.GAME_OVER;
            return;
        }
        boolean allRescued = world.hostagesTotal > 0 && world.hostagesRescued >= world.hostagesTotal;
        boolean reachedEnd = p.box.right() >= world.levelWidth - 4;
        if (allRescued || reachedEnd) {
            world.phase = Enums.Phase.LEVEL_CLEAR;
        }
    }

    // ================= Snapshot =================

    /** Coherent per-frame snapshot; entity lists are fresh copies (EDT-safe). */
    @Override public Views.GameStateView state() {
        synchronized (stateLock) {
            if (world == null) {
                return EMPTY;
            }
            return new Snapshot(world);
        }
    }

    /** Empty snapshot before {@link #newGame()} is called. */
    private static final Views.GameStateView EMPTY = new Views.GameStateView() {
        private final List<Views.EnemyView> e = new ArrayList<Views.EnemyView>();
        private final List<Views.ProjectileView> pr = new ArrayList<Views.ProjectileView>();
        private final List<Views.HostageView> h = new ArrayList<Views.HostageView>();
        private final List<Views.PlatformView> pl = new ArrayList<Views.PlatformView>();
        @Override public Views.PlayerView player() { return null; }
        @Override public List<Views.EnemyView> enemies() { return e; }
        @Override public List<Views.ProjectileView> projectiles() { return pr; }
        @Override public List<Views.HostageView> hostages() { return h; }
        @Override public List<Views.PlatformView> platforms() { return pl; }
        @Override public double cameraX() { return 0; }
        @Override public int levelWidth() { return Config.LEVEL_W; }
        @Override public Enums.Phase phase() { return Enums.Phase.PLAYING; }
        @Override public int secondsRemaining() { return Config.LEVEL_TIME_SECONDS; }
        @Override public int hostagesRescued() { return 0; }
        @Override public int hostagesTotal() { return 0; }
        @Override public double ninjutsuFlash() { return -1.0; }
    };

    /**
     * Immutable-per-frame view. Scalars are captured by value; the five entity
     * lists are copied so EDT iteration can never hit a mid-tick mutation of the
     * backing collections. Element view-objects are the live domain instances
     * (read-only via the {@code *View} interfaces).
     */
    private static final class Snapshot implements Views.GameStateView {
        private final Views.PlayerView player;
        private final List<Views.EnemyView> enemies;
        private final List<Views.ProjectileView> projectiles;
        private final List<Views.HostageView> hostages;
        private final List<Views.PlatformView> platforms;
        private final double cameraX;
        private final int levelWidth;
        private final Enums.Phase phase;
        private final int secondsRemaining;
        private final int hostagesRescued;
        private final int hostagesTotal;
        private final double ninjutsuFlash;

        Snapshot(WorldState w) {
            this.player = w.player;
            this.enemies = new ArrayList<Views.EnemyView>(w.enemies);
            this.projectiles = new ArrayList<Views.ProjectileView>(w.projectiles);
            this.hostages = new ArrayList<Views.HostageView>(w.hostages);
            this.platforms = new ArrayList<Views.PlatformView>(w.platforms);
            this.cameraX = w.cameraX;
            this.levelWidth = w.levelWidth;
            this.phase = w.phase;
            this.secondsRemaining = w.secondsRemaining;
            this.hostagesRescued = w.hostagesRescued;
            this.hostagesTotal = w.hostagesTotal;
            this.ninjutsuFlash = w.ninjutsuFlash;
        }

        @Override public Views.PlayerView player() { return player; }
        @Override public List<Views.EnemyView> enemies() { return enemies; }
        @Override public List<Views.ProjectileView> projectiles() { return projectiles; }
        @Override public List<Views.HostageView> hostages() { return hostages; }
        @Override public List<Views.PlatformView> platforms() { return platforms; }
        @Override public double cameraX() { return cameraX; }
        @Override public int levelWidth() { return levelWidth; }
        @Override public Enums.Phase phase() { return phase; }
        @Override public int secondsRemaining() { return secondsRemaining; }
        @Override public int hostagesRescued() { return hostagesRescued; }
        @Override public int hostagesTotal() { return hostagesTotal; }
        @Override public double ninjutsuFlash() { return ninjutsuFlash; }
    }
}

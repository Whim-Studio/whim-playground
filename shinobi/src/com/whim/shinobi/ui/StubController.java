package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.GameController;
import com.whim.shinobi.api.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Development-only fake engine. Implements {@link GameController} with a tiny
 * self-contained simulation so the UI can render, scroll, HUD, and accept input
 * BEFORE the real engine (Task 2) or domain (Task 1) exist. It imports ONLY the
 * api package — never domain/engine.
 *
 * Threading mirrors the real contract: a background thread ticks at ~60 Hz mutating
 * mutable state; input methods just set volatile intent; {@link #state()} returns a
 * fresh immutable snapshot built each tick, safe to read on the EDT.
 */
public final class StubController implements GameController {

    // ---- input intent (set on EDT, read on tick thread) ----
    private volatile boolean left, right, crouch;

    // ---- player mutable sim state (tick thread only, published via snapshot) ----
    private double px, py, pvx, pvy;
    private boolean onGround = true;
    private Enums.Plane plane = Enums.Plane.LOWER;
    private Enums.Facing facing = Enums.Facing.RIGHT;
    private Enums.EntityState pstate = Enums.EntityState.IDLE;
    private Enums.AttackMode lastAttack = Enums.AttackMode.MELEE;
    private int attackTimer = 0;
    private int lives = Config.START_LIVES;
    private int score = 0;
    private int ninjutsu = Config.START_NINJUTSU;
    private Enums.Weapon weapon = Enums.Weapon.SHURIKEN;

    private double cameraX = 0;
    private int ticks = 0;
    private int hostagesRescued = 0;
    private double ninjutsuFlash = -1;
    private Enums.Phase phase = Enums.Phase.PLAYING;
    private boolean paused = false;

    private final List<Enemy> enemies = new ArrayList<Enemy>();
    private final List<Hostage> hostages = new ArrayList<Hostage>();
    private final List<Platform> platforms = new ArrayList<Platform>();
    private final List<Projectile> projectiles = new ArrayList<Projectile>();

    private volatile Views.GameStateView snapshot;
    private Thread tickThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // ================================================================= lifecycle

    @Override
    public void newGame() {
        synchronized (this) {
            px = 80; py = Config.GROUND_Y_LOWER - Config.ENTITY_H; pvx = pvy = 0;
            onGround = true; plane = Enums.Plane.LOWER; facing = Enums.Facing.RIGHT;
            pstate = Enums.EntityState.IDLE; attackTimer = 0;
            lives = Config.START_LIVES; score = 0; ninjutsu = Config.START_NINJUTSU;
            weapon = Enums.Weapon.SHURIKEN;
            cameraX = 0; ticks = 0; hostagesRescued = 0; ninjutsuFlash = -1;
            phase = Enums.Phase.PLAYING; paused = false;
            left = right = crouch = false;

            enemies.clear(); hostages.clear(); platforms.clear(); projectiles.clear();
            buildLevel();
            snapshot = buildSnapshot();
        }
    }

    private void buildLevel() {
        // Raised upper-plane ledges the player can leap onto (background path).
        platforms.add(new Platform(600, Config.GROUND_Y_UPPER, 360, 24, Enums.Plane.UPPER));
        platforms.add(new Platform(1300, Config.GROUND_Y_UPPER, 420, 24, Enums.Plane.UPPER));
        platforms.add(new Platform(2400, Config.GROUND_Y_UPPER, 500, 24, Enums.Plane.UPPER));
        platforms.add(new Platform(3300, Config.GROUND_Y_UPPER, 500, 24, Enums.Plane.UPPER));
        // A couple of lower-plane crates/steps.
        platforms.add(new Platform(950, Config.GROUND_Y_LOWER - 70, 90, 70, Enums.Plane.LOWER));
        platforms.add(new Platform(1850, Config.GROUND_Y_LOWER - 110, 120, 110, Enums.Plane.LOWER));

        // Hostages across both planes.
        hostages.add(new Hostage(520, Config.GROUND_Y_LOWER - 40, Enums.Plane.LOWER));
        hostages.add(new Hostage(760, Config.GROUND_Y_UPPER - 40, Enums.Plane.UPPER));
        hostages.add(new Hostage(1600, Config.GROUND_Y_LOWER - 40, Enums.Plane.LOWER));
        hostages.add(new Hostage(2600, Config.GROUND_Y_UPPER - 40, Enums.Plane.UPPER));
        hostages.add(new Hostage(3500, Config.GROUND_Y_LOWER - 40, Enums.Plane.LOWER));

        // Enemies: mix of THUG + NINJA patrolling.
        enemies.add(new Enemy(400, Enums.Plane.LOWER, Enums.EnemyType.THUG, 340, 520));
        enemies.add(new Enemy(880, Enums.Plane.UPPER, Enums.EnemyType.NINJA, 640, 940));
        enemies.add(new Enemy(1500, Enums.Plane.LOWER, Enums.EnemyType.NINJA, 1400, 1720));
        enemies.add(new Enemy(2000, Enums.Plane.LOWER, Enums.EnemyType.THUG, 1900, 2250));
        enemies.add(new Enemy(2650, Enums.Plane.UPPER, Enums.EnemyType.NINJA, 2450, 2880));
        enemies.add(new Enemy(3200, Enums.Plane.LOWER, Enums.EnemyType.THUG, 3050, 3400));
    }

    @Override
    public void start() {
        if (running.getAndSet(true)) return;
        if (snapshot == null) newGame();
        tickThread = new Thread(new Runnable() {
            @Override public void run() { loop(); }
        }, "stub-tick");
        tickThread.setDaemon(true);
        tickThread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (tickThread != null) tickThread.interrupt();
    }

    private void loop() {
        final long periodNs = 1_000_000_000L / Config.TICK_HZ;
        long next = System.nanoTime();
        while (running.get()) {
            synchronized (this) {
                if (!paused && phase == Enums.Phase.PLAYING) tick();
                snapshot = buildSnapshot();
            }
            next += periodNs;
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                try { Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L)); }
                catch (InterruptedException e) { return; }
            } else {
                next = System.nanoTime();
            }
        }
    }

    /** Package-private deterministic single-tick advance for headless testing. */
    synchronized void tickForTest() {
        if (!paused && phase == Enums.Phase.PLAYING) tick();
        snapshot = buildSnapshot();
    }

    // =================================================================== the sim

    private void tick() {
        ticks++;

        // ---- horizontal movement ----
        double speed = Config.MOVE_SPEED * (crouch && onGround ? 0.35 : 1.0);
        pvx = 0;
        if (left && !right) { pvx = -speed; facing = Enums.Facing.LEFT; }
        else if (right && !left) { pvx = speed; facing = Enums.Facing.RIGHT; }
        px += pvx;
        if (px < 0) px = 0;
        if (px > Config.LEVEL_W - Config.ENTITY_W) px = Config.LEVEL_W - Config.ENTITY_W;

        // ---- gravity / ground ----
        double groundFeet = (plane == Enums.Plane.UPPER)
                ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER;
        double groundTop = groundFeet - Config.ENTITY_H;
        pvy += Config.GRAVITY;
        py += pvy;
        if (py >= groundTop) { py = groundTop; pvy = 0; onGround = true; }
        else { onGround = false; }

        // ---- pose ----
        if (attackTimer > 0) { attackTimer--; pstate = Enums.EntityState.ATTACK; }
        else if (!onGround) pstate = Enums.EntityState.JUMP;
        else if (crouch) pstate = Enums.EntityState.IDLE;
        else if (pvx != 0) pstate = Enums.EntityState.WALK;
        else pstate = Enums.EntityState.IDLE;

        // ---- camera follows player, clamped ----
        double target = (px + Config.ENTITY_W / 2.0) - Config.VIEW_W / 2.0;
        double max = Config.LEVEL_W - Config.VIEW_W;
        cameraX = target < 0 ? 0 : (target > max ? max : target);

        // ---- enemies ----
        for (int i = 0; i < enemies.size(); i++) enemies.get(i).tick(ticks);

        // ---- projectiles ----
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile pr = projectiles.get(i);
            pr.x += pr.vx;
            if (pr.x < cameraX - 64 || pr.x > cameraX + Config.VIEW_W + 64) projectiles.remove(i);
        }

        // ---- hostage rescue by proximity (walk into them) ----
        for (int i = 0; i < hostages.size(); i++) {
            Hostage h = hostages.get(i);
            if (!h.rescued && h.plane == plane && Math.abs((h.x + 14) - (px + 14)) < 30) {
                h.rescued = true; hostagesRescued++; score += 1000;
            }
        }

        // ---- ninjutsu flash animation ----
        if (ninjutsuFlash >= 0) {
            ninjutsuFlash += 0.06;
            if (ninjutsuFlash >= 1.0) ninjutsuFlash = -1;
        }

        // idle score drip so the HUD visibly ticks
        if (ticks % 30 == 0) score += 10;
    }

    // =============================================================== input impl

    @Override public void setLeft(boolean held)  { left = held; }
    @Override public void setRight(boolean held) { right = held; }
    @Override public void setCrouch(boolean held){ crouch = held; }

    @Override
    public synchronized void jump() {
        if (phase != Enums.Phase.PLAYING || paused) return;
        if (onGround) { pvy = Config.JUMP_VELOCITY; onGround = false; }
    }

    @Override
    public synchronized void shiftPlane() {
        if (phase != Enums.Phase.PLAYING || paused) return;
        plane = (plane == Enums.Plane.LOWER) ? Enums.Plane.UPPER : Enums.Plane.LOWER;
        // land on the new plane's ground
        double feet = (plane == Enums.Plane.UPPER) ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER;
        py = feet - Config.ENTITY_H; pvy = 0; onGround = true;
    }

    @Override
    public synchronized void attack() {
        if (phase != Enums.Phase.PLAYING || paused) return;
        attackTimer = 12;
        // context-sensitive: melee if an enemy is close on our plane, else throw
        boolean near = false;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy en = enemies.get(i);
            if (en.alive && en.plane == plane && Math.abs((en.x + 14) - (px + 14)) <= Config.MELEE_RANGE) {
                near = true; break;
            }
        }
        if (near) {
            lastAttack = Enums.AttackMode.MELEE;
            for (int i = 0; i < enemies.size(); i++) {
                Enemy en = enemies.get(i);
                if (en.alive && en.plane == plane
                        && Math.abs((en.x + 14) - (px + 14)) <= Config.MELEE_RANGE) {
                    en.alive = false; score += 200;
                }
            }
        } else {
            lastAttack = Enums.AttackMode.PROJECTILE;
            double dir = (facing == Enums.Facing.RIGHT) ? 1 : -1;
            double vx = dir * weapon.projectileSpeed();
            projectiles.add(new Projectile(px + (dir > 0 ? Config.ENTITY_W : -8),
                    py + Config.ENTITY_H / 2.0 - 4, vx, plane, weapon, true));
        }
    }

    @Override
    public synchronized void ninjutsu() {
        if (phase != Enums.Phase.PLAYING || paused) return;
        if (ninjutsu <= 0) return;
        ninjutsu--;
        ninjutsuFlash = 0.0;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy en = enemies.get(i);
            if (en.alive) { en.alive = false; score += 100; }
        }
    }

    @Override
    public synchronized void togglePause() {
        paused = !paused;
        phase = paused ? Enums.Phase.PAUSED : Enums.Phase.PLAYING;
    }

    @Override
    public Views.GameStateView state() {
        Views.GameStateView s = snapshot;
        if (s == null) { synchronized (this) { if (snapshot == null) snapshot = buildSnapshot(); s = snapshot; } }
        return s;
    }

    // ============================================================ snapshot build

    private Views.GameStateView buildSnapshot() {
        final List<Views.EnemyView> es = new ArrayList<Views.EnemyView>();
        for (int i = 0; i < enemies.size(); i++) es.add(enemies.get(i).view());
        final List<Views.ProjectileView> ps = new ArrayList<Views.ProjectileView>();
        for (int i = 0; i < projectiles.size(); i++) ps.add(projectiles.get(i).view());
        final List<Views.HostageView> hs = new ArrayList<Views.HostageView>();
        for (int i = 0; i < hostages.size(); i++) hs.add(hostages.get(i).view());
        final List<Views.PlatformView> pl = new ArrayList<Views.PlatformView>();
        for (int i = 0; i < platforms.size(); i++) pl.add(platforms.get(i).view());

        final PlayerSnap player = new PlayerSnap(px, py, plane, facing, pstate,
                lives, score, weapon, ninjutsu, lastAttack);
        final double camX = cameraX;
        final Enums.Phase ph = phase;
        final int secs = Math.max(0, Config.LEVEL_TIME_SECONDS - ticks / Config.TICK_HZ);
        final int resc = hostagesRescued;
        final int total = hostages.size();
        final double flash = ninjutsuFlash;

        return new Views.GameStateView() {
            @Override public Views.PlayerView player() { return player; }
            @Override public List<Views.EnemyView> enemies() { return es; }
            @Override public List<Views.ProjectileView> projectiles() { return ps; }
            @Override public List<Views.HostageView> hostages() { return hs; }
            @Override public List<Views.PlatformView> platforms() { return pl; }
            @Override public double cameraX() { return camX; }
            @Override public int levelWidth() { return Config.LEVEL_W; }
            @Override public Enums.Phase phase() { return ph; }
            @Override public int secondsRemaining() { return secs; }
            @Override public int hostagesRescued() { return resc; }
            @Override public int hostagesTotal() { return total; }
            @Override public double ninjutsuFlash() { return flash; }
        };
    }

    // ============================================================ inner sim types

    private static final class Enemy {
        double x, lo, hi, vx = 0.8;
        Enums.Plane plane;
        Enums.EnemyType type;
        Enums.Facing facing = Enums.Facing.LEFT;
        boolean alive = true;
        boolean blocking = false;

        Enemy(double x, Enums.Plane plane, Enums.EnemyType type, double lo, double hi) {
            this.x = x; this.plane = plane; this.type = type; this.lo = lo; this.hi = hi;
        }

        void tick(int t) {
            if (!alive) return;
            x += vx;
            if (x < lo) { x = lo; vx = Math.abs(vx); }
            else if (x > hi) { x = hi; vx = -Math.abs(vx); }
            facing = vx >= 0 ? Enums.Facing.RIGHT : Enums.Facing.LEFT;
            // ninjas periodically raise a block pose
            blocking = type == Enums.EnemyType.NINJA && ((t / 40) % 3 == 0);
        }

        double feetY() { return plane == Enums.Plane.UPPER ? Config.GROUND_Y_UPPER : Config.GROUND_Y_LOWER; }

        Views.EnemyView view() {
            final double vx = x, vy = feetY() - Config.ENTITY_H;
            final Enums.Plane vp = plane; final Enums.Facing vf = facing;
            final Enums.EntityState vs = blocking ? Enums.EntityState.BLOCK
                    : (Math.abs(this.vx) > 0.01 ? Enums.EntityState.WALK : Enums.EntityState.IDLE);
            final boolean va = alive, vb = blocking; final Enums.EnemyType vt = type;
            return new Views.EnemyView() {
                @Override public double x() { return vx; }
                @Override public double y() { return vy; }
                @Override public double w() { return Config.ENTITY_W; }
                @Override public double h() { return Config.ENTITY_H; }
                @Override public Enums.Plane plane() { return vp; }
                @Override public Enums.Facing facing() { return vf; }
                @Override public Enums.EntityState state() { return vs; }
                @Override public boolean alive() { return va; }
                @Override public Enums.EnemyType type() { return vt; }
                @Override public boolean blocking() { return vb; }
            };
        }
    }

    private static final class Projectile {
        double x, y, vx;
        Enums.Plane plane; Enums.Weapon weapon; boolean fromPlayer;
        Projectile(double x, double y, double vx, Enums.Plane plane, Enums.Weapon weapon, boolean fromPlayer) {
            this.x = x; this.y = y; this.vx = vx; this.plane = plane; this.weapon = weapon; this.fromPlayer = fromPlayer;
        }
        Views.ProjectileView view() {
            final double vx = x, vy = y; final Enums.Plane vp = plane;
            final Enums.Weapon vw = weapon; final boolean vf = fromPlayer;
            final Enums.Facing dir = this.vx >= 0 ? Enums.Facing.RIGHT : Enums.Facing.LEFT;
            return new Views.ProjectileView() {
                @Override public double x() { return vx; }
                @Override public double y() { return vy; }
                @Override public double w() { return 10; }
                @Override public double h() { return 8; }
                @Override public Enums.Plane plane() { return vp; }
                @Override public Enums.Facing facing() { return dir; }
                @Override public Enums.EntityState state() { return Enums.EntityState.IDLE; }
                @Override public boolean alive() { return true; }
                @Override public boolean fromPlayer() { return vf; }
                @Override public Enums.Weapon weapon() { return vw; }
            };
        }
    }

    private static final class Hostage {
        double x, y; Enums.Plane plane; boolean rescued = false;
        Hostage(double x, double y, Enums.Plane plane) { this.x = x; this.y = y; this.plane = plane; }
        Views.HostageView view() {
            final double vx = x, vy = y; final Enums.Plane vp = plane; final boolean vr = rescued;
            return new Views.HostageView() {
                @Override public double x() { return vx; }
                @Override public double y() { return vy; }
                @Override public double w() { return 24; }
                @Override public double h() { return 40; }
                @Override public Enums.Plane plane() { return vp; }
                @Override public boolean rescued() { return vr; }
            };
        }
    }

    private static final class Platform {
        double x, y, w, h; Enums.Plane plane;
        Platform(double x, double y, double w, double h, Enums.Plane plane) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.plane = plane;
        }
        Views.PlatformView view() {
            final double vx = x, vy = y, vw = w, vh = h; final Enums.Plane vp = plane;
            return new Views.PlatformView() {
                @Override public double x() { return vx; }
                @Override public double y() { return vy; }
                @Override public double w() { return vw; }
                @Override public double h() { return vh; }
                @Override public Enums.Plane plane() { return vp; }
            };
        }
    }

    private static final class PlayerSnap implements Views.PlayerView {
        private final double x, y; private final Enums.Plane plane; private final Enums.Facing facing;
        private final Enums.EntityState state; private final int lives, score, ninjutsu;
        private final Enums.Weapon weapon; private final Enums.AttackMode lastAttack;
        PlayerSnap(double x, double y, Enums.Plane plane, Enums.Facing facing, Enums.EntityState state,
                   int lives, int score, Enums.Weapon weapon, int ninjutsu, Enums.AttackMode lastAttack) {
            this.x = x; this.y = y; this.plane = plane; this.facing = facing; this.state = state;
            this.lives = lives; this.score = score; this.weapon = weapon; this.ninjutsu = ninjutsu;
            this.lastAttack = lastAttack;
        }
        @Override public double x() { return x; }
        @Override public double y() { return y; }
        @Override public double w() { return Config.ENTITY_W; }
        @Override public double h() { return Config.ENTITY_H; }
        @Override public Enums.Plane plane() { return plane; }
        @Override public Enums.Facing facing() { return facing; }
        @Override public Enums.EntityState state() { return state; }
        @Override public boolean alive() { return true; }
        @Override public int lives() { return lives; }
        @Override public int score() { return score; }
        @Override public Enums.Weapon weapon() { return weapon; }
        @Override public int ninjutsu() { return ninjutsu; }
        @Override public Enums.AttackMode lastAttack() { return lastAttack; }
    }
}

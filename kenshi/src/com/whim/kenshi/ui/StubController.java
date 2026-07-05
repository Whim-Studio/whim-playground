package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dev-only fake {@link GameController} so the UI runs standalone before the real
 * engine lands. It hand-places a handful of characters (a PLAYER squad plus some
 * bandits and drifters), a couple of towns and a procedurally-tinted map, then
 * drifts everyone with a trivial wander on a background thread that honours pause
 * and game-speed. {@link #state()} returns immutable per-frame snapshots so the
 * EDT never observes a half-updated unit. NOT used by {@code Main}.
 */
public final class StubController implements GameController {

    private final Object lock = new Object();
    private final Random rng = new Random(20260705L);

    private final Enums.Terrain[][] terrain;
    private final List<Unit> units = new ArrayList<Unit>();
    private final List<Node> nodes = new ArrayList<Node>();
    private final Set<String> selected = new LinkedHashSet<String>();
    private final List<LogLine> log = new ArrayList<LogLine>();

    private volatile boolean paused = false;
    private volatile int speed = 1;
    private long tick = 0;
    private double worldSeconds = 0;

    private Thread thread;
    private volatile boolean running = false;

    public StubController() {
        this.terrain = generateTerrain(rng);
        newGame(20260705L);
    }

    // ---- lifecycle -------------------------------------------------------
    public void newGame(long seed) {
        synchronized (lock) {
            units.clear();
            nodes.clear();
            selected.clear();
            log.clear();
            tick = 0;
            worldSeconds = 0;

            double cx = Config.WORLD_SIZE * 0.5;
            double cy = Config.WORLD_SIZE * 0.5;

            nodes.add(new Node("n_hub", "Squin", Enums.NodeType.TOWN, Enums.FactionId.TRADE_GUILD, cx, cy, 90));
            nodes.add(new Node("n_bar", "The Last Drop", Enums.NodeType.BAR, Enums.FactionId.DRIFTERS, cx + 260, cy - 140, 46));
            nodes.add(new Node("n_camp", "Dust Camp", Enums.NodeType.CAMP, Enums.FactionId.DUST_BANDITS, cx - 300, cy + 200, 60));
            nodes.add(new Node("n_ruin", "Old Ruin", Enums.NodeType.RUIN, Enums.FactionId.DRIFTERS, cx + 180, cy + 240, 54));

            // Player squad (3 near the town).
            addUnit("p1", "Beep", Enums.FactionId.PLAYER, true, cx - 40, cy - 30, Enums.WeaponClass.ONE_HANDED);
            addUnit("p2", "Ruka", Enums.FactionId.PLAYER, true, cx, cy - 20, Enums.WeaponClass.TWO_HANDED);
            addUnit("p3", "Iyo", Enums.FactionId.PLAYER, true, cx + 40, cy - 30, Enums.WeaponClass.UNARMED);

            // A few bandits (hostile) and drifters (neutral).
            addUnit("b1", "Dust Raider", Enums.FactionId.DUST_BANDITS, false, cx - 240, cy + 150, Enums.WeaponClass.ONE_HANDED);
            addUnit("b2", "Dust Thug", Enums.FactionId.DUST_BANDITS, false, cx - 280, cy + 190, Enums.WeaponClass.TWO_HANDED);
            addUnit("h1", "Starving Bandit", Enums.FactionId.HUNGRY_BANDITS, false, cx + 210, cy + 120, Enums.WeaponClass.UNARMED);
            addUnit("d1", "Wandering Drifter", Enums.FactionId.DRIFTERS, false, cx + 150, cy - 120, Enums.WeaponClass.UNARMED);
            addUnit("g1", "Town Guard", Enums.FactionId.TRADE_GUILD, false, cx + 30, cy + 60, Enums.WeaponClass.ONE_HANDED);

            // Showcase the distinct states/HUD: one wounded, one downed, one dead.
            Unit wounded = byId("b1");
            if (wounded != null) {
                wounded.hp[Enums.BodyPart.LEFT_LEG.ordinal()] = -10; // disabled leg
                wounded.hp[Enums.BodyPart.RIGHT_ARM.ordinal()] = 22;
                wounded.bleedRate = 1.4;
            }
            Unit downed = byId("h1");
            if (downed != null) {
                downed.blood = 18; // below unconscious threshold
                downed.hp[Enums.BodyPart.STOMACH.ordinal()] = -4;
                downed.moveState = Enums.MoveState.DOWNED;
                downed.alive = true;
                downed.downed = true;
            }
            Unit dead = byId("b2");
            if (dead != null) {
                dead.hp[Enums.BodyPart.HEAD.ordinal()] = -100;
                dead.hp[Enums.BodyPart.CHEST.ordinal()] = -100;
                dead.moveState = Enums.MoveState.DEAD;
                dead.alive = false;
            }
            // Both legs down -> crawling demo.
            Unit crawler = byId("d1");
            if (crawler != null) {
                crawler.hp[Enums.BodyPart.LEFT_LEG.ordinal()] = -5;
                crawler.hp[Enums.BodyPart.RIGHT_LEG.ordinal()] = -5;
                crawler.moveState = Enums.MoveState.CRAWLING;
            }

            selected.add("p1");
            units.get(0).selected = true;
            pushLog("The squad arrives near Squin.");
            pushLog("Dust Bandits spotted to the southwest.");
        }
    }

    public void start() {
        synchronized (lock) {
            if (running) return;
            running = true;
            thread = new Thread(new Runnable() {
                public void run() { loop(); }
            }, "stub-tick");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void stop() {
        running = false;
        Thread t = thread;
        if (t != null) t.interrupt();
    }

    private void loop() {
        long periodMs = 1000L / Config.TICK_HZ;
        while (running) {
            try {
                Thread.sleep(periodMs);
            } catch (InterruptedException e) {
                return;
            }
            if (paused) continue;
            int sub = Math.max(1, speed);
            synchronized (lock) {
                for (int i = 0; i < sub; i++) stepOnce();
            }
        }
    }

    private void stepOnce() {
        tick++;
        worldSeconds += Config.WORLD_SECONDS_PER_TICK;
        double dtWorld = Config.WORLD_SECONDS_PER_TICK;
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.alive || u.moveState == Enums.MoveState.DEAD) continue;

            // trivial wander
            double dx = u.tx - u.x;
            double dy = u.ty - u.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 8 || u.tx == 0) {
                pickWander(u);
            } else {
                double sp = Config.BASE_MOVE_SPEED * dtWorld;
                if (u.moveState == Enums.MoveState.CRAWLING) sp *= Config.CRAWL_SPEED_MULT;
                if (u.downed) sp = 0;
                u.x += dx / dist * sp;
                u.y += dy / dist * sp;
                if (sp > 0) u.heading = Math.atan2(dy, dx);
            }
            // gentle hunger decay for flavour
            u.hunger = Math.max(0, u.hunger - Config.HUNGER_DECAY_PER_SEC * dtWorld);
        }
    }

    private void pickWander(Unit u) {
        double r = 140;
        u.tx = clampWorld(u.homeX + (rng.nextDouble() - 0.5) * 2 * r);
        u.ty = clampWorld(u.homeY + (rng.nextDouble() - 0.5) * 2 * r);
    }

    // ---- controls --------------------------------------------------------
    public void setPaused(boolean p) { this.paused = p; }
    public boolean isPaused() { return paused; }
    public void togglePause() { this.paused = !this.paused; }
    public void setGameSpeed(int m) { if (m > 0) this.speed = m; }

    public void setSelection(List<String> ids) {
        synchronized (lock) {
            selected.clear();
            if (ids != null) selected.addAll(ids);
            for (int i = 0; i < units.size(); i++) {
                units.get(i).selected = selected.contains(units.get(i).id);
            }
        }
    }

    public void orderMove(List<String> ids, double x, double y) {
        synchronized (lock) {
            for (int i = 0; i < units.size(); i++) {
                Unit u = units.get(i);
                if (ids != null && ids.contains(u.id)) {
                    u.tx = clampWorld(x);
                    u.ty = clampWorld(y);
                    u.orderType = Enums.OrderType.MOVE;
                    u.targetId = null;
                }
            }
            pushLog("Move order issued (" + (ids == null ? 0 : ids.size()) + " unit(s)).");
        }
    }

    public void orderAttack(List<String> ids, String targetId) {
        synchronized (lock) {
            Unit t = byId(targetId);
            for (int i = 0; i < units.size(); i++) {
                Unit u = units.get(i);
                if (ids != null && ids.contains(u.id)) {
                    u.orderType = Enums.OrderType.ATTACK;
                    u.targetId = targetId;
                    if (t != null) { u.tx = t.x; u.ty = t.y; }
                }
            }
            pushLog("Attack order on " + (t == null ? targetId : t.name) + ".");
        }
    }

    public void orderInteract(List<String> ids, String nodeId) {
        synchronized (lock) {
            Node n = nodeById(nodeId);
            for (int i = 0; i < units.size(); i++) {
                Unit u = units.get(i);
                if (ids != null && ids.contains(u.id)) {
                    u.orderType = Enums.OrderType.INTERACT;
                    u.targetId = null;
                    if (n != null) { u.tx = n.x; u.ty = n.y; }
                }
            }
            pushLog("Interact with " + (n == null ? nodeId : n.name) + ".");
        }
    }

    // ---- snapshot --------------------------------------------------------
    public Views.GameStateView state() {
        synchronized (lock) {
            List<Views.CharacterView> cs = new ArrayList<Views.CharacterView>();
            for (int i = 0; i < units.size(); i++) cs.add(units.get(i).snapshot());

            List<Views.NodeView> ns = new ArrayList<Views.NodeView>();
            for (int i = 0; i < nodes.size(); i++) ns.add(nodes.get(i));

            List<Views.SquadView> sq = new ArrayList<Views.SquadView>();
            List<String> playerIds = new ArrayList<String>();
            for (int i = 0; i < units.size(); i++) {
                if (units.get(i).playerControlled) playerIds.add(units.get(i).id);
            }
            sq.add(new SquadSnap("sq_player", "The Squad", Enums.FactionId.PLAYER, playerIds));

            List<Views.FactionView> fs = new ArrayList<Views.FactionView>();
            Enums.FactionId[] fids = Enums.FactionId.values();
            for (int i = 0; i < fids.length; i++) fs.add(new FactionSnap(fids[i], reputation(fids[i])));

            List<Views.LogView> lg = new ArrayList<Views.LogView>();
            for (int i = 0; i < log.size(); i++) lg.add(log.get(i));

            List<String> sel = new ArrayList<String>(selected);
            Enums.Phase phase = paused ? Enums.Phase.PAUSED : Enums.Phase.RUNNING;
            int gs = paused ? 0 : speed;
            return new StateSnap(phase, tick, worldSeconds, gs, new MapSnap(terrain),
                    cs, sq, fs, ns, sel, lg);
        }
    }

    // ---- helpers ---------------------------------------------------------
    private void addUnit(String id, String name, Enums.FactionId f, boolean player,
                         double x, double y, Enums.WeaponClass weapon) {
        Unit u = new Unit(id, name, f, player, x, y, weapon);
        units.add(u);
    }

    private Unit byId(String id) {
        for (int i = 0; i < units.size(); i++) if (units.get(i).id.equals(id)) return units.get(i);
        return null;
    }

    private Node nodeById(String id) {
        for (int i = 0; i < nodes.size(); i++) if (nodes.get(i).id.equals(id)) return nodes.get(i);
        return null;
    }

    private void pushLog(String text) {
        log.add(new LogLine(tick, text));
        while (log.size() > 40) log.remove(0);
    }

    private int reputation(Enums.FactionId f) {
        switch (f) {
            case PLAYER:         return 100;
            case TRADE_GUILD:    return 15;
            case DRIFTERS:       return 0;
            case HOLY_NATION:    return -10;
            case SHEK:           return -5;
            case DUST_BANDITS:   return -60;
            case HUNGRY_BANDITS: return -40;
            default:             return 0;
        }
    }

    private static Enums.Relation relation(Enums.FactionId a, Enums.FactionId b) {
        if (a == b) return Enums.Relation.ALLY;
        boolean aBandit = isBandit(a), bBandit = isBandit(b);
        if (aBandit && bBandit) return Enums.Relation.ALLY;
        if (aBandit || bBandit) {
            // bandits are hostile to non-bandits (player, guilds)
            return Enums.Relation.HOSTILE;
        }
        if ((a == Enums.FactionId.HOLY_NATION && b == Enums.FactionId.SHEK)
                || (a == Enums.FactionId.SHEK && b == Enums.FactionId.HOLY_NATION)) {
            return Enums.Relation.HOSTILE;
        }
        return Enums.Relation.NEUTRAL;
    }

    private static boolean isBandit(Enums.FactionId f) {
        return f == Enums.FactionId.DUST_BANDITS || f == Enums.FactionId.HUNGRY_BANDITS;
    }

    private static double clampWorld(double v) {
        if (v < 0) return 0;
        if (v > Config.WORLD_SIZE) return Config.WORLD_SIZE;
        return v;
    }

    // ---- procedural terrain ---------------------------------------------
    private static Enums.Terrain[][] generateTerrain(Random rng) {
        int n = Config.MAP_TILES;
        Enums.Terrain[][] t = new Enums.Terrain[n][n];
        // Value-noise-ish blobs: sum a few sine fields for smoothly varying biome.
        double s1 = rng.nextDouble() * 10, s2 = rng.nextDouble() * 10, s3 = rng.nextDouble() * 10;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                double v = Math.sin(c * 0.10 + s1) + Math.cos(r * 0.12 + s2)
                        + Math.sin((c + r) * 0.07 + s3);
                v = (v + 3) / 6.0; // 0..1
                Enums.Terrain tt;
                if (v < 0.16)      tt = Enums.Terrain.WATER;
                else if (v < 0.30) tt = Enums.Terrain.ASH;
                else if (v < 0.45) tt = Enums.Terrain.ROCK;
                else if (v < 0.62) tt = Enums.Terrain.SAND;
                else if (v < 0.78) tt = Enums.Terrain.SCRUB;
                else               tt = Enums.Terrain.GREEN;
                t[r][c] = tt;
            }
        }
        // A town patch near the map centre.
        int mid = n / 2;
        for (int r = mid - 2; r <= mid + 2; r++) {
            for (int c = mid - 2; c <= mid + 2; c++) {
                if (r >= 0 && r < n && c >= 0 && c < n) t[r][c] = Enums.Terrain.TOWN;
            }
        }
        return t;
    }

    // ===== mutable working character =====================================
    private final class Unit {
        final String id;
        final String name;
        final Enums.FactionId faction;
        final boolean playerControlled;
        final Enums.WeaponClass weapon;
        final double homeX, homeY;
        double x, y, heading;
        double tx, ty;
        Enums.MoveState moveState = Enums.MoveState.IDLE;
        Enums.AiState aiState = Enums.AiState.WANDER;
        boolean selected, alive = true, downed = false;
        double hunger = Config.HUNGER_MAX * 0.7;
        double blood = Config.BLOOD_MAX;
        double bleedRate = 0;
        Enums.OrderType orderType = Enums.OrderType.NONE;
        String targetId = null;
        final double[] hp = new double[Enums.BodyPart.values().length];
        final double[] max = new double[Enums.BodyPart.values().length];
        final int[] skills = new int[Enums.SkillType.values().length];

        Unit(String id, String name, Enums.FactionId f, boolean player,
             double x, double y, Enums.WeaponClass weapon) {
            this.id = id; this.name = name; this.faction = f; this.playerControlled = player;
            this.x = x; this.y = y; this.homeX = x; this.homeY = y;
            this.weapon = weapon;
            this.heading = rng.nextDouble() * Math.PI * 2;
            Enums.BodyPart[] parts = Enums.BodyPart.values();
            for (int i = 0; i < parts.length; i++) {
                double m = parts[i].vital() ? Config.TORSO_PART_MAX : Config.LIMB_PART_MAX;
                max[i] = m; hp[i] = m;
            }
            Enums.SkillType[] st = Enums.SkillType.values();
            for (int i = 0; i < st.length; i++) skills[i] = 10 + rng.nextInt(40);
            this.moveState = Enums.MoveState.MOVING;
        }

        CharSnap snapshot() {
            return new CharSnap(this);
        }
    }

    // ===== immutable per-frame snapshots =================================
    private static final class CharSnap implements Views.CharacterView {
        final String id, name, targetId;
        final Enums.FactionId faction;
        final double x, y, heading, hunger, blood, bleedRate;
        final Enums.MoveState moveState;
        final Enums.AiState aiState;
        final boolean selected, playerControlled, alive, downed;
        final Enums.WeaponClass weapon;
        final Enums.OrderType orderType;
        final double[] hp, max;
        final int[] skills;

        CharSnap(Unit u) {
            this.id = u.id; this.name = u.name; this.faction = u.faction;
            this.x = u.x; this.y = u.y; this.heading = u.heading;
            this.moveState = u.moveState; this.aiState = u.aiState;
            this.selected = u.selected; this.playerControlled = u.playerControlled;
            this.alive = u.alive; this.downed = u.downed;
            this.hunger = u.hunger; this.blood = u.blood; this.bleedRate = u.bleedRate;
            this.weapon = u.weapon; this.orderType = u.orderType;
            this.targetId = u.targetId;
            this.hp = Arrays.copyOf(u.hp, u.hp.length);
            this.max = Arrays.copyOf(u.max, u.max.length);
            this.skills = Arrays.copyOf(u.skills, u.skills.length);
        }

        public String id() { return id; }
        public String name() { return name; }
        public Enums.FactionId faction() { return faction; }
        public double x() { return x; }
        public double y() { return y; }
        public double heading() { return heading; }
        public Enums.MoveState moveState() { return moveState; }
        public Enums.AiState aiState() { return aiState; }
        public boolean selected() { return selected; }
        public boolean playerControlled() { return playerControlled; }
        public boolean alive() { return alive; }
        public boolean downed() { return downed; }
        public double partHp(Enums.BodyPart p) { return hp[p.ordinal()]; }
        public double partMax(Enums.BodyPart p) { return max[p.ordinal()]; }
        public boolean partDisabled(Enums.BodyPart p) { return hp[p.ordinal()] <= 0; }
        public double hunger() { return hunger; }
        public double hungerMax() { return Config.HUNGER_MAX; }
        public double blood() { return blood; }
        public double bloodMax() { return Config.BLOOD_MAX; }
        public double bleedRate() { return bleedRate; }
        public Enums.WeaponClass weapon() { return weapon; }
        public int skill(Enums.SkillType s) { return skills[s.ordinal()]; }
        public Enums.OrderType orderType() { return orderType; }
        public String targetId() { return targetId; }
    }

    private static final class Node implements Views.NodeView {
        final String id, name;
        final Enums.NodeType type;
        final Enums.FactionId owner;
        final double x, y, radius;
        Node(String id, String name, Enums.NodeType type, Enums.FactionId owner,
             double x, double y, double radius) {
            this.id = id; this.name = name; this.type = type; this.owner = owner;
            this.x = x; this.y = y; this.radius = radius;
        }
        public String id() { return id; }
        public String name() { return name; }
        public Enums.NodeType type() { return type; }
        public Enums.FactionId owner() { return owner; }
        public double x() { return x; }
        public double y() { return y; }
        public double radius() { return radius; }
    }

    private static final class SquadSnap implements Views.SquadView {
        final String id, name;
        final Enums.FactionId faction;
        final List<String> members;
        SquadSnap(String id, String name, Enums.FactionId f, List<String> m) {
            this.id = id; this.name = name; this.faction = f;
            this.members = Collections.unmodifiableList(new ArrayList<String>(m));
        }
        public String id() { return id; }
        public String name() { return name; }
        public Enums.FactionId faction() { return faction; }
        public List<String> memberIds() { return members; }
    }

    private static final class FactionSnap implements Views.FactionView {
        final Enums.FactionId id;
        final int rep;
        FactionSnap(Enums.FactionId id, int rep) { this.id = id; this.rep = rep; }
        public Enums.FactionId id() { return id; }
        public String label() { return id.label(); }
        public Enums.Relation relationTo(Enums.FactionId other) { return relation(id, other); }
        public int reputationWithPlayer() { return rep; }
    }

    private static final class MapSnap implements Views.MapView {
        final Enums.Terrain[][] t;
        MapSnap(Enums.Terrain[][] t) { this.t = t; }
        public int tiles() { return Config.MAP_TILES; }
        public double tileSize() { return Config.TILE_SIZE; }
        public Enums.Terrain terrain(int col, int row) {
            if (row < 0 || row >= t.length || col < 0 || col >= t.length) return Enums.Terrain.SAND;
            return t[row][col];
        }
    }

    private static final class LogLine implements Views.LogView {
        final long tick; final String text;
        LogLine(long tick, String text) { this.tick = tick; this.text = text; }
        public long tick() { return tick; }
        public String text() { return text; }
    }

    private static final class StateSnap implements Views.GameStateView {
        final Enums.Phase phase; final long tick; final double worldSeconds; final int speed;
        final Views.MapView map;
        final List<Views.CharacterView> chars;
        final List<Views.SquadView> squads;
        final List<Views.FactionView> factions;
        final List<Views.NodeView> nodes;
        final List<String> selected;
        final List<Views.LogView> log;
        StateSnap(Enums.Phase phase, long tick, double worldSeconds, int speed, Views.MapView map,
                  List<Views.CharacterView> chars, List<Views.SquadView> squads,
                  List<Views.FactionView> factions, List<Views.NodeView> nodes,
                  List<String> selected, List<Views.LogView> log) {
            this.phase = phase; this.tick = tick; this.worldSeconds = worldSeconds; this.speed = speed;
            this.map = map; this.chars = chars; this.squads = squads; this.factions = factions;
            this.nodes = nodes; this.selected = selected; this.log = log;
        }
        public Enums.Phase phase() { return phase; }
        public long tick() { return tick; }
        public double worldSeconds() { return worldSeconds; }
        public int gameSpeed() { return speed; }
        public Views.MapView map() { return map; }
        public List<Views.CharacterView> characters() { return chars; }
        public List<Views.SquadView> squads() { return squads; }
        public List<Views.FactionView> factions() { return factions; }
        public List<Views.NodeView> nodes() { return nodes; }
        public List<String> selectedIds() { return selected; }
        public List<Views.LogView> log() { return log; }
    }

    // Unit uses an order type field; declare default here to keep snapshot simple.
    // (kept last to avoid clutter above)
}

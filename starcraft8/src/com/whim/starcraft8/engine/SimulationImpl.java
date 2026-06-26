package com.whim.starcraft8.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.whim.starcraft8.domain.ArmorClass;
import com.whim.starcraft8.domain.AttackKind;
import com.whim.starcraft8.domain.BuildState;
import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.DamageType;
import com.whim.starcraft8.domain.GameMap;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Projectile;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.ResourceType;
import com.whim.starcraft8.domain.Terrain;
import com.whim.starcraft8.domain.Unit;
import com.whim.starcraft8.domain.UnitState;
import com.whim.starcraft8.domain.UnitType;

/**
 * The whole simulation: 60-tps background thread, command queue, economy, production,
 * supply accounting, combat, pathfinding, win/lose, and the AI driver. No swing/awt
 * rendering — {@code java.awt.Color} flows through only as data on projectiles.
 */
final class SimulationImpl implements Simulation, WorldReader {

    // Engine-owned economy/combat constants (kept local so the engine never depends on
    // data.Balance constant NAMES that aren't frozen in the contract).
    static final int TICKS_PER_SECOND = 60;
    static final int WORKER_CARRY = 8;
    static final int GATHER_TICKS = 40;
    static final int SUPPLY_MAX = 200;
    static final int SHIELD_REGEN_PERIOD = 32;   // +1 shield every N ticks out of combat
    static final int OUT_OF_COMBAT_TICKS = 96;   // ticks since last combat to count as "safe"
    static final int AI_THINK_PERIOD = 24;       // AI re-evaluates ~2.5x/sec

    private final GameState state;
    private final int humanPlayerId;
    private final Object lock = new Object();
    private final java.util.Queue<Command> queue = new ConcurrentLinkedQueue<Command>();

    private final Map<Long, Order> orders = new HashMap<Long, Order>();
    private final Map<Long, Long> lastCombatTick = new HashMap<Long, Long>();
    private final List<AiController> ais = new ArrayList<AiController>();

    private volatile boolean running = false;
    private Thread thread;
    private long tickCount = 0;

    SimulationImpl(GameState state, int humanPlayerId) {
        this.state = state;
        this.humanPlayerId = humanPlayerId;
        for (Player p : state.players()) {
            if (p.id() != humanPlayerId) {
                ais.add(new AiController(this, p.id()));
            }
        }
    }

    // ------------------------------------------------------------------ public API

    public void start() {
        synchronized (lock) {
            if (running) return;
            running = true;
        }
        thread = new Thread(new Runnable() {
            public void run() { loop(); }
        }, "starcraft8-sim");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() { running = false; }

    public void enqueue(Command c) { if (c != null) queue.add(c); }

    public void readState(Consumer<WorldReader> reader) {
        synchronized (lock) { reader.accept(this); }
    }

    public boolean isRunning() { return running; }

    public int humanPlayerId() { return humanPlayerId; }

    public GameState state() { return state; }

    private void loop() {
        final long nanosPerTick = 1000000000L / TICKS_PER_SECOND;
        long next = System.nanoTime();
        while (running) {
            tickOnce();
            next += nanosPerTick;
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                try { Thread.sleep(sleep / 1000000L, (int) (sleep % 1000000L)); }
                catch (InterruptedException e) { break; }
            } else {
                next = System.nanoTime(); // fell behind; resync rather than spiral
            }
        }
    }

    /** One simulation step. Package-private so the headless smoke test can pump it. */
    void tickOnce() {
        synchronized (lock) {
            if (state.winnerId() != -1) { state.setTick(tickCount); return; }
            drainCommands();
            for (AiController ai : ais) {
                if ((tickCount % AI_THINK_PERIOD) == ai.phase()) ai.think(tickCount);
            }
            updateBuildings();
            updateUnits();
            updateProjectiles();
            regenShields();
            cleanupDead();
            recomputeSupply();
            checkWinLose();
            tickCount++;
            state.setTick(tickCount);
        }
    }

    private void drainCommands() {
        Command c;
        while ((c = queue.poll()) != null) {
            if (c instanceof Commands.Base) ((Commands.Base) c).exec(this);
        }
    }

    // ------------------------------------------------------------------ command sinks

    void cmdMove(List<Long> ids, double tx, double ty, boolean attack) {
        for (Long id : ids) {
            Unit u = unit(id);
            if (u == null || !u.alive()) continue;
            Order o = order(u.id());
            o.reset();
            o.kind = attack ? Order.ATTACK_MOVE : Order.MOVE;
            o.tx = tx; o.ty = ty;
            u.setTarget(tx, ty);
            u.setTargetEntityId(-1);
            u.setState(UnitState.MOVING);
        }
    }

    void cmdAttackTarget(List<Long> ids, long targetId) {
        for (Long id : ids) {
            Unit u = unit(id);
            if (u == null || !u.alive()) continue;
            Order o = order(u.id());
            o.reset();
            o.kind = Order.ATTACK;
            o.targetId = targetId;
            u.setTargetEntityId(targetId);
            u.setState(UnitState.ATTACKING);
        }
    }

    void cmdGather(List<Long> ids, long resId) {
        for (Long id : ids) {
            Unit u = unit(id);
            if (u == null || !u.alive() || !u.type().isWorker()) continue;
            assignGather(u, resId);
        }
    }

    void cmdBuild(long workerId, BuildingType type, int tx, int ty) {
        Unit w = unit(workerId);
        if (w == null || !w.alive() || !w.type().isWorker()) return;
        doBuild(w.ownerId(), w, type, tx, ty);
    }

    void cmdTrain(long buildingId, UnitType type) {
        Building b = building(buildingId);
        if (b == null) return;
        doTrain(b, type);
    }

    void cmdRally(long buildingId, double tx, double ty) {
        Building b = building(buildingId);
        if (b != null) b.setRally(tx, ty);
    }

    void cmdStop(List<Long> ids) {
        for (Long id : ids) {
            Unit u = unit(id);
            if (u == null || !u.alive()) continue;
            Order o = order(u.id());
            o.reset();
            o.kind = Order.NONE;
            u.setTargetEntityId(-1);
            u.setCarried(ResourceType.MINERALS, 0);
            u.setState(UnitState.IDLE);
        }
    }

    // ------------------------------------------------------------------ economy: build

    /** Validate + deduct + place a building. Returns true on success. */
    boolean doBuild(int ownerId, Unit worker, BuildingType type, int tx, int ty) {
        Player p = state.player(ownerId);
        if (p == null || type == null) return false;
        if (type.race() != p.race()) return false;
        if (!prereqsMet(ownerId, type)) return false;
        if (p.minerals() < type.mineralCost() || p.gas() < type.gasCost()) return false;
        if (!canPlace(type, tx, ty)) return false;

        p.addMinerals(-type.mineralCost());
        p.addGas(-type.gasCost());

        Building b = new Building(type, ownerId, tx, ty);
        b.setState(BuildState.UNDER_CONSTRUCTION);
        b.setBuildProgress(0);
        b.setHp(Math.max(1, type.maxHp() / 10));
        b.setRally(tx + type.widthTiles() / 2.0, ty + type.heightTiles() + 0.5);
        state.buildings().add(b);

        // Zerg structures morph from the drone (consumed); others free the worker.
        if (worker != null) {
            if (p.race() == Race.ZERG) {
                worker.setHp(0);
                worker.setState(UnitState.DEAD);
            } else {
                Order o = order(worker.id());
                o.reset();
                o.kind = Order.NONE;
                worker.setState(UnitState.IDLE);
            }
        }
        return true;
    }

    /** Gas structures sit on a geyser; everything else needs clear buildable ground. */
    boolean canPlace(BuildingType type, int tx, int ty) {
        GameMap map = state.map();
        int w = type.widthTiles(), h = type.heightTiles();
        if (!map.inBounds(tx, ty) || !map.inBounds(tx + w - 1, ty + h - 1)) return false;
        if (type.isGas()) {
            if (map.terrainAt(tx, ty) != Terrain.GEYSER) return false;
        } else {
            for (int yy = ty; yy < ty + h; yy++) {
                for (int xx = tx; xx < tx + w; xx++) {
                    if (map.terrainAt(xx, yy) != Terrain.GROUND) return false;
                }
            }
        }
        for (int yy = ty; yy < ty + h; yy++) {
            for (int xx = tx; xx < tx + w; xx++) {
                if (buildingCovering(xx, yy) != null) return false;
            }
        }
        return true;
    }

    boolean prereqsMet(int ownerId, BuildingType type) {
        List<BuildingType> reqs = com.whim.starcraft8.data.TechTree.prerequisites(type);
        if (reqs == null || reqs.isEmpty()) return true;
        for (BuildingType req : reqs) {
            boolean have = false;
            for (Building b : state.buildings()) {
                if (b.ownerId() == ownerId && b.type() == req
                        && b.alive() && b.state() != BuildState.UNDER_CONSTRUCTION) {
                    have = true; break;
                }
            }
            if (!have) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ economy: train

    boolean doTrain(Building b, UnitType type) {
        if (b == null || type == null || !b.alive()) return false;
        if (b.state() == BuildState.UNDER_CONSTRUCTION) return false;
        if (!com.whim.starcraft8.data.TechTree.producedBy(b.type()).contains(type)) return false;
        Player p = state.player(b.ownerId());
        if (p == null) return false;
        if (p.minerals() < type.mineralCost() || p.gas() < type.gasCost()) return false;

        int need = type.supplyCost() * spawnCount(type);
        if (type.supplyProvided() == 0 && p.supplyUsed() + need > p.supplyCap()) return false;

        p.addMinerals(-type.mineralCost());
        p.addGas(-type.gasCost());
        b.productionQueue().addLast(type);
        return true;
    }

    /** Zerglings hatch two per order; everything else one. */
    static int spawnCount(UnitType t) {
        if (t.race() == Race.ZERG && !t.isWorker() && t.attackKind() == AttackKind.MELEE) return 2;
        return 1;
    }

    // ------------------------------------------------------------------ buildings tick

    private void updateBuildings() {
        for (Building b : state.buildings()) {
            if (!b.alive()) continue;
            if (b.state() == BuildState.UNDER_CONSTRUCTION) {
                int bp = b.buildProgress() + 1;
                int total = Math.max(1, b.type().buildTicks());
                if (bp >= total) {
                    b.setBuildProgress(total);
                    b.setHp(b.type().maxHp());
                    b.setState(BuildState.COMPLETE);
                } else {
                    b.setBuildProgress(bp);
                    int hp = (int) ((long) b.type().maxHp() * bp / total);
                    b.setHp(Math.max(1, hp));
                }
                continue;
            }
            // production
            if (b.productionTicksLeft() > 0) {
                b.setProductionTicksLeft(b.productionTicksLeft() - 1);
                if (b.productionTicksLeft() == 0) {
                    UnitType t = b.productionQueue().pollFirst();
                    if (t != null) spawnTrained(b, t);
                    b.setState(b.productionQueue().isEmpty() ? BuildState.COMPLETE : BuildState.PRODUCING);
                }
            } else if (!b.productionQueue().isEmpty()) {
                b.setProductionTicksLeft(Math.max(1, b.productionQueue().peekFirst().buildTicks()));
                b.setState(BuildState.PRODUCING);
            }
        }
    }

    private void spawnTrained(Building b, UnitType t) {
        int n = spawnCount(t);
        for (int i = 0; i < n; i++) {
            double ox = b.tileX() + b.type().widthTiles() / 2.0 + (i == 0 ? -0.4 : 0.4);
            double oy = b.tileY() + b.type().heightTiles() + 0.6;
            ox = clamp(ox, 0, state.map().width() - 0.01);
            oy = clamp(oy, 0, state.map().height() - 0.01);
            Unit u = new Unit(t, b.ownerId(), ox, oy);
            state.units().add(u);
            Order o = order(u.id());
            if (t.isWorker()) {
                assignGather(u, -1);        // new workers auto-mine
            } else if (b.rallyX() != 0 || b.rallyY() != 0) {
                o.reset();
                o.kind = Order.ATTACK_MOVE;
                o.tx = b.rallyX(); o.ty = b.rallyY();
                u.setTarget(b.rallyX(), b.rallyY());
                u.setState(UnitState.MOVING);
            }
        }
    }

    // ------------------------------------------------------------------ units tick

    private void updateUnits() {
        // snapshot list because combat/economy do not add units mid-iteration
        List<Unit> units = state.units();
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.alive()) continue;
            if (u.cooldownLeft() > 0) u.setCooldownLeft(u.cooldownLeft() - 1);
            Order o = order(u.id());

            if (u.type().isWorker() && (o.kind == Order.GATHER)) {
                updateGather(u, o);
                continue;
            }
            switch (o.kind) {
                case Order.ATTACK:
                    updateAttackOrder(u, o); break;
                case Order.ATTACK_MOVE:
                    updateAttackMove(u, o); break;
                case Order.MOVE:
                    updateMove(u, o); break;
                default:
                    updateIdle(u, o); break;
            }
        }
    }

    private void updateMove(Unit u, Order o) {
        if (u.distanceTo(o.tx, o.ty) <= 0.15) {
            o.kind = Order.NONE;
            u.setState(UnitState.IDLE);
            return;
        }
        u.setState(UnitState.MOVING);
        stepToward(u, o.tx, o.ty);
    }

    private void updateIdle(Unit u, Order o) {
        // combat units defend themselves when idle
        if (u.type().attackKind() != AttackKind.NONE && !u.type().isWorker()) {
            long tid = acquireTarget(u, u.type().sight());
            if (tid != -1) { engage(u, tid, false); return; }
        }
        u.setState(UnitState.IDLE);
    }

    private void updateAttackOrder(Unit u, Order o) {
        if (!entityAlive(o.targetId)) {
            o.kind = Order.NONE;
            u.setTargetEntityId(-1);
            u.setState(UnitState.IDLE);
            return;
        }
        engage(u, o.targetId, true);
    }

    private void updateAttackMove(Unit u, Order o) {
        long tid = acquireTarget(u, u.type().sight());
        if (tid != -1) { engage(u, tid, false); return; }
        if (u.distanceTo(o.tx, o.ty) <= 0.4) {
            o.kind = Order.NONE;
            u.setState(UnitState.IDLE);
            return;
        }
        u.setState(UnitState.MOVING);
        stepToward(u, o.tx, o.ty);
    }

    /** Move into range of target id and fire on cooldown. */
    private void engage(Unit u, long targetId, boolean chase) {
        double[] tp = aimPoint(u, targetId);
        if (tp == null) { u.setState(UnitState.IDLE); return; }
        u.setTargetEntityId(targetId);
        double dist = u.distanceTo(tp[0], tp[1]);
        double range = Math.max(0.6, u.type().range());
        if (dist > range) {
            u.setState(UnitState.MOVING);
            stepToward(u, tp[0], tp[1]);
            return;
        }
        u.setState(UnitState.ATTACKING);
        if (u.cooldownLeft() <= 0 && u.type().attackKind() != AttackKind.NONE) {
            fire(u, targetId, tp[0], tp[1]);
            u.setCooldownLeft(u.type().cooldown());
        }
    }

    private void fire(Unit u, long targetId, double tx, double ty) {
        lastCombatTick.put(u.id(), tickCount);
        if (u.type().attackKind() == AttackKind.RANGED) {
            Projectile p = new Projectile(u.x(), u.y(), targetId, u.type().damage(),
                    u.type().damageType(), u.type().splashRadius(), 0.35, u.type().baseColor());
            state.projectiles().add(p);
        } else { // MELEE: instant
            applyDamageAt(targetId, tx, ty, u.type().damage(), u.type().damageType(),
                    u.type().splashRadius());
        }
    }

    // ------------------------------------------------------------------ economy: gather

    void assignGather(Unit w, long resId) {
        int[] tile = resolveResourceTile(w, resId);
        Order o = order(w.id());
        o.reset();
        if (tile == null) { o.kind = Order.NONE; w.setState(UnitState.IDLE); return; }
        o.kind = Order.GATHER;
        o.resX = tile[0]; o.resY = tile[1];
        o.gatherStage = (w.carriedResource() > 0) ? 2 : 0;
        w.setState(UnitState.GATHERING);
    }

    private int[] resolveResourceTile(Unit w, long resId) {
        GameMap map = state.map();
        if (resId <= -2) { // packed tile id = -(ty*width+tx)-2
            int idx = (int) (-resId - 2);
            int tx = idx % map.width(), ty = idx / map.width();
            if (map.inBounds(tx, ty)) return new int[]{tx, ty};
            return null;
        }
        if (resId >= 0) { // a gas building id -> gather gas from its geyser
            Building b = building(resId);
            if (b != null && b.type().isGas()) return new int[]{b.tileX(), b.tileY()};
        }
        return nearestMineralTile(w.x(), w.y()); // -1 or anything else: nearest mineral patch
    }

    private int[] nearestMineralTile(double x, double y) {
        GameMap map = state.map();
        int best = -1, bx = -1, by = -1;
        double bd = Double.MAX_VALUE;
        for (int ty = 0; ty < map.height(); ty++) {
            for (int tx = 0; tx < map.width(); tx++) {
                if (map.terrainAt(tx, ty) == Terrain.MINERAL_FIELD && map.resourceAt(tx, ty) > 0) {
                    double dx = tx + 0.5 - x, dy = ty + 0.5 - y;
                    double d = dx * dx + dy * dy;
                    if (d < bd) { bd = d; bx = tx; by = ty; best = 1; }
                }
            }
        }
        return best == -1 ? null : new int[]{bx, by};
    }

    private void updateGather(Unit w, Order o) {
        GameMap map = state.map();
        int rx = o.resX, ry = o.resY;
        boolean gas = map.terrainAt(rx, ry) == Terrain.GEYSER;
        ResourceType rt = gas ? ResourceType.GAS : ResourceType.MINERALS;

        if (o.gatherStage == 0) { // travel to field
            boolean usable = map.resourceAt(rx, ry) > 0 && (!gas || hasGasBuilding(w.ownerId(), rx, ry));
            if (!usable) { reassignMineral(w, o); return; }
            double cx = rx + 0.5, cy = ry + 0.5;
            if (w.distanceTo(cx, cy) <= 1.6) {
                w.setProgressTicks(0);
                w.setState(UnitState.GATHERING);
                o.gatherStage = 1;
            } else {
                w.setState(UnitState.GATHERING);
                stepToward(w, cx, cy);
            }
        } else if (o.gatherStage == 1) { // mining
            int avail = map.resourceAt(rx, ry);
            if (avail <= 0 || (gas && !hasGasBuilding(w.ownerId(), rx, ry))) { reassignMineral(w, o); return; }
            w.setProgressTicks(w.progressTicks() + 1);
            if (w.progressTicks() >= GATHER_TICKS) {
                int amt = Math.min(WORKER_CARRY, avail);
                map.setResourceAt(rx, ry, avail - amt);
                w.setCarried(rt, amt);
                w.setState(UnitState.RETURNING);
                o.gatherStage = 2;
            }
        } else { // return to town hall (approach the footprint edge, not the blocked centre)
            Building hall = nearestTownHall(w.ownerId(), w.x(), w.y());
            if (hall == null) { w.setState(UnitState.IDLE); return; }
            double[] ap = nearestPointOnBuilding(hall, w.x(), w.y());
            double hx = ap[0], hy = ap[1];
            if (w.distanceTo(hx, hy) <= 1.4) {
                Player p = state.player(w.ownerId());
                if (p != null) {
                    if (w.carriedType() == ResourceType.GAS) p.addGas(w.carriedResource());
                    else p.addMinerals(w.carriedResource());
                }
                w.setCarried(ResourceType.MINERALS, 0);
                o.gatherStage = 0;
                w.setState(UnitState.GATHERING);
            } else {
                w.setState(UnitState.RETURNING);
                stepToward(w, hx, hy);
            }
        }
    }

    private void reassignMineral(Unit w, Order o) {
        int[] t = nearestMineralTile(w.x(), w.y());
        if (t == null) { o.kind = Order.NONE; w.setState(UnitState.IDLE); return; }
        o.resX = t[0]; o.resY = t[1];
        o.gatherStage = (w.carriedResource() > 0) ? 2 : 0;
    }

    private boolean hasGasBuilding(int ownerId, int tx, int ty) {
        for (Building b : state.buildings()) {
            if (b.ownerId() == ownerId && b.type().isGas() && b.alive()
                    && b.state() != BuildState.UNDER_CONSTRUCTION
                    && b.tileX() == tx && b.tileY() == ty) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ projectiles

    private void updateProjectiles() {
        for (Projectile p : state.projectiles()) {
            if (p.done()) continue;
            double[] tp = entityPos(p.targetId());
            if (tp == null) { p.setDone(true); continue; } // target gone: fizzle
            double dx = tp[0] - p.x(), dy = tp[1] - p.y();
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d <= Math.max(0.35, p.speed())) {
                applyDamageAt(p.targetId(), tp[0], tp[1], p.damage(), p.damageType(), p.splashRadius());
                p.setDone(true);
            } else {
                p.setPos(p.x() + dx / d * p.speed(), p.y() + dy / d * p.speed());
            }
        }
    }

    // ------------------------------------------------------------------ damage model

    private void applyDamageAt(long targetId, double cx, double cy, int dmg, DamageType dt, int splash) {
        Unit tu = unit(targetId);
        if (tu != null && tu.alive()) {
            int victimOwner = tu.ownerId();
            damageUnit(tu, dmg, dt);
            if (splash > 0) splashUnits(tu, cx, cy, dmg, dt, splash, victimOwner);
            return;
        }
        Building tb = building(targetId);
        if (tb != null && tb.alive()) {
            damageBuilding(tb, dmg, dt);
        }
    }

    private void splashUnits(Unit primary, double cx, double cy, int dmg, DamageType dt, int splash, int side) {
        double r2 = (double) splash * splash;
        for (Unit other : state.units()) {
            if (other == primary || !other.alive() || other.ownerId() != side) continue;
            double dx = other.x() - cx, dy = other.y() - cy;
            if (dx * dx + dy * dy <= r2) damageUnit(other, dmg, dt);
        }
    }

    private void damageUnit(Unit v, int dmg, DamageType dt) {
        double mult = damageMultiplier(dt, v.type().armorClass());
        int raw = (int) Math.round(dmg * mult);
        int eff = Math.max(1, raw - v.type().armor());
        int sh = v.shield();
        if (sh > 0) {
            int ab = Math.min(sh, eff);
            v.setShield(sh - ab);
            eff -= ab;
        }
        if (eff > 0) {
            int hp = v.hp() - eff;
            if (hp <= 0) { v.setHp(0); v.setState(UnitState.DEAD); }
            else v.setHp(hp);
        }
        lastCombatTick.put(v.id(), tickCount);
    }

    private void damageBuilding(Building v, int dmg, DamageType dt) {
        // Buildings have no armor class in the contract; treat as LARGE (concussive halved).
        double mult = damageMultiplier(dt, ArmorClass.LARGE);
        int eff = Math.max(1, (int) Math.round(dmg * mult));
        v.setHp(Math.max(0, v.hp() - eff));
    }

    static double damageMultiplier(DamageType dt, ArmorClass ac) {
        switch (dt) {
            case EXPLOSIVE:  return ac == ArmorClass.SMALL ? 0.5 : 1.0;
            case CONCUSSIVE: return ac == ArmorClass.LARGE ? 0.5 : 1.0;
            case NORMAL:
            default:         return 1.0;
        }
    }

    // ------------------------------------------------------------------ shields / cleanup

    private void regenShields() {
        if ((tickCount % SHIELD_REGEN_PERIOD) != 0) return;
        for (Unit u : state.units()) {
            if (!u.alive() || u.type().maxShield() <= 0 || u.shield() >= u.type().maxShield()) continue;
            Long last = lastCombatTick.get(u.id());
            boolean safe = last == null || (tickCount - last) >= OUT_OF_COMBAT_TICKS;
            if (safe) u.setShield(Math.min(u.type().maxShield(), u.shield() + 1));
        }
    }

    private void cleanupDead() {
        Iterator<Unit> ui = state.units().iterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (!u.alive()) { orders.remove(u.id()); lastCombatTick.remove(u.id()); ui.remove(); }
        }
        Iterator<Building> bi = state.buildings().iterator();
        while (bi.hasNext()) {
            Building b = bi.next();
            if (!b.alive()) bi.remove();
        }
        Iterator<Projectile> pi = state.projectiles().iterator();
        while (pi.hasNext()) {
            if (pi.next().done()) pi.remove();
        }
    }

    // ------------------------------------------------------------------ supply / win

    private void recomputeSupply() {
        Map<Integer, int[]> acc = new HashMap<Integer, int[]>(); // id -> {used, cap}
        for (Player p : state.players()) acc.put(p.id(), new int[]{0, 0});
        for (Unit u : state.units()) {
            if (!u.alive()) continue;
            int[] a = acc.get(u.ownerId());
            if (a == null) continue;
            a[0] += u.type().supplyCost();
            a[1] += u.type().supplyProvided();
        }
        for (Building b : state.buildings()) {
            if (!b.alive()) continue;
            int[] a = acc.get(b.ownerId());
            if (a == null) continue;
            if (b.state() != BuildState.UNDER_CONSTRUCTION) a[1] += b.type().supplyProvided();
            for (UnitType t : b.productionQueue()) a[0] += t.supplyCost() * spawnCount(t);
        }
        for (Player p : state.players()) {
            int[] a = acc.get(p.id());
            p.setSupplyUsed(a[0]);
            p.setSupplyCap(Math.min(SUPPLY_MAX, a[1]));
        }
    }

    private void checkWinLose() {
        int alive = 0, lastAlive = -1;
        for (Player p : state.players()) {
            boolean hasUnit = false, hasBld = false;
            for (Unit u : state.units()) { if (u.ownerId() == p.id() && u.alive()) { hasUnit = true; break; } }
            for (Building b : state.buildings()) { if (b.ownerId() == p.id() && b.alive()) { hasBld = true; break; } }
            boolean dead = !hasUnit && !hasBld;
            if (dead) p.setDefeated(true);
            else { alive++; lastAlive = p.id(); }
        }
        if (alive <= 1 && state.winnerId() == -1) state.setWinnerId(lastAlive);
    }

    // ------------------------------------------------------------------ movement / steering

    private void stepToward(Unit u, double tx, double ty) {
        double speed = Math.max(0.0001, u.type().speed());
        double dx = tx - u.x(), dy = ty - u.y();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d <= 1e-9) return;
        double step = Math.min(speed, d);
        double nx = u.x() + dx / d * step, ny = u.y() + dy / d * step;
        if (!u.type().isFlyer() && blockedTile(nx, ny, tx, ty)) {
            double px = -dy / d, py = dx / d;       // perpendicular nudge
            double ax = u.x() + px * step, ay = u.y() + py * step;
            double bx = u.x() - px * step, by = u.y() - py * step;
            if (!blockedTile(ax, ay, tx, ty)) { nx = ax; ny = ay; }
            else if (!blockedTile(bx, by, tx, ty)) { nx = bx; ny = by; }
            // else: pass through to guarantee no permanent deadlock
        }
        GameMap map = state.map();
        nx = clamp(nx, 0, map.width() - 0.01);
        ny = clamp(ny, 0, map.height() - 0.01);
        u.setPos(nx, ny);
    }

    private boolean blockedTile(double x, double y, double tx, double ty) {
        GameMap map = state.map();
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        if (!map.inBounds(ix, iy)) return true;
        // never block the destination tile itself (e.g. attacking a building)
        if (ix == (int) Math.floor(tx) && iy == (int) Math.floor(ty)) return false;
        Building b = buildingCovering(ix, iy);
        return b != null;
    }

    private boolean blockedTile(double x, double y) {
        GameMap map = state.map();
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        if (!map.inBounds(ix, iy)) return true;
        return buildingCovering(ix, iy) != null;
    }

    // ------------------------------------------------------------------ lookups (package use)

    Unit unit(long id) {
        for (Unit u : state.units()) if (u.id() == id) return u;
        return null;
    }

    Building building(long id) {
        for (Building b : state.buildings()) if (b.id() == id) return b;
        return null;
    }

    Building buildingCovering(int tx, int ty) {
        for (Building b : state.buildings()) {
            if (!b.alive()) continue;
            if (tx >= b.tileX() && tx < b.tileX() + b.type().widthTiles()
                    && ty >= b.tileY() && ty < b.tileY() + b.type().heightTiles()) {
                return b;
            }
        }
        return null;
    }

    Building nearestTownHall(int ownerId, double x, double y) {
        Building best = null; double bd = Double.MAX_VALUE;
        for (Building b : state.buildings()) {
            if (b.ownerId() != ownerId || !b.alive() || !b.type().isTownHall()) continue;
            if (b.state() == BuildState.UNDER_CONSTRUCTION) continue;
            double dx = b.tileX() - x, dy = b.tileY() - y;
            double d = dx * dx + dy * dy;
            if (d < bd) { bd = d; best = b; }
        }
        return best;
    }

    /** Nearest live enemy unit, else enemy building, within sight. -1 if none. */
    long acquireTarget(Unit u, double sight) {
        double s2 = sight * sight;
        long best = -1; double bd = s2;
        for (Unit e : state.units()) {
            if (!e.alive() || e.ownerId() == u.ownerId()) continue;
            double dx = e.x() - u.x(), dy = e.y() - u.y();
            double d = dx * dx + dy * dy;
            if (d <= bd) { bd = d; best = e.id(); }
        }
        if (best != -1) return best;
        bd = s2;
        for (Building b : state.buildings()) {
            if (!b.alive() || b.ownerId() == u.ownerId()) continue;
            double cx = b.tileX() + b.type().widthTiles() / 2.0;
            double cy = b.tileY() + b.type().heightTiles() / 2.0;
            double dx = cx - u.x(), dy = cy - u.y();
            double d = dx * dx + dy * dy;
            if (d <= bd) { bd = d; best = b.id(); }
        }
        return best;
    }

    private boolean entityAlive(long id) {
        Unit u = unit(id);
        if (u != null) return u.alive();
        Building b = building(id);
        return b != null && b.alive();
    }

    /** Aim/approach point for an attacker: a unit's position, or the nearest point on a
     *  building's footprint (so melee can reach multi-tile buildings). null if gone. */
    double[] aimPoint(Unit u, long targetId) {
        Unit tu = unit(targetId);
        if (tu != null && tu.alive()) return new double[]{tu.x(), tu.y()};
        Building tb = building(targetId);
        if (tb != null && tb.alive()) return nearestPointOnBuilding(tb, u.x(), u.y());
        return null;
    }

    static double[] nearestPointOnBuilding(Building b, double x, double y) {
        double minx = b.tileX(), maxx = b.tileX() + b.type().widthTiles();
        double miny = b.tileY(), maxy = b.tileY() + b.type().heightTiles();
        return new double[]{clamp(x, minx, maxx), clamp(y, miny, maxy)};
    }

    /** Center position of a unit or building by id; null if gone. */
    double[] entityPos(long id) {
        Unit u = unit(id);
        if (u != null && u.alive()) return new double[]{u.x(), u.y()};
        Building b = building(id);
        if (b != null && b.alive()) {
            return new double[]{b.tileX() + b.type().widthTiles() / 2.0,
                    b.tileY() + b.type().heightTiles() / 2.0};
        }
        return null;
    }

    Order order(long id) {
        Order o = orders.get(id);
        if (o == null) { o = new Order(); orders.put(id, o); }
        return o;
    }

    GameState gameState() { return state; }

    long tick() { return tickCount; }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Mutable per-unit engine order data (kept here so domain.Unit stays a plain POJO). */
    static final class Order {
        static final int NONE = 0, MOVE = 1, ATTACK_MOVE = 2, ATTACK = 3, GATHER = 4;
        int kind = NONE;
        double tx, ty;
        long targetId = -1;
        int resX = -1, resY = -1;
        int gatherStage = 0; // 0=to field, 1=mining, 2=returning

        void reset() {
            kind = NONE; tx = 0; ty = 0; targetId = -1;
            resX = -1; resY = -1; gatherStage = 0;
        }
    }
}

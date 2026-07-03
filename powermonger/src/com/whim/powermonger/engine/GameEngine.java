package com.whim.powermonger.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import com.whim.powermonger.api.ActionResult;
import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.GameStateView;
import com.whim.powermonger.domain.ArmyBloc;
import com.whim.powermonger.domain.Captain;
import com.whim.powermonger.domain.MapGrid;
import com.whim.powermonger.domain.Order;
import com.whim.powermonger.domain.Pigeon;
import com.whim.powermonger.domain.Tile;
import com.whim.powermonger.domain.Town;
import com.whim.powermonger.domain.WorldGenerator;
import com.whim.powermonger.domain.WorldState;

/**
 * The simulation engine and the single {@link GameController} seam the UI talks to.
 *
 * <p>Owns a {@link WorldState} and drives it forward on a {@link SimulationLoop}
 * daemon thread at ~20 ticks/second. Every mutation — the tick and every player
 * command — happens under {@link #lock}; {@link #state()} reads {@code snapshot()}
 * under the same lock, so the Swing EDT always sees a consistent, non-torn view.</p>
 *
 * <p>Subsystems: {@link WeatherSystem}, {@link LifeAI}, {@link CombatResolver},
 * {@link VictoryMonitor}, plus movement / pigeon-lag / order-execution kept here
 * because they weave the event log together.</p>
 */
public final class GameEngine implements GameController {

    /** Simulation rate. */
    public static final int TICKS_PER_SECOND = 20;

    /** Army bloc travel speed, tiles per tick (before weather factor). */
    private static final double ARMY_SPEED = 0.06;
    /** How close a bloc must be to its destination to have "arrived". */
    private static final double ARRIVE_EPS = 0.15;
    /** Range within which an action order (recruit/gather/…) fires at its target. */
    private static final double ACTION_RANGE = 1.1;
    /** Cap on the retained event log. */
    private static final int MAX_EVENTS = 300;

    private final Object lock = new Object();

    private WorldState world;
    private Random rng;
    private long seed;
    private int selectedId = WorldState.SUPREME_COMMANDER_ID;

    private WeatherSystem weather;
    private LifeAI life;
    private CombatResolver combat;
    private VictoryMonitor victory;

    private final Deque<String> events = new ArrayDeque<String>();
    private final SimulationLoop loop;

    public GameEngine() {
        this(0L);
    }

    public GameEngine(long seed) {
        this.loop = new SimulationLoop(new Runnable() {
            @Override public void run() {
                stepOnce();
            }
        }, TICKS_PER_SECOND);
        newGame(seed);
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void newGame(long seed) {
        synchronized (lock) {
            this.seed = seed;
            this.rng = new Random(seed ^ 0x5DEECE66DL);
            this.world = WorldGenerator.generate(seed);
            this.weather = new WeatherSystem();
            this.life = new LifeAI();
            this.combat = new CombatResolver();
            this.victory = new VictoryMonitor();
            this.events.clear();
            this.selectedId = WorldState.SUPREME_COMMANDER_ID;
            applySelection(selectedId);
            log("New game (seed " + seed + ")");
        }
    }

    @Override
    public void start() {
        loop.start();
    }

    @Override
    public void stop() {
        loop.stop();
    }

    public boolean isRunning() {
        return loop.isRunning();
    }

    public long seed() {
        return seed;
    }

    // ----------------------------------------------------------------- controller

    @Override
    public GameStateView state() {
        synchronized (lock) {
            return world.snapshot();
        }
    }

    @Override
    public ActionResult issueOrder(int captainId, CommandType type, int targetTileX, int targetTileY) {
        if (type == null) {
            return ActionResult.fail("No order type given");
        }
        synchronized (lock) {
            Captain c = world.captain(captainId);
            if (c == null) {
                return ActionResult.fail("No such captain: " + captainId);
            }
            if (c.allegiance() != Allegiance.PLAYER) {
                return ActionResult.fail("You may only command your own captains");
            }
            if (!c.alive()) {
                return ActionResult.fail("Captain " + c.name() + " has fallen");
            }
            Order order = new Order(type, targetTileX, targetTileY, c.posture());

            Captain supreme = world.supremeCommander();
            boolean immediate = c.supremeCommander() || supreme == null || !supreme.alive();
            if (immediate) {
                applyOrder(c, order);
                log("Order " + type.label() + " to " + c.name() + " (immediate)");
                return ActionResult.ok(type.label() + " ordered to " + c.name());
            }

            // Subordinate: dispatch a carrier pigeon; command lag ∝ distance.
            double dist = CommandLag.distance(supreme.x(), supreme.y(), c.x(), c.y());
            Pigeon pigeon = new Pigeon(captainId, supreme.x(), supreme.y(), c.x(), c.y(), order);
            world.pigeons().add(pigeon);
            int ticks = CommandLag.flightTicks(dist);
            log("Pigeon dispatched: " + type.label() + " to " + c.name()
                    + " (~" + ticks + " ticks, " + String.format("%.1f", dist) + " tiles)");
            return ActionResult.ok("Pigeon carrying " + type.label() + " to " + c.name());
        }
    }

    @Override
    public ActionResult setDestination(int captainId, int targetTileX, int targetTileY) {
        return issueOrder(captainId, CommandType.MOVE, targetTileX, targetTileY);
    }

    @Override
    public ActionResult setPosture(int captainId, Posture posture) {
        if (posture == null) {
            return ActionResult.fail("No posture given");
        }
        synchronized (lock) {
            Captain c = world.captain(captainId);
            if (c == null) {
                return ActionResult.fail("No such captain: " + captainId);
            }
            if (c.allegiance() != Allegiance.PLAYER) {
                return ActionResult.fail("You may only command your own captains");
            }
            c.bloc().setPosture(posture);
            log(c.name() + " posture -> " + posture + " (" + posture.swords() + " swords)");
            return ActionResult.ok(c.name() + " now " + posture);
        }
    }

    @Override
    public void selectCaptain(int captainId) {
        synchronized (lock) {
            applySelection(captainId);
        }
    }

    @Override
    public int selectedCaptainId() {
        synchronized (lock) {
            return selectedId;
        }
    }

    private void applySelection(int captainId) {
        Captain chosen = world.captain(captainId);
        List<Captain> caps = world.captains();
        for (int i = 0; i < caps.size(); i++) {
            caps.get(i).setSelected(false);
        }
        if (chosen != null && chosen.allegiance() == Allegiance.PLAYER) {
            chosen.setSelected(true);
            selectedId = captainId;
        } else {
            selectedId = -1;
        }
    }

    // --------------------------------------------------------------------- tick

    /** Advance the world one fixed step. All mutation is confined here, under lock. */
    public void stepOnce() {
        synchronized (lock) {
            if (world.gameOver()) {
                return;
            }
            String seasonNote = weather.tick(world, rng);
            if (seasonNote != null) {
                log(seasonNote + " — weather " + world.weather());
            }
            life.tick(world, rng);
            deliverPigeons();
            moveArmies();
            processActions();
            combat.tick(world, drain());
            victory.tick(world, drain());
            world.incrementTick();
        }
    }

    // ---- pigeons ----------------------------------------------------------

    private void deliverPigeons() {
        List<Pigeon> pigeons = world.pigeons();
        for (int i = pigeons.size() - 1; i >= 0; i--) {
            Pigeon p = pigeons.get(i);
            double dist = CommandLag.distance(p.originX(), p.originY(), p.targetX(), p.targetY());
            p.advance(CommandLag.progressPerTick(CommandLag.flightTicks(dist)));
            if (p.arrived()) {
                Captain target = world.captain(p.targetCaptainId());
                if (target != null && target.alive()) {
                    target.commandQueue().enqueue(p.carriedOrder());
                    log("Pigeon arrived at " + target.name() + ": "
                            + p.carriedOrder().type().label());
                }
                pigeons.remove(i);
            }
        }
        // Drain any queued (delivered) orders into the blocs.
        List<Captain> caps = world.captains();
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            while (!c.commandQueue().isEmpty()) {
                applyOrder(c, c.commandQueue().poll());
            }
        }
    }

    /** Translate an order into bloc state (current order + a destination to act at). */
    private void applyOrder(Captain c, Order order) {
        ArmyBloc bloc = c.bloc();
        bloc.setCurrentOrder(order.type());
        bloc.setDestination(order.targetTileX() + 0.5, order.targetTileY() + 0.5);
    }

    // ---- movement ---------------------------------------------------------

    private void moveArmies() {
        double factor = Math.max(0.2, world.movementFactor());
        double speed = ARMY_SPEED * factor;
        List<Captain> caps = world.captains();
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (!c.alive()) {
                continue;
            }
            if (c.allegiance() == Allegiance.ENEMY) {
                // Simple aggressive AI: close on the nearest living player bloc.
                Captain prey = nearestLivingPlayer(c);
                if (prey != null) {
                    stepToward(c, prey.x(), prey.y(), speed);
                }
                continue;
            }
            ArmyBloc bloc = c.bloc();
            if (bloc.hasDestination()) {
                boolean arrived = stepToward(c, bloc.destX(), bloc.destY(), speed);
                if (arrived) {
                    CommandType order = bloc.currentOrder();
                    // Plain travel finishes on arrival; action orders are cleared by
                    // processActions once their effect fires.
                    if (order == null || order == CommandType.MOVE || order == CommandType.SCOUT) {
                        bloc.clearDestination();
                    }
                }
            }
        }
    }

    /** Move a captain toward (tx,ty). Returns true if it has effectively arrived. */
    private boolean stepToward(Captain c, double tx, double ty, double speed) {
        double dx = tx - c.x();
        double dy = ty - c.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= ARRIVE_EPS) {
            return true;
        }
        double stepLen = Math.min(speed, dist);
        c.setPosition(c.x() + dx / dist * stepLen, c.y() + dy / dist * stepLen);
        return dist - stepLen <= ARRIVE_EPS;
    }

    private Captain nearestLivingPlayer(Captain from) {
        List<Captain> caps = world.captains();
        Captain best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (c.alive() && c.allegiance() == Allegiance.PLAYER) {
                double d = CommandLag.distance(from.x(), from.y(), c.x(), c.y());
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }

    // ---- action execution -------------------------------------------------

    private void processActions() {
        List<Captain> caps = world.captains();
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (!c.alive() || c.allegiance() != Allegiance.PLAYER) {
                continue;
            }
            ArmyBloc bloc = c.bloc();
            CommandType order = bloc.currentOrder();
            if (order == null || !bloc.hasDestination()) {
                continue;
            }
            double d = CommandLag.distance(c.x(), c.y(), bloc.destX(), bloc.destY());
            if (d > ACTION_RANGE) {
                continue;
            }
            int tx = (int) Math.floor(bloc.destX());
            int ty = (int) Math.floor(bloc.destY());
            switch (order) {
                case RECRUIT:    doRecruit(c, tx, ty); break;
                case GATHER_FOOD: doGather(c, tx, ty); break;
                case SUPPLY_FOOD: doSupply(c, tx, ty); break;
                case DISBAND:    doDisband(c); break;
                case INVENT:     finishOrder(c, c.name() + " invents new arms"); break;
                case TRADE:      finishOrder(c, c.name() + " trades at " + tx + "," + ty); break;
                case FIGHT:      break; // resolved on contact by CombatResolver
                case MOVE:       break; // handled by movement
                case SCOUT:      break;
                default:         break;
            }
        }
    }

    private void doRecruit(Captain c, int tx, int ty) {
        Town town = townAt(tx, ty);
        if (town == null || town.allegiance() == Allegiance.ENEMY) {
            finishOrder(c, c.name() + " found no town to recruit");
            return;
        }
        int take = PostureMath.recruitCount(town.population(), c.posture());
        town.setPopulation(town.population() - take);
        c.bloc().addStrength(take);
        finishOrder(c, c.name() + " recruited " + take + " from " + town.name()
                + " (" + c.posture().swords() + " swords)");
    }

    private void doGather(Captain c, int tx, int ty) {
        Tile t = world.grid().tile(tx, ty);
        if (t == null) {
            finishOrder(c, c.name() + " found nothing to gather");
            return;
        }
        int take = PostureMath.lootAmount(t.foodPotential(), c.posture());
        t.setFoodPotential(t.foodPotential() - take);
        c.bloc().addFood(take);
        if (t.hasTrees() && world.grid().deforest(tx, ty)) {
            finishOrder(c, c.name() + " gathered " + take + " food, clearing woodland at "
                    + tx + "," + ty);
        } else {
            finishOrder(c, c.name() + " gathered " + take + " food at " + tx + "," + ty);
        }
    }

    private void doSupply(Captain c, int tx, int ty) {
        Town town = townAt(tx, ty);
        int give = PostureMath.scaled(c.food(), c.posture());
        c.bloc().addFood(-give);
        if (town != null) {
            town.setPopulation(town.population() + Math.max(0, give / 4));
            finishOrder(c, c.name() + " supplied " + give + " food to " + town.name());
        } else {
            finishOrder(c, c.name() + " cached " + give + " food");
        }
    }

    private void doDisband(Captain c) {
        int loss = PostureMath.scaled(c.strength(), c.posture());
        c.bloc().addStrength(-loss);
        finishOrder(c, c.name() + " disbanded " + loss + " men");
    }

    private void finishOrder(Captain c, String note) {
        c.bloc().clearDestination();
        log(note);
    }

    private Town townAt(int tx, int ty) {
        List<Town> towns = world.towns();
        for (int i = 0; i < towns.size(); i++) {
            Town t = towns.get(i);
            if (t.tileX() == tx && t.tileY() == ty) {
                return t;
            }
        }
        return null;
    }

    // ---- event log --------------------------------------------------------

    private void log(String msg) {
        events.addLast("[t" + world.tickCount() + "] " + msg);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
    }

    /**
     * A live view onto the event log passed to subsystems this tick. Returns the
     * backing deque as a List-like adapter is overkill — subsystems take a List, so
     * we hand them a small forwarding list bound to {@link #events}.
     */
    private List<String> drain() {
        return new EventSink(this);
    }

    /** Minimal List facade so subsystems can append events without exposing the deque. */
    private static final class EventSink extends java.util.AbstractList<String> {
        private final GameEngine engine;
        EventSink(GameEngine engine) { this.engine = engine; }
        @Override public boolean add(String s) { engine.log(s); return true; }
        @Override public String get(int index) { throw new UnsupportedOperationException(); }
        @Override public int size() { return 0; }
    }

    /** Snapshot of the most recent events (oldest first). For self-checks / consoles. */
    public java.util.List<String> recentEvents(int max) {
        synchronized (lock) {
            java.util.ArrayList<String> out = new java.util.ArrayList<String>(events);
            int from = Math.max(0, out.size() - max);
            return new java.util.ArrayList<String>(out.subList(from, out.size()));
        }
    }

    public MapGrid grid() {
        synchronized (lock) {
            return world.grid();
        }
    }
}

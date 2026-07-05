package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.Phase;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;
import com.whim.kenshi.domain.Character;
import com.whim.kenshi.domain.WorldBuilder;
import com.whim.kenshi.domain.WorldState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The simulation core and the sole implementation of {@link GameController}.
 *
 * <p>Threading: the {@link TickLoop} calls {@link #tick()} on a background daemon
 * thread. ALL world mutation happens there (guarded by {@code worldLock}, shared
 * only with {@link #newGame}). Player commands from the Swing EDT are enqueued on
 * a thread-safe {@link CommandQueue} and applied at the top of each tick. After
 * every tick a fresh immutable {@link Snapshot} is published to a volatile field;
 * {@link #state()} simply returns it, so it is non-blocking and always coherent.</p>
 */
public final class GameEngine implements GameController {

    private final Object worldLock = new Object();
    private final CommandQueue commands = new CommandQueue();
    private final EventLog log = new EventLog();
    private final TickLoop loop;

    // --- world + derived systems (rebuilt on newGame) ---
    private WorldState world;
    private Views.MapView cachedMap;
    private Pathfinder pathfinder;
    private Movement movement;
    private CharacterAI ai;
    private CombatSystem combat;
    private SurvivalSystem survival;
    private Random rng;
    private final Map<String, AiMemory> memories = new HashMap<String, AiMemory>();
    private final List<String> selection = new ArrayList<String>();
    /** Ids that moved during the current tick (drives the Snapshot MOVING flag). */
    private final Set<String> movingIds = new HashSet<String>();

    // --- clock / control ---
    private long tickCount;
    private double worldSeconds;
    private Phase phase = Phase.PAUSED;
    private volatile boolean paused = true;
    private volatile int gameSpeed = 1;

    // --- published snapshot (read by EDT) ---
    private volatile Views.GameStateView latest;

    public GameEngine() {
        this.loop = new TickLoop(new TickLoop.Tick() {
            public void run() {
                tick();
            }
        });
    }

    // ==================================================================
    // GameController — snapshot
    // ==================================================================
    public Views.GameStateView state() {
        Views.GameStateView s = latest;
        if (s == null) {
            // Before newGame(): hand back an empty but valid snapshot.
            return emptyState();
        }
        return s;
    }

    // ==================================================================
    // GameController — lifecycle
    // ==================================================================
    public void newGame(long seed) {
        synchronized (worldLock) {
            world = WorldBuilder.build(seed);
            cachedMap = new Snapshot.ImmutableMapView(world.map());
            rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
            pathfinder = new Pathfinder(world.map());
            movement = new Movement(pathfinder);
            ai = new CharacterAI(world.factions(), rng);
            combat = new CombatSystem(rng, log);
            survival = new SurvivalSystem(log);

            memories.clear();
            List<Character> chars = world.charactersList();
            for (int i = 0; i < chars.size(); i++) {
                Character c = chars.get(i);
                memories.put(c.id(), new AiMemory(c.x(), c.y()));
            }
            selection.clear();
            log.clear();
            tickCount = 0L;
            worldSeconds = 0.0;
            phase = paused ? Phase.PAUSED : Phase.RUNNING;
            log.add(0L, "A new game begins (seed " + seed + ")");
            publish();
        }
    }

    public void start() {
        synchronized (worldLock) {
            paused = false;
            if (phase != Phase.GAME_OVER) {
                phase = Phase.RUNNING;
            }
        }
        loop.start();
    }

    public void stop() {
        loop.stop();
    }

    // ==================================================================
    // GameController — real-time-with-pause
    // ==================================================================
    public void setPaused(boolean p) {
        this.paused = p;
        synchronized (worldLock) {
            if (phase != Phase.GAME_OVER) {
                phase = p ? Phase.PAUSED : Phase.RUNNING;
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void togglePause() {
        setPaused(!paused);
    }

    public void setGameSpeed(int multiplier) {
        int m = multiplier;
        if (m < 1) { m = 1; }
        if (m > 16) { m = 16; }
        this.gameSpeed = m;
    }

    // ==================================================================
    // GameController — selection & orders (enqueued, applied on tick thread)
    // ==================================================================
    public void setSelection(final List<String> characterIds) {
        final List<String> ids = copyIds(characterIds);
        commands.submit(new CommandQueue.Command() {
            public void apply(WorldState w) {
                applySelection(w, ids);
            }
        });
    }

    public void orderMove(final List<String> characterIds, final double worldX, final double worldY) {
        final List<String> ids = copyIds(characterIds);
        commands.submit(new CommandQueue.Command() {
            public void apply(WorldState w) {
                for (int i = 0; i < ids.size(); i++) {
                    Character c = playerChar(w, ids.get(i));
                    if (c == null) { continue; }
                    c.orderMove(worldX, worldY);
                    resetGoal(c);
                }
            }
        });
    }

    public void orderAttack(final List<String> characterIds, final String targetId) {
        final List<String> ids = copyIds(characterIds);
        commands.submit(new CommandQueue.Command() {
            public void apply(WorldState w) {
                for (int i = 0; i < ids.size(); i++) {
                    Character c = playerChar(w, ids.get(i));
                    if (c == null) { continue; }
                    c.orderAttack(targetId);
                    resetGoal(c);
                }
            }
        });
    }

    public void orderInteract(final List<String> characterIds, final String nodeId) {
        final List<String> ids = copyIds(characterIds);
        commands.submit(new CommandQueue.Command() {
            public void apply(WorldState w) {
                for (int i = 0; i < ids.size(); i++) {
                    Character c = playerChar(w, ids.get(i));
                    if (c == null) { continue; }
                    c.orderInteract(nodeId);
                    resetGoal(c);
                }
            }
        });
    }

    // ==================================================================
    // Tick
    // ==================================================================
    void tick() {
        synchronized (worldLock) {
            if (world == null) {
                return;
            }
            // Orders/selection apply at the top of the tick (even while paused,
            // so the player can queue orders during pause).
            commands.drainInto(world);

            movingIds.clear();
            if (!paused && phase == Phase.RUNNING) {
                int steps = gameSpeed < 1 ? 1 : gameSpeed;
                for (int i = 0; i < steps; i++) {
                    step(Config.WORLD_SECONDS_PER_TICK);
                    if (phase == Phase.GAME_OVER) {
                        break;
                    }
                }
            }
            publish();
        }
    }

    /** Advance the simulation by {@code dtWorld} world-seconds. */
    private void step(double dtWorld) {
        List<Character> all = world.charactersList();

        // 1. AI decisions
        for (int i = 0; i < all.size(); i++) {
            ai.decide(all.get(i), all, world, memories, dtWorld);
        }

        // 2. Movement (trains Athletics)
        for (int i = 0; i < all.size(); i++) {
            Character c = all.get(i);
            AiMemory mem = memories.get(c.id());
            if (mem == null) { continue; }
            double moved = movement.advance(c, mem, dtWorld);
            if (moved > 0.05) {
                movingIds.add(c.id());
            }
            SkillSystem.trainMovement(c, moved);
        }

        // 3. Combat resolution
        Set<String> fighting = new HashSet<String>();
        for (int i = 0; i < all.size(); i++) {
            Character c = all.get(i);
            MoveState ms = c.moveState();
            if (ms == MoveState.DEAD || ms == MoveState.DOWNED) {
                continue;
            }
            AiMemory mem = memories.get(c.id());
            if (mem == null || mem.combatTargetId == null) {
                continue;
            }
            Character target = world.character(mem.combatTargetId);
            if (target == null) {
                continue;
            }
            MoveState tms = target.moveState();
            if (tms == MoveState.DEAD || tms == MoveState.DOWNED) {
                continue;
            }
            if (!CharacterAI.inMelee(c, target)) {
                continue;
            }
            c.setHeading(Math.atan2(target.y() - c.y(), target.x() - c.x()));
            combat.resolveSwing(tickCount, c, target);
            fighting.add(c.id());
            fighting.add(target.id());
        }

        // 4. Survival (hunger, bleed, heal, DOWNED/DEAD recompute)
        for (int i = 0; i < all.size(); i++) {
            Character c = all.get(i);
            survival.step(tickCount, c, dtWorld, fighting.contains(c.id()));
        }

        // 5. Clock + phase
        tickCount++;
        worldSeconds += dtWorld;
        if (!anyPlayerAlive(all)) {
            if (phase != Phase.GAME_OVER) {
                phase = Phase.GAME_OVER;
                log.add(tickCount, "The player squad has fallen. Game over.");
            }
        }
    }

    private boolean anyPlayerAlive(List<Character> all) {
        boolean sawPlayer = false;
        for (int i = 0; i < all.size(); i++) {
            Character c = all.get(i);
            if (c.faction() == FactionId.PLAYER) {
                sawPlayer = true;
                if (c.moveState() != MoveState.DEAD) {
                    return true;
                }
            }
        }
        // If the world has no player faction at all, don't declare game over.
        return !sawPlayer;
    }

    // ==================================================================
    // Snapshot publishing
    // ==================================================================
    private void publish() {
        int reportedSpeed = paused ? 0 : gameSpeed;
        latest = Snapshot.build(world, cachedMap, phase, tickCount, worldSeconds,
                reportedSpeed, selection, log.copy(), movingIds);
    }

    private Views.GameStateView emptyState() {
        List<Views.CharacterView> chars = new ArrayList<Views.CharacterView>();
        List<Views.SquadView> squads = new ArrayList<Views.SquadView>();
        List<Views.FactionView> factions = new ArrayList<Views.FactionView>();
        List<Views.NodeView> nodes = new ArrayList<Views.NodeView>();
        List<String> sel = new ArrayList<String>();
        List<Views.LogView> logs = new ArrayList<Views.LogView>();
        Views.MapView map = new Views.MapView() {
            public int tiles() { return Config.MAP_TILES; }
            public double tileSize() { return Config.TILE_SIZE; }
            public com.whim.kenshi.api.Enums.Terrain terrain(int col, int row) {
                return com.whim.kenshi.api.Enums.Terrain.SAND;
            }
        };
        return new Snapshot.ImmutableGameStateView(Phase.PAUSED, 0L, 0.0, 0, map,
                chars, squads, factions, nodes, sel, logs);
    }

    // ==================================================================
    // Command helpers (run on tick thread)
    // ==================================================================
    private void applySelection(WorldState w, List<String> ids) {
        // Clear old selection flags.
        for (int i = 0; i < selection.size(); i++) {
            Character c = w.character(selection.get(i));
            if (c != null) {
                c.setSelected(false);
            }
        }
        selection.clear();
        // Set new (player-controlled only).
        for (int i = 0; i < ids.size(); i++) {
            Character c = playerChar(w, ids.get(i));
            if (c != null) {
                c.setSelected(true);
                selection.add(c.id());
            }
        }
    }

    private void resetGoal(Character c) {
        AiMemory mem = memories.get(c.id());
        if (mem != null) {
            mem.clearGoal();
        }
    }

    private static Character playerChar(WorldState w, String id) {
        Character c = w.character(id);
        if (c == null || c.faction() != FactionId.PLAYER) {
            return null;
        }
        return c;
    }

    private static List<String> copyIds(List<String> ids) {
        List<String> out = new ArrayList<String>();
        if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
                String s = ids.get(i);
                if (s != null) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}

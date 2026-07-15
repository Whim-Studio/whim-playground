package com.whim.settlers.economy;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Recipe.ExtractorNeed;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.transport.TransportSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The production simulation for one player: settler pool, staffing, the
 * production chains, that player's central {@link #stock} stockpile, and the two
 * priority systems (goods distribution and tool priority). One {@code Economy}
 * exists per player and only touches buildings that player owns, so the human and
 * the AI run the identical rules over the shared road network.
 *
 * <p>Inputs are pulled from, and outputs pushed to, this player's Castle
 * stockpile via the flag-relay {@link TransportSystem} (Phase 4): a building with
 * no road to the Castle stalls.
 *
 * <p>Distribution priority is realised as a per-building-type integer: when a
 * scarce input is contested, buildings are serviced highest-priority-first each
 * tick, so raising a consumer's priority genuinely lets it win scarce goods.
 */
public final class Economy {

    private static final int EXTRACT_RADIUS = 5;
    private static final float SETTLER_SPAWN_SECONDS = 4f;
    private static final int   BASE_POP_CAP = 16;
    private static final int   WAREHOUSE_POP_CAP = 16;

    private final TileMap map;
    private final BuildingManager buildings;
    private final TransportSystem transport;
    private final int playerId;
    private final Inventory stock = new Inventory();

    /** Idle settlers waiting for a job, and the running total (idle + employed). */
    private int idle;
    private int totalPopulation;
    private float spawnTimer;

    /** Per-building runtime state, created lazily when a building finishes. */
    private final Map<Building, BState> state = new HashMap<Building, BState>();

    /** Distribution priority per building type (1..9, default 5). */
    private final EnumMap<BuildingType, Integer> priority =
            new EnumMap<BuildingType, Integer>(BuildingType.class);

    /** Player-ordered tool-production priority for the Tool Maker. */
    private final List<Good> toolOrder = new ArrayList<Good>();

    public Economy(TileMap map, BuildingManager buildings, TransportSystem transport, int playerId) {
        this.map = map;
        this.buildings = buildings;
        this.transport = transport;
        this.playerId = playerId;
        for (Good g : ProductionChains.toolOutputs()) toolOrder.add(g);
        seedStartingStock();
    }

    public int playerId() { return playerId; }

    private int castleFlag() { return transport.castleFlagId(playerId); }

    /** Starting supplies and settlers so the first production ring can bootstrap. */
    private void seedStartingStock() {
        idle = 12;
        totalPopulation = 12;
        stock.add(Good.PLANK, 24);
        stock.add(Good.STONE, 12);
        stock.add(Good.BREAD, 12);
        // One of each tool, so the initial buildings can be staffed before the
        // Tool Maker is running.
        for (Good g : ProductionChains.toolOutputs()) stock.add(g, 1);
    }

    // ------------------------------------------------------------------ update

    public void update(float dt) {
        spawnSettlers(dt);
        staffBuildings();
        runProduction(dt);
    }

    private void spawnSettlers(float dt) {
        int cap = BASE_POP_CAP + WAREHOUSE_POP_CAP * countFinished(BuildingType.WAREHOUSE);
        if (totalPopulation >= cap) return;
        spawnTimer += dt;
        if (spawnTimer >= SETTLER_SPAWN_SECONDS) {
            spawnTimer = 0f;
            idle++;
            totalPopulation++;
        }
    }

    /** Draw one idle settler for military conscription; false if none free. */
    public boolean takeSettler() {
        if (idle <= 0) return false;
        idle--;
        return true;
    }

    /** Assign idle settlers to unstaffed finished buildings that have their tool. */
    private void staffBuildings() {
        for (Building b : buildings.all()) {
            if (!b.isFinished()) continue;
            if (b.ownerId() != playerId) continue;
            Recipe r = ProductionChains.of(b.type());
            if (r == null) continue; // non-productive building
            BState s = stateOf(b);
            if (s.worker != null) continue;
            // A settler must be able to walk from the Castle to the building.
            Integer flag = transport.flagFor(b);
            if (flag == null || !transport.connected(flag, castleFlag())) {
                s.reason = "no road"; continue;
            }
            if (idle <= 0) { s.reason = "no worker"; continue; }
            Good tool = r.requiredTool();
            if (tool != null && !stock.has(tool, 1)) {
                s.reason = "needs " + tool.label().toLowerCase();
                continue;
            }
            if (tool != null) stock.take(tool, 1);
            s.worker = new Settler(r.profession());
            idle--;
            s.reason = "staffed";
        }
    }

    /** Run one production step for every staffed building, priority-ordered. */
    private void runProduction(float dt) {
        List<Building> order = new ArrayList<Building>(buildings.all());
        Collections.sort(order, new Comparator<Building>() {
            @Override public int compare(Building a, Building b) {
                return priorityOf(b.type()) - priorityOf(a.type()); // desc
            }
        });
        for (Building b : order) {
            if (!b.isFinished()) continue;
            if (b.ownerId() != playerId) continue;
            Recipe r = ProductionChains.of(b.type());
            if (r == null) continue;
            BState s = stateOf(b);
            if (s.worker == null) continue;
            step(b, r, s, dt);
        }
    }

    private void step(Building b, Recipe r, BState s, float dt) {
        int flag = flagId(b);
        boolean connected = flag >= 0 && transport.connected(flag, castleFlag());

        if (s.progress > 0f) {
            // Mid-cycle: advance, then complete (output relays to the Castle).
            s.progress += dt / r.seconds();
            if (s.progress >= 1f) {
                s.progress = 0f;
                complete(b, r, s, flag);
            }
            s.reason = "working";
            return;
        }
        // A building with no road to the Castle can neither receive inputs nor
        // ship output — it stalls. This is the "goods only move on roads" rule.
        if (!connected) { s.reason = "no road"; return; }
        if (!extractorReady(b, r)) { s.reason = stalledReason(r); return; }

        Good target = resolveOutput(b, r); // Tool Maker picks a tool; null = none wanted
        if (r.output() == null && r.profession() == Profession.TOOLMAKER && target == null) {
            s.reason = "no tool demand"; return;
        }
        // Pull any missing inputs (and food) from the Castle stockpile over roads.
        requestInputs(r, s, flag);
        if (!bufferReady(r, s)) { s.reason = "awaiting goods"; return; }

        // Consume delivered inputs from the building's buffer and begin a cycle.
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) s.buffer.take(e.getKey(), e.getValue());
        if (r.consumesFood()) takeBufferFood(s);
        s.pendingOutput = target;
        s.progress = 0.0001f;
        s.reason = "working";
    }

    /** Ship any not-yet-present inputs from the Castle stockpile to the building buffer. */
    private void requestInputs(Recipe r, final BState s, int flag) {
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) {
            final Good g = e.getKey();
            final int qty = e.getValue();
            if (s.buffer.get(g) >= qty || s.inTransit.contains(g)) continue;
            if (!stock.has(g, qty)) continue;
            stock.take(g, qty);
            s.inTransit.add(g);
            transport.ship(g, castleFlag(), flag, new Runnable() {
                @Override public void run() { s.buffer.add(g, qty); s.inTransit.remove(g); }
            });
        }
        if (r.consumesFood() && bufferFood(s) == 0 && !s.foodInTransit) {
            final Good food = availableFood();
            if (food != null) {
                stock.take(food, 1);
                s.foodInTransit = true;
                transport.ship(food, castleFlag(), flag, new Runnable() {
                    @Override public void run() { s.buffer.add(food, 1); s.foodInTransit = false; }
                });
            }
        }
    }

    private boolean bufferReady(Recipe r, BState s) {
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) {
            if (s.buffer.get(e.getKey()) < e.getValue()) return false;
        }
        return !r.consumesFood() || bufferFood(s) > 0;
    }

    private int bufferFood(BState s) {
        return s.buffer.get(Good.BREAD) + s.buffer.get(Good.FISH) + s.buffer.get(Good.MEAT);
    }

    private void takeBufferFood(BState s) {
        if (s.buffer.take(Good.BREAD, 1)) return;
        if (s.buffer.take(Good.FISH, 1)) return;
        s.buffer.take(Good.MEAT, 1);
    }

    private void complete(Building b, Recipe r, BState s, int flag) {
        if (r.profession() == Profession.FORESTER) {
            replantForest(b); // special: no good, plants a tree
            return;
        }
        final Good out;
        final int qty;
        if (r.profession() == Profession.BLACKSMITH) {
            out = s.blacksmithShield ? Good.SHIELD : Good.SWORD;
            s.blacksmithShield = !s.blacksmithShield;
            qty = 1;
        } else if (r.output() != null) {
            out = r.output();
            qty = r.outputQty();
        } else {
            out = s.pendingOutput; // Tool Maker's chosen tool
            qty = 1;
        }
        if (r.profession() == Profession.WOODCUTTER) fellTree(b); // deplete forest
        if (out == null) return;
        final Good fout = out; final int fqty = qty;
        // Relay the finished good back to the Castle stockpile over the roads.
        transport.ship(out, flag, castleFlag(), new Runnable() {
            @Override public void run() { stock.add(fout, fqty); }
        });
    }

    private int flagId(Building b) {
        Integer f = transport.flagFor(b);
        return f == null ? -1 : f;
    }

    // ------------------------------------------------------- extractor helpers

    private boolean extractorReady(Building b, Recipe r) {
        switch (r.extractorNeed()) {
            case NONE:      return true;
            case MOUNTAIN:  return true; // guaranteed by placement (mine sits on it)
            case FOREST:    return hasTerrainNear(b, TerrainType.FOREST);
            case WATER:     return hasWaterNear(b);
            case STONE_ROCK:return hasTerrainNear(b, TerrainType.MOUNTAIN_STONE);
            case FARMLAND:  return hasTerrainNear(b, TerrainType.GRASS);
            default:        return false;
        }
    }

    private String stalledReason(Recipe r) {
        switch (r.extractorNeed()) {
            case FOREST:     return "no trees";
            case STONE_ROCK: return "no rock";
            case WATER:      return "no water";
            case FARMLAND:   return "no land";
            default:         return "blocked";
        }
    }

    private boolean hasTerrainNear(Building b, TerrainType want) {
        return findTile(b, want) != null;
    }

    private boolean hasWaterNear(Building b) {
        return findTile(b, TerrainType.WATER) != null;
    }

    /** First tile of the wanted terrain within the extract radius, or null. */
    private int[] findTile(Building b, TerrainType want) {
        int cx = b.x() + b.type().footprintW() / 2;
        int cy = b.y() + b.type().footprintH() / 2;
        for (int dy = -EXTRACT_RADIUS; dy <= EXTRACT_RADIUS; dy++) {
            for (int dx = -EXTRACT_RADIUS; dx <= EXTRACT_RADIUS; dx++) {
                int tx = cx + dx, ty = cy + dy;
                if (map.inBounds(tx, ty) && map.get(tx, ty) == want) {
                    return new int[] { tx, ty };
                }
            }
        }
        return null;
    }

    /** Woodcutter fells a nearby tree: FOREST -> GRASS. Sustained by foresters. */
    private void fellTree(Building b) {
        int[] t = findTile(b, TerrainType.FOREST);
        if (t != null) map.set(t[0], t[1], TerrainType.GRASS);
    }

    /** Forester replants: GRASS -> FOREST within its radius. */
    private void replantForest(Building b) {
        int[] t = findTile(b, TerrainType.GRASS);
        if (t != null) map.set(t[0], t[1], TerrainType.FOREST);
    }

    // ------------------------------------------------------------ input/output

    private Good availableFood() {
        if (stock.has(Good.BREAD, 1)) return Good.BREAD;
        if (stock.has(Good.FISH, 1))  return Good.FISH;
        if (stock.has(Good.MEAT, 1))  return Good.MEAT;
        return null;
    }

    /**
     * For the Tool Maker, pick which tool to build: the highest-priority tool
     * (per {@link #toolOrder}) that is in demand — i.e. some finished building is
     * waiting for it, or the stockpile has none in reserve. Returns null if no
     * tool is currently wanted. For all other buildings returns their fixed
     * output.
     */
    private Good resolveOutput(Building b, Recipe r) {
        if (r.profession() != Profession.TOOLMAKER) return r.output();
        for (Good tool : toolOrder) {
            if (toolInDemand(tool)) return tool;
        }
        return null;
    }

    private boolean toolInDemand(Good tool) {
        if (!stock.has(tool, 1)) {
            // Any unstaffed building that needs this tool, or just keep a reserve.
            for (Building b : buildings.all()) {
                Recipe r = ProductionChains.of(b.type());
                if (b.isFinished() && r != null && tool.equals(r.requiredTool())
                        && stateOf(b).worker == null) {
                    return true;
                }
            }
            return true; // no reserve of this tool -> build one
        }
        return false;
    }

    // --------------------------------------------------------------- accessors

    private BState stateOf(Building b) {
        BState s = state.get(b);
        if (s == null) { s = new BState(); state.put(b, s); }
        return s;
    }

    private int countFinished(BuildingType type) {
        int n = 0;
        for (Building b : buildings.all()) {
            if (b.isFinished() && b.type() == type) n++;
        }
        return n;
    }

    public int priorityOf(BuildingType t) {
        Integer v = priority.get(t);
        return v == null ? 5 : v;
    }

    public void bumpPriority(BuildingType t, int delta) {
        int v = Math.max(1, Math.min(9, priorityOf(t) + delta));
        priority.put(t, v);
    }

    /** Move a tool up (-1) or down (+1) in the Tool Maker's priority order. */
    public void moveTool(Good tool, int delta) {
        int i = toolOrder.indexOf(tool);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= toolOrder.size()) return;
        Collections.swap(toolOrder, i, j);
    }

    public Inventory stock()          { return stock; }
    public int idle()                 { return idle; }
    public int totalPopulation()      { return totalPopulation; }
    public List<Good> toolOrder()     { return Collections.unmodifiableList(toolOrder); }

    /** Short status string for a building, for the info overlay. */
    public String statusOf(Building b) {
        if (!b.isFinished()) return "building";
        Recipe r = ProductionChains.of(b.type());
        if (r == null) return "";
        return stateOf(b).reason;
    }

    public boolean isStaffed(Building b) {
        return b.isFinished() && stateOf(b).worker != null;
    }

    /** Per-building runtime state. */
    private static final class BState {
        Settler worker;
        float progress;
        Good pendingOutput;      // resolved tool for the Tool Maker
        boolean blacksmithShield;// alternates sword/shield
        String reason = "idle";
        /** Inputs delivered by the transport relay, awaiting consumption. */
        final Inventory buffer = new Inventory();
        /** Input goods currently in transit to this building (avoid double-requesting). */
        final EnumSet<Good> inTransit = EnumSet.noneOf(Good.class);
        boolean foodInTransit;
    }
}

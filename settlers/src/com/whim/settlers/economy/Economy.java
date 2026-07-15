package com.whim.settlers.economy;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Recipe.ExtractorNeed;
import com.whim.settlers.map.TerrainType;
import com.whim.settlers.map.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The production simulation: settler pool, staffing, the production chains, the
 * central stockpile, and the two player-facing priority systems (goods
 * distribution and tool priority).
 *
 * <p><b>Transport is stubbed instant this phase</b> — inputs are pulled from and
 * outputs pushed to one central {@link #stock} stockpile, as if every building
 * were directly connected. Phase 4 replaces that with the flag-relay carrier
 * system; the recipe/staffing logic here is unchanged by that swap.
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

    public Economy(TileMap map, BuildingManager buildings) {
        this.map = map;
        this.buildings = buildings;
        for (Good g : ProductionChains.toolOutputs()) toolOrder.add(g);
        seedStartingStock();
    }

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

    /** Assign idle settlers to unstaffed finished buildings that have their tool. */
    private void staffBuildings() {
        for (Building b : buildings.all()) {
            if (!b.isFinished()) continue;
            Recipe r = ProductionChains.of(b.type());
            if (r == null) continue; // non-productive building
            BState s = stateOf(b);
            if (s.worker != null) continue;
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
            Recipe r = ProductionChains.of(b.type());
            if (r == null) continue;
            BState s = stateOf(b);
            if (s.worker == null) continue;
            step(b, r, s, dt);
        }
    }

    private void step(Building b, Recipe r, BState s, float dt) {
        if (s.progress > 0f) {
            // Mid-cycle: advance, then complete.
            s.progress += dt / r.seconds();
            if (s.progress >= 1f) {
                s.progress = 0f;
                complete(b, r, s);
            }
            s.reason = "working";
            return;
        }
        // Idle: try to start a new cycle.
        if (!extractorReady(b, r)) { s.reason = stalledReason(r); return; }
        Good food = r.consumesFood() ? availableFood() : null;
        if (r.consumesFood() && food == null) { s.reason = "no food"; return; }
        Good target = resolveOutput(b, r); // may pick a tool; null if nothing to make
        if (r.output() == null && r.profession() == Profession.TOOLMAKER && target == null) {
            s.reason = "no tool demand"; return;
        }
        if (!inputsAvailable(r)) { s.reason = missingInput(r); return; }
        // Consume inputs (+food) and begin.
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) stock.take(e.getKey(), e.getValue());
        if (food != null) stock.take(food, 1);
        s.pendingOutput = target;
        s.progress = 0.0001f;
        s.reason = "working";
    }

    private void complete(Building b, Recipe r, BState s) {
        if (r.profession() == Profession.FORESTER) {
            replantForest(b); // special: no good, plants a tree
            return;
        }
        if (r.profession() == Profession.BLACKSMITH) {
            Good weapon = s.blacksmithShield ? Good.SHIELD : Good.SWORD;
            s.blacksmithShield = !s.blacksmithShield;
            stock.add(weapon, 1);
            return;
        }
        Good out = r.output() != null ? r.output() : s.pendingOutput;
        if (out != null) stock.add(out, r.output() != null ? r.outputQty() : 1);
        if (r.profession() == Profession.WOODCUTTER) fellTree(b); // deplete forest
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

    private boolean inputsAvailable(Recipe r) {
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) {
            if (!stock.has(e.getKey(), e.getValue())) return false;
        }
        return true;
    }

    private String missingInput(Recipe r) {
        for (Map.Entry<Good, Integer> e : r.inputs().entrySet()) {
            if (!stock.has(e.getKey(), e.getValue())) return "no " + e.getKey().label().toLowerCase();
        }
        return "idle";
    }

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
    }
}

package com.whim.settlers.transport;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.economy.Good;
import com.whim.settlers.map.TileMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives the flag-relay transport: places building flags, builds roads (routed
 * with {@link Pathfinder}), and each tick advances carriers and dispatches
 * waiting goods onto free road segments. Goods move only along roads and only
 * hop-by-hop between flags — never teleported.
 *
 * <p>Topology note: producers ship their output to the Castle stockpile and
 * consumers request inputs from it, so the Castle is the hub. The physical
 * movement is still a genuine multi-hop relay with per-segment carriers and
 * congestion; the hub simplification (vs. arbitrary producer→consumer routing)
 * is documented in the GDD.
 */
public final class TransportSystem {

    private final TileMap map;
    private final BuildingManager buildings;
    private final RoadNetwork net = new RoadNetwork();
    private final Map<Building, Integer> buildingFlag = new HashMap<Building, Integer>();
    /** Each player's Castle flag id — the stockpile hub for that player. */
    private final Map<Integer, Integer> castleFlag = new HashMap<Integer, Integer>();
    /** Per-road sustained-congestion accumulator (seconds); drives donkey upgrades. */
    private final Map<Road, Float> congestion = new HashMap<Road, Float>();

    /** Seconds a segment must stay congested before it gains a second carrier. */
    private static final float DONKEY_AFTER_SECONDS = 6f;

    public TransportSystem(TileMap map, BuildingManager buildings) {
        this.map = map;
        this.buildings = buildings;
    }

    public RoadNetwork network() { return net; }

    /** The stockpile-hub flag for a player, or -1 if that player has no Castle. */
    public int castleFlagId(int player) {
        Integer f = castleFlag.get(player);
        return f == null ? -1 : f;
    }

    /** True for tiles a road may cross (not water, mountain, or a building). */
    public boolean walkable(int x, int y) {
        if (!map.inBounds(x, y)) return false;
        if (map.get(x, y).isWater() || map.get(x, y).isMountain()) return false;
        return buildings.at(x, y) == null;
    }

    /** Ensure a flag exists next to a building; returns its flag id (or -1). */
    public int ensureFlagFor(Building b) {
        Integer existing = buildingFlag.get(b);
        if (existing != null) return existing;
        int[] tile = adjacentWalkable(b);
        if (tile == null) return -1;
        Flag f = net.addFlag(tile[0], tile[1]);
        buildingFlag.put(b, f.id());
        return f.id();
    }

    public Integer flagFor(Building b) { return buildingFlag.get(b); }

    /** Register a player's Castle and remember its flag as that player's hub. */
    public void registerCastle(Building castle) {
        castleFlag.put(castle.ownerId(), ensureFlagFor(castle));
    }

    private int[] adjacentWalkable(Building b) {
        int x0 = b.x() - 1, y0 = b.y() - 1;
        int x1 = b.x() + b.type().footprintW(), y1 = b.y() + b.type().footprintH();
        // Scan the ring around the footprint for the first walkable tile.
        for (int x = x0; x <= x1; x++) {
            if (isFree(x, y1)) return new int[] { x, y1 };
            if (isFree(x, y0)) return new int[] { x, y0 };
        }
        for (int y = y0; y <= y1; y++) {
            if (isFree(x1, y)) return new int[] { x1, y };
            if (isFree(x0, y)) return new int[] { x0, y };
        }
        return null;
    }

    private boolean isFree(int x, int y) { return walkable(x, y); }

    /** Place a standalone flag if the tile is walkable and empty. Returns it or null. */
    public Flag placeFlag(int x, int y) {
        if (!walkable(x, y)) return null;
        return net.addFlag(x, y);
    }

    /**
     * Build a road between two flags along the shortest walkable tile path.
     * Returns the road, or null if the flags are the same, already joined, or no
     * path exists.
     */
    public Road buildRoad(int flagIdA, int flagIdB) {
        if (flagIdA == flagIdB) return null;
        Flag a = net.flag(flagIdA), b = net.flag(flagIdB);
        if (a == null || b == null) return null;
        if (net.directRoadExists(flagIdA, flagIdB)) return null;
        List<int[]> path = Pathfinder.find(map.width(), map.height(),
                new TilePredicate() {
                    @Override public boolean test(int x, int y) { return walkable(x, y); }
                }, a.x(), a.y(), b.x(), b.y());
        if (path == null) return null;
        return net.addRoad(flagIdA, flagIdB, path);
    }

    public boolean connected(int flagId, int dest) {
        return flagId >= 0 && dest >= 0 && net.connected(flagId, dest);
    }

    /** Queue a good at {@code fromFlag} to relay toward {@code destFlag}. */
    public void ship(Good good, int fromFlag, int destFlag, Runnable onArrive) {
        Flag f = net.flag(fromFlag);
        if (f == null) return;
        f.enqueue(new Shipment(good, destFlag, onArrive));
    }

    // ------------------------------------------------------------------ update

    public void update(float dt) {
        advanceCarriers(dt);
        dispatch(dt);
    }

    private void advanceCarriers(float dt) {
        for (Road r : net.roads()) {
            for (Road.Arrival a : r.update(dt)) {
                if (a.flagId == a.shipment.destFlagId()) {
                    a.shipment.deliver();               // reached destination
                } else {
                    net.flag(a.flagId).enqueue(a.shipment); // relay onward
                }
            }
        }
    }

    private void dispatch(float dt) {
        Set<Road> blockedByDemand = new HashSet<Road>();
        for (Flag f : net.flags()) {
            // FIFO: keep loading the head shipment while a free next-hop exists.
            while (f.hasWaiting()) {
                Shipment s = f.peek();
                Road r = net.nextHop(f.id(), s.destFlagId());
                if (r == null) break;              // no route yet
                if (r.free()) {
                    f.poll();
                    r.load(s, f.id());
                } else {
                    blockedByDemand.add(r);        // a shipment wants it, but it's busy
                    break;                         // congestion
                }
            }
        }
        // Roads with pending demand they can't serve accrue congestion and, once it
        // stays high, gain a donkey (second carrier). Idle/served roads relax.
        for (Road r : net.roads()) {
            float c = congestion.containsKey(r) ? congestion.get(r) : 0f;
            if (blockedByDemand.contains(r) && !r.upgraded()) {
                c += dt;
                if (c >= DONKEY_AFTER_SECONDS) { r.addCarrier(); c = 0f; }
            } else {
                c = Math.max(0f, c - dt * 0.5f);
            }
            congestion.put(r, c);
        }
    }
}

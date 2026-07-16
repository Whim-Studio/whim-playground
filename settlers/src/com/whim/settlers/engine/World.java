package com.whim.settlers.engine;

import com.whim.settlers.ai.AiController;
import com.whim.settlers.buildings.Building;
import com.whim.settlers.buildings.BuildingManager;
import com.whim.settlers.buildings.BuildingType;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.map.TileMap;
import com.whim.settlers.military.MilitarySystem;
import com.whim.settlers.military.Players;
import com.whim.settlers.transport.TransportSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root game-state container. Holds the map, camera, and buildings; settlers,
 * roads, and players are added in later phases. Kept deliberately thin so the
 * update/render split stays clear.
 */
public final class World {

    /** The human player's id for this single-player-first build. */
    public static final int PLAYER_ID = 0;

    /** Overall game result, polled by the meta layer each tick while playing. */
    public enum Outcome { ONGOING, VICTORY, DEFEAT }

    private final TileMap map;
    private final Camera camera;
    private final BuildingManager buildings;
    private final TransportSystem transport;
    /** One economy per player id. */
    private final Map<Integer, Economy> economies = new LinkedHashMap<Integer, Economy>();
    private final MilitarySystem military;
    private final List<AiController> ais = new ArrayList<AiController>();

    /** Total simulated time in seconds — handy for animation and debugging. */
    private double clock;

    public World(TileMap map) {
        this.map = map;
        this.camera = new Camera(map.width() / 2.0, map.height() / 2.0);
        this.buildings = new BuildingManager(map);
        this.transport = new TransportSystem(map, buildings);
        this.military = new MilitarySystem(map, buildings, economies);
    }

    /** Advance the simulation by a fixed timestep. */
    public void update(double dtSeconds) {
        clock += dtSeconds;
        float dt = (float) dtSeconds;
        buildings.update(dt);
        // Every building gets a flag next to it as soon as it exists, so it can be
        // road-connected.
        for (Building b : buildings.all()) transport.ensureFlagFor(b);
        for (Economy e : economies.values()) e.update(dt); // may enqueue shipments
        transport.update(dt);  // moves carriers / delivers
        military.update(dt);   // knights, territory, combat
        for (AiController ai : ais) ai.update(dt);
        camera.clampTo(map.width(), map.height());
    }

    /**
     * Found the settlement by placing the Castle on the nearest buildable spot
     * to the map centre, and centre the camera on it. Returns true on success.
     * (Phase 7 will replace this with an interactive founding step.)
     */
    public boolean foundSettlement() {
        int cx = map.width() / 2, cy = map.height() / 2;
        int[] spot = nearestBuildable(BuildingType.CASTLE, cx, cy);
        return spot != null && foundSettlementAt(spot[0], spot[1]);
    }

    /**
     * Interactive founding (Phase 7): place the human Castle at the player-chosen
     * tile if it is a valid Castle site, wire up its stockpile/economy/garrison,
     * and centre the camera on it. Returns false if the tile is not buildable.
     */
    public boolean foundSettlementAt(int ax, int ay) {
        if (economies.containsKey(PLAYER_ID)) return false; // already founded
        if (!buildings.canPlace(BuildingType.CASTLE, ax, ay)) return false;
        Building castle = buildings.place(BuildingType.CASTLE, ax, ay, PLAYER_ID);
        transport.registerCastle(castle); // Castle flag = stockpile hub
        economies.put(PLAYER_ID, new Economy(map, buildings, transport, PLAYER_ID));
        military.seedGarrison(castle, 1, 1); // HQ starts with a knight
        camera.centreOn(ax + 1.5, ay + 1.5);
        return true;
    }

    /** Is {@code (ax,ay)} a valid spot to found the human Castle right now? */
    public boolean canFoundAt(int ax, int ay) {
        return buildings.canPlace(BuildingType.CASTLE, ax, ay);
    }

    /** Whether any valid Castle site exists at all (robustness guard for setup). */
    public boolean hasAnyCastleSite() {
        return nearestBuildable(BuildingType.CASTLE, map.width() / 2, map.height() / 2) != null;
    }

    /**
     * Spawn {@code aggressions.length} AI opponents (player ids 1..n), each with a
     * Castle placed well away from the human and from one another, a seeded
     * garrison, its own economy over the shared road network, and an {@link
     * AiController} tuned to the given peaceful↔aggressive personality. AIs play by
     * the same public systems the human UI drives. Returns the number spawned.
     */
    public int spawnAiPlayers(float[] aggressions) {
        if (aggressions == null) return 0;
        int spawned = 0;
        for (int i = 0; i < aggressions.length; i++) {
            int player = i + 1;
            int[] seat = aiSeat(player, aggressions.length);
            Building castle = placeNearest(BuildingType.CASTLE, seat[0], seat[1], player);
            if (castle == null) continue;
            transport.registerCastle(castle);
            economies.put(player, new Economy(map, buildings, transport, player));
            military.seedGarrison(castle, 2, 2);
            ais.add(new AiController(this, player, aggressions[i]));
            spawned++;
        }
        return spawned;
    }

    /**
     * A spread-out starting seat for AI {@code player}: the human founds near the
     * centre, so AIs take positions around the map rim, distributed by index.
     */
    private int[] aiSeat(int player, int aiCount) {
        double cx = map.width() / 2.0, cy = map.height() / 2.0;
        double radius = Math.min(map.width(), map.height()) * 0.38;
        // Start opposite the map centre and fan the rest evenly around the ring.
        double angle = Math.PI + (player - 1) * (2 * Math.PI / Math.max(1, aiCount + 1));
        int x = (int) Math.round(cx + Math.cos(angle) * radius);
        int y = (int) Math.round(cy + Math.sin(angle) * radius);
        x = Math.max(2, Math.min(map.width() - 3, x));
        y = Math.max(2, Math.min(map.height() - 3, y));
        return new int[] { x, y };
    }

    /**
     * Evaluate the win/lose condition (GDD: control the whole map — every rival
     * eliminated). A player is eliminated once it holds no Castle. Defeat if the
     * human has lost its Castle; victory if there were rivals and none survives.
     */
    public Outcome checkOutcome() {
        if (!ownsCastle(PLAYER_ID)) return Outcome.DEFEAT;
        boolean anyRival = false;
        for (AiController ai : ais) {
            if (ownsCastle(ai.player())) anyRival = true;
        }
        if (!ais.isEmpty() && !anyRival) return Outcome.VICTORY;
        return Outcome.ONGOING;
    }

    private boolean ownsCastle(int player) {
        for (Building b : buildings.all()) {
            if (b.ownerId() == player && b.type() == BuildingType.CASTLE) return true;
        }
        return false;
    }

    /** Nearest tile to (cx,cy) where {@code type} can be placed, or null. */
    private int[] nearestBuildable(BuildingType type, int cx, int cy) {
        int maxR = Math.max(map.width(), map.height());
        for (int r = 0; r < maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int ax = cx + dx, ay = cy + dy;
                    if (buildings.canPlace(type, ax, ay)) return new int[] { ax, ay };
                }
            }
        }
        return null;
    }

    /**
     * Place an enemy settlement (Castle + a Guard Hut) away from the human, with a
     * seeded garrison so it holds territory. The enemy is static until Phase 6's
     * AI takes it over. Returns true if an enemy castle was placed.
     */
    public boolean spawnEnemy() {
        int ecx = map.width() * 3 / 4, ecy = map.height() * 3 / 4;
        Building castle = placeNearest(BuildingType.CASTLE, ecx, ecy, Players.ENEMY);
        if (castle == null) return false;
        transport.registerCastle(castle);
        economies.put(Players.ENEMY, new Economy(map, buildings, transport, Players.ENEMY));
        military.seedGarrison(castle, 2, 2);
        military.setKnightTarget(Players.ENEMY, 8);
        // The AI opponent plays by the same rules (build economy, road, garrison,
        // attack) — see AiController.
        ais.add(new AiController(this, Players.ENEMY));
        return true;
    }

    /** Place a building on the nearest valid tile to (px,py) for the given owner. */
    private Building placeNearest(BuildingType type, int px, int py, int owner) {
        int maxR = Math.max(map.width(), map.height());
        for (int r = 0; r < maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int ax = px + dx, ay = py + dy;
                    if (buildings.canPlace(type, ax, ay)) {
                        return buildings.place(type, ax, ay, owner);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Territory-aware placement check for the interactive UI: terrain-valid, and
     * either inside the player's borders (ordinary buildings) or on/adjacent to
     * them (military buildings, which push the border outward). The Castle is
     * exempt (it founds the first territory).
     */
    public boolean canPlayerPlace(BuildingType type, int ax, int ay, int player) {
        if (!buildings.canPlace(type, ax, ay)) return false;
        if (type == BuildingType.CASTLE) return true;
        if (MilitarySystem.isFort(type)) return military.adjacentToTerritory(player, type, ax, ay);
        return military.withinTerritory(player, type, ax, ay);
    }

    public TileMap map()             { return map; }
    public Camera camera()           { return camera; }
    public BuildingManager buildings(){ return buildings; }
    public TransportSystem transport(){ return transport; }
    public MilitarySystem military() { return military; }
    public double clock()            { return clock; }

    /** The human player's economy (convenience). */
    public Economy economy()          { return economies.get(PLAYER_ID); }
    public Economy economyOf(int p)   { return economies.get(p); }
}

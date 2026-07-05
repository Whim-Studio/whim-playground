package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.AiState;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.NodeType;
import com.whim.kenshi.api.Enums.OrderType;
import com.whim.kenshi.api.Enums.Phase;
import com.whim.kenshi.api.Enums.Relation;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.api.Enums.Terrain;
import com.whim.kenshi.api.Enums.WeaponClass;
import com.whim.kenshi.api.Views;
import com.whim.kenshi.domain.Character;
import com.whim.kenshi.domain.FactionMatrix;
import com.whim.kenshi.domain.MapGrid;
import com.whim.kenshi.domain.Squad;
import com.whim.kenshi.domain.WorldNode;
import com.whim.kenshi.domain.WorldState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Immutable implementations of every {@link Views} interface. The tick thread
 * builds one {@code GameStateView} per completed tick from the live
 * {@link WorldState}; the EDT reads only the published, frozen snapshot so it
 * never observes a half-updated world and never touches domain objects.
 */
final class Snapshot {

    private Snapshot() {}

    private static final BodyPart[] PARTS = BodyPart.values();
    private static final SkillType[] SKILLS = SkillType.values();
    private static final FactionId[] FACTIONS = FactionId.values();

    // ---- Map (built once; terrain is static) ------------------------------
    static final class ImmutableMapView implements Views.MapView {
        private final int tiles;
        private final double tileSize;
        private final Terrain[] grid; // row-major: col + row*tiles

        ImmutableMapView(MapGrid map) {
            this.tiles = Config.MAP_TILES;
            this.tileSize = Config.TILE_SIZE;
            this.grid = new Terrain[tiles * tiles];
            for (int row = 0; row < tiles; row++) {
                for (int col = 0; col < tiles; col++) {
                    grid[col + row * tiles] = map.terrain(col, row);
                }
            }
        }

        public int tiles() { return tiles; }
        public double tileSize() { return tileSize; }
        public Terrain terrain(int col, int row) {
            if (col < 0 || row < 0 || col >= tiles || row >= tiles) {
                return Terrain.WATER;
            }
            return grid[col + row * tiles];
        }
    }

    static final class ImmutableCharacterView implements Views.CharacterView {
        private final String id;
        private final String name;
        private final FactionId faction;
        private final double x;
        private final double y;
        private final double heading;
        private final MoveState moveState;
        private final AiState aiState;
        private final boolean selected;
        private final boolean playerControlled;
        private final boolean alive;
        private final boolean downed;
        private final double[] partHp;
        private final double[] partMax;
        private final boolean[] partDisabled;
        private final double hunger;
        private final double blood;
        private final double bleedRate;
        private final WeaponClass weapon;
        private final int[] skills;
        private final OrderType orderType;
        private final String targetId;

        ImmutableCharacterView(Character c, boolean moving) {
            this.id = c.id();
            this.name = c.name();
            this.faction = c.faction();
            this.x = c.x();
            this.y = c.y();
            this.heading = c.heading();
            MoveState ms = c.moveState();
            // The domain only reports MOVING for a live player MOVE order; the
            // engine overrides IDLE -> MOVING for any character it is actively
            // pathing this tick (NPC wander/pursue/patrol), as the domain invites.
            if (ms == MoveState.IDLE && moving) {
                ms = MoveState.MOVING;
            }
            this.moveState = ms;
            this.aiState = c.aiState();
            this.selected = c.selected();
            this.playerControlled = c.faction() == FactionId.PLAYER;
            this.alive = this.moveState != MoveState.DEAD;
            this.downed = this.moveState == MoveState.DOWNED;
            this.partHp = new double[PARTS.length];
            this.partMax = new double[PARTS.length];
            this.partDisabled = new boolean[PARTS.length];
            for (int i = 0; i < PARTS.length; i++) {
                this.partHp[i] = c.anatomy().hp(PARTS[i]);
                this.partMax[i] = c.anatomy().max(PARTS[i]);
                this.partDisabled[i] = c.anatomy().disabled(PARTS[i]);
            }
            this.hunger = c.hunger();
            this.blood = c.blood();
            this.bleedRate = c.bleedRate();
            this.weapon = c.effectiveWeapon();
            this.skills = new int[SKILLS.length];
            for (int i = 0; i < SKILLS.length; i++) {
                this.skills[i] = c.skills().level(SKILLS[i]);
            }
            this.orderType = c.orderType();
            this.targetId = c.targetId();
        }

        public String id() { return id; }
        public String name() { return name; }
        public FactionId faction() { return faction; }
        public double x() { return x; }
        public double y() { return y; }
        public double heading() { return heading; }
        public MoveState moveState() { return moveState; }
        public AiState aiState() { return aiState; }
        public boolean selected() { return selected; }
        public boolean playerControlled() { return playerControlled; }
        public boolean alive() { return alive; }
        public boolean downed() { return downed; }
        public double partHp(BodyPart part) { return partHp[part.ordinal()]; }
        public double partMax(BodyPart part) { return partMax[part.ordinal()]; }
        public boolean partDisabled(BodyPart part) { return partDisabled[part.ordinal()]; }
        public double hunger() { return hunger; }
        public double hungerMax() { return Config.HUNGER_MAX; }
        public double blood() { return blood; }
        public double bloodMax() { return Config.BLOOD_MAX; }
        public double bleedRate() { return bleedRate; }
        public WeaponClass weapon() { return weapon; }
        public int skill(SkillType s) { return skills[s.ordinal()]; }
        public OrderType orderType() { return orderType; }
        public String targetId() { return targetId; }
    }

    static final class ImmutableSquadView implements Views.SquadView {
        private final String id;
        private final String name;
        private final FactionId faction;
        private final List<String> members;

        ImmutableSquadView(Squad s) {
            this.id = s.id();
            this.name = s.name();
            this.faction = s.faction();
            this.members = Collections.unmodifiableList(new ArrayList<String>(s.memberIds()));
        }

        public String id() { return id; }
        public String name() { return name; }
        public FactionId faction() { return faction; }
        public List<String> memberIds() { return members; }
    }

    static final class ImmutableFactionView implements Views.FactionView {
        private final FactionId id;
        private final Relation[] relations; // by other.ordinal()
        private final int reputation;

        ImmutableFactionView(FactionId id, FactionMatrix fm) {
            this.id = id;
            this.relations = new Relation[FACTIONS.length];
            for (int i = 0; i < FACTIONS.length; i++) {
                this.relations[i] = fm.relation(id, FACTIONS[i]);
            }
            this.reputation = fm.reputationWithPlayer(id);
        }

        public FactionId id() { return id; }
        public String label() { return id.label(); }
        public Relation relationTo(FactionId other) { return relations[other.ordinal()]; }
        public int reputationWithPlayer() { return reputation; }
    }

    static final class ImmutableNodeView implements Views.NodeView {
        private final String id;
        private final String name;
        private final NodeType type;
        private final FactionId owner;
        private final double x;
        private final double y;
        private final double radius;

        ImmutableNodeView(WorldNode n) {
            this.id = n.id();
            this.name = n.name();
            this.type = n.type();
            this.owner = n.owner();
            this.x = n.x();
            this.y = n.y();
            this.radius = n.radius();
        }

        public String id() { return id; }
        public String name() { return name; }
        public NodeType type() { return type; }
        public FactionId owner() { return owner; }
        public double x() { return x; }
        public double y() { return y; }
        public double radius() { return radius; }
    }

    static final class ImmutableLogView implements Views.LogView {
        private final long tick;
        private final String text;
        ImmutableLogView(long tick, String text) {
            this.tick = tick;
            this.text = text;
        }
        public long tick() { return tick; }
        public String text() { return text; }
    }

    static final class ImmutableGameStateView implements Views.GameStateView {
        private final Phase phase;
        private final long tick;
        private final double worldSeconds;
        private final int gameSpeed;
        private final Views.MapView map;
        private final List<Views.CharacterView> characters;
        private final List<Views.SquadView> squads;
        private final List<Views.FactionView> factions;
        private final List<Views.NodeView> nodes;
        private final List<String> selectedIds;
        private final List<Views.LogView> log;

        ImmutableGameStateView(Phase phase, long tick, double worldSeconds, int gameSpeed,
                               Views.MapView map,
                               List<Views.CharacterView> characters,
                               List<Views.SquadView> squads,
                               List<Views.FactionView> factions,
                               List<Views.NodeView> nodes,
                               List<String> selectedIds,
                               List<Views.LogView> log) {
            this.phase = phase;
            this.tick = tick;
            this.worldSeconds = worldSeconds;
            this.gameSpeed = gameSpeed;
            this.map = map;
            this.characters = Collections.unmodifiableList(characters);
            this.squads = Collections.unmodifiableList(squads);
            this.factions = Collections.unmodifiableList(factions);
            this.nodes = Collections.unmodifiableList(nodes);
            this.selectedIds = Collections.unmodifiableList(selectedIds);
            this.log = Collections.unmodifiableList(log);
        }

        public Phase phase() { return phase; }
        public long tick() { return tick; }
        public double worldSeconds() { return worldSeconds; }
        public int gameSpeed() { return gameSpeed; }
        public Views.MapView map() { return map; }
        public List<Views.CharacterView> characters() { return characters; }
        public List<Views.SquadView> squads() { return squads; }
        public List<Views.FactionView> factions() { return factions; }
        public List<Views.NodeView> nodes() { return nodes; }
        public List<String> selectedIds() { return selectedIds; }
        public List<Views.LogView> log() { return log; }
    }

    /**
     * Build a full frozen snapshot from the live world plus the engine-owned
     * clock/phase/log. {@code mapView} is shared across frames (terrain static).
     */
    static Views.GameStateView build(WorldState world, Views.MapView mapView,
                                     Phase phase, long tick, double worldSeconds,
                                     int gameSpeed, List<String> selectedIds,
                                     List<EventLog.Line> logLines, Set<String> movingIds) {
        List<Character> chars = world.charactersList();
        List<Views.CharacterView> cvs = new ArrayList<Views.CharacterView>(chars.size());
        for (int i = 0; i < chars.size(); i++) {
            Character c = chars.get(i);
            boolean moving = movingIds != null && movingIds.contains(c.id());
            cvs.add(new ImmutableCharacterView(c, moving));
        }

        List<Squad> squads = world.squadsList();
        List<Views.SquadView> svs = new ArrayList<Views.SquadView>(squads.size());
        for (int i = 0; i < squads.size(); i++) {
            svs.add(new ImmutableSquadView(squads.get(i)));
        }

        FactionMatrix fm = world.factions();
        List<Views.FactionView> fvs = new ArrayList<Views.FactionView>(FACTIONS.length);
        for (int i = 0; i < FACTIONS.length; i++) {
            fvs.add(new ImmutableFactionView(FACTIONS[i], fm));
        }

        List<WorldNode> nodes = world.nodesList();
        List<Views.NodeView> nvs = new ArrayList<Views.NodeView>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            nvs.add(new ImmutableNodeView(nodes.get(i)));
        }

        List<String> sel = new ArrayList<String>(selectedIds);

        List<Views.LogView> logs = new ArrayList<Views.LogView>(logLines.size());
        for (int i = 0; i < logLines.size(); i++) {
            EventLog.Line ln = logLines.get(i);
            logs.add(new ImmutableLogView(ln.tick, ln.text));
        }

        return new ImmutableGameStateView(phase, tick, worldSeconds, gameSpeed, mapView,
                cvs, svs, fvs, nvs, sel, logs);
    }
}

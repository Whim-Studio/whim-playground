package com.whim.cardwoven.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.whim.cardwoven.api.ActionResult;
import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.GamePhase;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.AttachmentView;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.MapView;
import com.whim.cardwoven.api.Views.PlayerView;
import com.whim.cardwoven.api.Views.TileView;

/**
 * DEV-ONLY fake {@link GameController} living in the UI package. It hand-builds a
 * small but fully-shaped {@link GameStateView} — a 10x8 map, a couple of starting
 * buildings, a playable hand, resources and victory progress — and implements the
 * action methods with just enough behaviour that the interface feels alive:
 * placing buildings, attaching units, playing economy/explore cards, resolving
 * combat, and ending turns (yields + redraw). It is NOT the real ruleset; the
 * final {@code Main} swaps in the engine and this class is dropped.
 *
 * It implements the {@code api.Views} interfaces with its own private classes so
 * it stays entirely within the allowed imports (api + java.util). No domain or
 * engine types are referenced.
 */
public class StubController implements GameController {

    private final Random rnd = new Random(1337L);
    private int nextCardId = 1;
    private int nextBuildingId = 1;
    private SState state;

    public StubController() {
        newGame(Faction.LANDS_OF_THE_KING);
    }

    // =====================================================================
    // GameController API
    // =====================================================================

    @Override
    public GameStateView state() { return state; }

    @Override
    public void newGame(Faction humanFaction) {
        nextCardId = 1;
        nextBuildingId = 1;
        state = new SState();
        state.map = buildMap();
        SPlayer human = new SPlayer(0, humanFaction, humanFaction.display() + " (You)", true);
        SPlayer ai = new SPlayer(1, otherFaction(humanFaction), "Rival AI", false);
        state.players.add(human);
        state.players.add(ai);
        state.currentPlayerIndex = 0;
        state.turnNumber = 1;
        state.phase = GamePhase.MAIN;

        human.resources.put(ResourceType.GOLD, 9);
        human.resources.put(ResourceType.COMMAND_POINTS, 3);
        human.deckCount = 14;
        human.discardCount = 0;
        human.pursuable = victoriesFor(humanFaction);

        // A couple of pre-placed buildings so the map isn't empty.
        SBuilding city = placeBuilding(BuildingType.CITY, 2, 3, 0, 3);
        city.attachments.add(makeAttachment(AttachmentType.WORKER));
        SBuilding temple = placeBuilding(BuildingType.TEMPLE, 5, 6, 0, 2);
        temple.attachments.add(makeAttachment(AttachmentType.IDOL));

        // Some raiders to fight.
        state.map.grid[6][2].raiderStrength = 3;
        state.map.grid[3][8].raiderStrength = 5;

        // Starting hand for the faction.
        human.hand.clear();
        human.hand.addAll(startingHand(humanFaction));

        recomputeVictory();
        log("A new age dawns for " + humanFaction.display() + ".");
    }

    @Override
    public ActionResult playBuilding(int cardId, int row, int col) {
        SPlayer me = current();
        SCard card = takeFromHand(me, cardId);
        if (card == null) return ActionResult.fail("Card not in hand.");
        if (card.type != CardType.BUILDING || card.buildingType == null) {
            me.hand.add(card);
            return ActionResult.fail(card.name + " is not a building card.");
        }
        if (!inBounds(row, col)) { me.hand.add(card); return ActionResult.fail("Off the map."); }
        STile tile = state.map.grid[row][col];
        if (tile.building != null) { me.hand.add(card); return ActionResult.fail("Tile already occupied."); }
        if (tile.terrain == TerrainType.WATER && card.buildingType != BuildingType.PORT) {
            me.hand.add(card);
            return ActionResult.fail("Only a Port can sit on water.");
        }
        if (card.buildingType == BuildingType.PORT && !waterAdjacent(row, col)) {
            me.hand.add(card);
            return ActionResult.fail("A Port must be next to water.");
        }
        if (!spend(me, ResourceType.GOLD, card.cost)) {
            me.hand.add(card);
            return ActionResult.fail("Not enough gold (need " + card.cost + ").");
        }
        SBuilding b = placeBuilding(card.buildingType, row, col, 0, capacityFor(card.buildingType));
        tile.explored = true;
        me.discardCount++;
        recomputeVictory();
        log("Built a " + card.buildingType.display() + " at (" + row + "," + col + ").");
        return ActionResult.ok("Built " + card.buildingType.display() + ".");
    }

    @Override
    public ActionResult attachCard(int cardId, int buildingId) {
        SPlayer me = current();
        SCard card = takeFromHand(me, cardId);
        if (card == null) return ActionResult.fail("Card not in hand.");
        if (card.type != CardType.ATTACHMENT || card.attachmentType == null) {
            me.hand.add(card);
            return ActionResult.fail(card.name + " is not an attachment.");
        }
        SBuilding b = findBuilding(buildingId);
        if (b == null) { me.hand.add(card); return ActionResult.fail("No such building."); }
        if (b.attachments.size() >= b.capacity) {
            me.hand.add(card);
            return ActionResult.fail(b.type.display() + " is at capacity.");
        }
        if (!legalAttach(card.attachmentType, b.type)) {
            me.hand.add(card);
            return ActionResult.fail(card.attachmentType.display() + " can't attach to a "
                    + b.type.display() + ".");
        }
        if (!spend(me, ResourceType.GOLD, card.cost)) {
            me.hand.add(card);
            return ActionResult.fail("Not enough gold (need " + card.cost + ").");
        }
        b.attachments.add(makeAttachment(card.attachmentType));
        me.discardCount++;
        recomputeVictory();
        log("Attached a " + card.attachmentType.display() + " to the " + b.type.display() + ".");
        return ActionResult.ok("Attached " + card.attachmentType.display() + ".");
    }

    @Override
    public ActionResult playCard(int cardId, int row, int col) {
        SPlayer me = current();
        SCard card = takeFromHand(me, cardId);
        if (card == null) return ActionResult.fail("Card not in hand.");
        if (card.type == CardType.ECONOMY) {
            int gain = Math.max(2, card.attack == 0 ? 3 : card.attack);
            add(me, ResourceType.GOLD, gain);
            me.discardCount++;
            recomputeVictory();
            log(card.name + " yields " + gain + " gold.");
            return ActionResult.ok("+" + gain + " gold.");
        }
        if (card.type == CardType.EXPLORE) {
            if (!inBounds(row, col)) { me.hand.add(card); return ActionResult.fail("Off the map."); }
            int revealed = 0;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = row + dr, c = col + dc;
                    if (inBounds(r, c) && !state.map.grid[r][c].explored) {
                        state.map.grid[r][c].explored = true;
                        revealed++;
                    }
                }
            }
            add(me, ResourceType.COMMAND_POINTS, 1);
            me.discardCount++;
            recomputeVictory();
            log(card.name + " reveals " + revealed + " tile(s).");
            return ActionResult.ok("Explored " + revealed + " tile(s).");
        }
        me.hand.add(card);
        return ActionResult.fail(card.name + " can't be played that way.");
    }

    @Override
    public ActionResult resolveCombat(int cardId, int row, int col) {
        SPlayer me = current();
        if (!inBounds(row, col)) return ActionResult.fail("Off the map.");
        STile tile = state.map.grid[row][col];
        if (tile.raiderStrength <= 0) {
            return ActionResult.fail("No raiders on that tile.");
        }
        SCard card = takeFromHand(me, cardId);
        if (card == null) return ActionResult.fail("Card not in hand.");
        if (card.type != CardType.MILITARY) {
            me.hand.add(card);
            return ActionResult.fail(card.name + " is not a military card.");
        }
        me.discardCount++;
        int before = tile.raiderStrength;
        if (card.attack >= before) {
            tile.raiderStrength = 0;
            me.raidersCleared++;
            add(me, ResourceType.GOLD, 2);
            recomputeVictory();
            log(card.name + " (atk " + card.attack + ") crushes the raiders at ("
                    + row + "," + col + "). +2 gold plunder.");
            return ActionResult.ok("Raiders destroyed!");
        } else {
            tile.raiderStrength = before - card.attack;
            recomputeVictory();
            log(card.name + " (atk " + card.attack + ") wounds the raiders — "
                    + tile.raiderStrength + " strength remains.");
            return ActionResult.ok("Raiders weakened to " + tile.raiderStrength + ".");
        }
    }

    @Override
    public ActionResult endTurn() {
        SPlayer me = current();
        // YIELD phase — accrue from buildings' attachments.
        int gold = 0, command = 0, draws = 0;
        for (SBuilding b : allBuildingsOf(0)) {
            for (SAttachment a : b.attachments) {
                if (a.type == AttachmentType.WORKER) gold += a.yieldAmount;
                else if (a.type == AttachmentType.WITCH) command += a.yieldAmount;
                else if (a.type == AttachmentType.IDOL) draws += a.bonusDraw;
            }
        }
        add(me, ResourceType.GOLD, gold);
        add(me, ResourceType.COMMAND_POINTS, command);

        // Rival AI does something modest so raiders/turn feel alive.
        if (rnd.nextInt(2) == 0) {
            int rr = rnd.nextInt(state.map.rows), rc = rnd.nextInt(state.map.cols);
            STile t = state.map.grid[rr][rc];
            if (t.building == null && t.raiderStrength == 0 && t.terrain != TerrainType.WATER) {
                t.raiderStrength = 2 + rnd.nextInt(4);
            }
        }

        // DRAW phase — refill toward the faction base hand size (+ idol draws).
        int target = me.faction.baseHandSize() + draws;
        int drew = 0;
        while (me.hand.size() < target && me.deckCount > 0) {
            me.hand.add(drawCard(me));
            me.deckCount--;
            drew++;
        }
        if (me.deckCount <= 0 && me.discardCount > 0 && me.hand.size() < target) {
            // Reshuffle discard into deck.
            me.deckCount += me.discardCount;
            me.discardCount = 0;
            log("Discard reshuffled into deck.");
            while (me.hand.size() < target && me.deckCount > 0) {
                me.hand.add(drawCard(me));
                me.deckCount--;
                drew++;
            }
        }

        state.turnNumber++;
        state.phase = GamePhase.MAIN;
        recomputeVictory();
        checkWinner();

        StringBuilder sb = new StringBuilder("Turn ").append(state.turnNumber)
                .append(": +").append(gold).append("g +").append(command).append("cp, drew ")
                .append(drew).append(".");
        log(sb.toString());
        return ActionResult.ok(sb.toString());
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private SPlayer current() { return state.players.get(0); }

    private boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < state.map.rows && c < state.map.cols;
    }

    private boolean waterAdjacent(int r, int c) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (inBounds(nr, nc) && state.map.grid[nr][nc].terrain == TerrainType.WATER) return true;
            }
        }
        return false;
    }

    private boolean legalAttach(AttachmentType a, BuildingType b) {
        if (a == AttachmentType.WORKER) return b == BuildingType.CITY || b == BuildingType.FARM;
        if (a == AttachmentType.IDOL) return b == BuildingType.TEMPLE;
        return b == BuildingType.TEMPLE || b == BuildingType.CITY; // WITCH
    }

    private int capacityFor(BuildingType t) {
        if (t == BuildingType.CITY) return 3;
        if (t == BuildingType.TEMPLE) return 2;
        return 1;
    }

    private SCard takeFromHand(SPlayer me, int cardId) {
        for (int i = 0; i < me.hand.size(); i++) {
            if (me.hand.get(i).id == cardId) return me.hand.remove(i);
        }
        return null;
    }

    private SBuilding findBuilding(int id) {
        for (int r = 0; r < state.map.rows; r++) {
            for (int c = 0; c < state.map.cols; c++) {
                SBuilding b = state.map.grid[r][c].building;
                if (b != null && b.id == id) return b;
            }
        }
        return null;
    }

    private List<SBuilding> allBuildingsOf(int owner) {
        List<SBuilding> out = new ArrayList<SBuilding>();
        for (int r = 0; r < state.map.rows; r++) {
            for (int c = 0; c < state.map.cols; c++) {
                SBuilding b = state.map.grid[r][c].building;
                if (b != null && b.owner == owner) out.add(b);
            }
        }
        return out;
    }

    private boolean spend(SPlayer me, ResourceType t, int amt) {
        int have = me.resources.get(t);
        if (have < amt) return false;
        me.resources.put(t, have - amt);
        return true;
    }

    private void add(SPlayer me, ResourceType t, int amt) {
        me.resources.put(t, me.resources.get(t) + amt);
    }

    private SBuilding placeBuilding(BuildingType type, int row, int col, int owner, int capacity) {
        SBuilding b = new SBuilding();
        b.id = nextBuildingId++;
        b.type = type;
        b.row = row;
        b.col = col;
        b.owner = owner;
        b.capacity = capacity;
        b.defense = 1 + capacity;
        state.map.grid[row][col].building = b;
        state.map.grid[row][col].explored = true;
        return b;
    }

    private SAttachment makeAttachment(AttachmentType t) {
        SAttachment a = new SAttachment();
        a.type = t;
        a.card = makeCard("", CardType.ATTACHMENT, 0, null, t, 0, "");
        if (t == AttachmentType.WORKER) { a.yieldResource = ResourceType.GOLD; a.yieldAmount = 2; }
        else if (t == AttachmentType.WITCH) { a.yieldResource = ResourceType.COMMAND_POINTS; a.yieldAmount = 1; }
        else { a.yieldResource = null; a.yieldAmount = 0; a.bonusDraw = 1; } // IDOL
        return a;
    }

    private SCard drawCard(SPlayer me) {
        // Procedurally vary the drawn card so the hand stays interesting.
        CardType[] pool = new CardType[] {
            CardType.BUILDING, CardType.ATTACHMENT, CardType.MILITARY,
            CardType.ECONOMY, CardType.EXPLORE
        };
        CardType t = pool[rnd.nextInt(pool.length)];
        if (me.faction == Faction.THE_UNFAITHFUL && rnd.nextInt(5) == 0) {
            return makeCard("Sin", CardType.SIN, 0, null, null, 0, "Dead weight — must be discarded.");
        }
        return sampleCard(t);
    }

    private SCard sampleCard(CardType t) {
        if (t == CardType.BUILDING) {
            BuildingType[] bs = BuildingType.values();
            BuildingType b = bs[rnd.nextInt(bs.length)];
            return makeCard(b.display(), CardType.BUILDING, 2 + rnd.nextInt(3), b, null, 0,
                    "Place a " + b.display() + " on the map.");
        }
        if (t == CardType.ATTACHMENT) {
            AttachmentType[] as = AttachmentType.values();
            AttachmentType a = as[rnd.nextInt(as.length)];
            return makeCard(a.display(), CardType.ATTACHMENT, 1 + rnd.nextInt(2), null, a, 0,
                    "Attach a " + a.display() + " for per-turn yield.");
        }
        if (t == CardType.MILITARY) {
            int atk = 2 + rnd.nextInt(5);
            return makeCard("Warband", CardType.MILITARY, 1 + rnd.nextInt(3), null, null, atk,
                    "Strike raiders with attack " + atk + ".");
        }
        if (t == CardType.ECONOMY) {
            int g = 3 + rnd.nextInt(3);
            return makeCard("Levy", CardType.ECONOMY, 0, null, null, g, "Gain " + g + " gold.");
        }
        return makeCard("Scout", CardType.EXPLORE, 0, null, null, 0, "Reveal nearby tiles.");
    }

    private List<SCard> startingHand(Faction f) {
        List<SCard> hand = new ArrayList<SCard>();
        hand.add(makeCard("Fortified City", CardType.BUILDING, 3, BuildingType.CITY, null, 0,
                "Place a City — holds up to 3 attachments."));
        hand.add(makeCard("Grand Temple", CardType.BUILDING, 4, BuildingType.TEMPLE, null, 0,
                "Place a Temple for Idols and Witches."));
        hand.add(makeCard("Worker Gang", CardType.ATTACHMENT, 1, null, AttachmentType.WORKER, 0,
                "Attach to a City for +2 gold/turn."));
        hand.add(makeCard("King's Warband", CardType.MILITARY, 2, null, null, 4,
                "Attack 4 vs raiders."));
        hand.add(makeCard("Royal Levy", CardType.ECONOMY, 0, null, null, 4, "Gain 4 gold."));
        hand.add(makeCard("Pathfinder", CardType.EXPLORE, 0, null, null, 0, "Reveal nearby tiles."));
        if (f == Faction.BABYLON) {
            hand.add(makeCard("Ziggurat", CardType.BUILDING, 2, BuildingType.TEMPLE, null, 0,
                    "Cheap building — Babylon specialty."));
        } else if (f == Faction.THE_UNFAITHFUL) {
            hand.add(makeCard("Dark Pact", CardType.ECONOMY, 0, null, null, 6,
                    "Gain 6 gold — but sows Sin in your deck."));
            hand.add(makeCard("Sin", CardType.SIN, 0, null, null, 0, "Dead weight — must be discarded."));
        } else {
            hand.add(makeCard("Witch Coven", CardType.ATTACHMENT, 2, null, AttachmentType.WITCH, 0,
                    "Attach for +1 command/turn."));
        }
        return hand;
    }

    private SCard makeCard(String name, CardType type, int cost, BuildingType bt,
                           AttachmentType at, int attack, String desc) {
        SCard c = new SCard();
        c.id = nextCardId++;
        c.name = name;
        c.type = type;
        c.cost = cost;
        c.buildingType = bt;
        c.attachmentType = at;
        c.attack = attack;
        c.description = desc;
        return c;
    }

    private SMap buildMap() {
        int rows = 8, cols = 10;
        SMap m = new SMap();
        m.rows = rows;
        m.cols = cols;
        m.grid = new STile[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                STile t = new STile();
                t.row = r;
                t.col = c;
                t.terrain = terrainAt(r, c, rows, cols);
                // Reveal the central band; leave the fringes as fog to explore.
                t.explored = c >= 1 && c <= cols - 2 && r >= 1 && r <= rows - 2;
                m.grid[r][c] = t;
            }
        }
        return m;
    }

    private TerrainType terrainAt(int r, int c, int rows, int cols) {
        if (c >= cols - 2) return TerrainType.WATER;            // eastern sea
        if (c <= 1 && r % 3 == 0) return TerrainType.MOUNTAIN;  // western range
        if (r == rows - 1) return TerrainType.DESERT;           // southern sands
        if ((r + c) % 4 == 0) return TerrainType.FOREST;
        if ((r * 3 + c) % 5 == 0) return TerrainType.MOUNTAIN;
        return TerrainType.PLAINS;
    }

    private void recomputeVictory() {
        SPlayer me = current();
        int gold = me.resources.get(ResourceType.GOLD);
        int command = me.resources.get(ResourceType.COMMAND_POINTS);
        List<SBuilding> mine = allBuildingsOf(0);
        int buildings = mine.size();
        int temples = 0, idols = 0;
        for (SBuilding b : mine) {
            if (b.type == BuildingType.TEMPLE) temples++;
            for (SAttachment a : b.attachments) if (a.type == AttachmentType.IDOL) idols++;
        }
        me.progress.put(VictoryType.ECONOMIC, clamp(gold / 40.0));
        me.progress.put(VictoryType.MILITARY, clamp(me.raidersCleared / 5.0));
        me.progress.put(VictoryType.EXPANSION, clamp(buildings / 8.0));
        me.progress.put(VictoryType.FAITH, clamp((temples + idols) / 6.0));
        me.progress.put(VictoryType.DOMINANCE, clamp(command / 20.0));
    }

    private void checkWinner() {
        SPlayer me = current();
        for (VictoryType vt : me.pursuable) {
            Double p = me.progress.get(vt);
            if (p != null && p >= 1.0) {
                state.gameOver = true;
                state.winnerIndex = 0;
                state.winningVictory = vt;
                log("★ " + me.faction.display() + " achieves a " + vt.display() + " victory!");
                return;
            }
        }
    }

    private double clamp(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private List<VictoryType> victoriesFor(Faction f) {
        if (f == Faction.BABYLON) {
            return Arrays.asList(VictoryType.EXPANSION, VictoryType.ECONOMIC, VictoryType.FAITH);
        }
        if (f == Faction.THE_UNFAITHFUL) {
            return Arrays.asList(VictoryType.MILITARY, VictoryType.DOMINANCE, VictoryType.ECONOMIC);
        }
        return Arrays.asList(VictoryType.ECONOMIC, VictoryType.MILITARY,
                VictoryType.EXPANSION, VictoryType.FAITH, VictoryType.DOMINANCE);
    }

    private Faction otherFaction(Faction f) {
        for (Faction x : Faction.values()) if (x != f) return x;
        return f;
    }

    private void log(String msg) {
        state.log.add(msg);
        while (state.log.size() > 40) state.log.remove(0);
    }

    // =====================================================================
    // View implementations (mutable, package-private POJOs)
    // =====================================================================

    private static final class SCard implements CardView {
        int id; String name; CardType type; int cost;
        BuildingType buildingType; AttachmentType attachmentType; int attack; String description;
        public int id() { return id; }
        public String name() { return name; }
        public CardType type() { return type; }
        public int cost() { return cost; }
        public BuildingType buildingType() { return buildingType; }
        public AttachmentType attachmentType() { return attachmentType; }
        public int attack() { return attack; }
        public String description() { return description; }
    }

    private static final class SAttachment implements AttachmentView {
        SCard card; AttachmentType type; ResourceType yieldResource; int yieldAmount; int bonusDraw;
        public CardView card() { return card; }
        public AttachmentType type() { return type; }
        public ResourceType yieldResource() { return yieldResource; }
        public int yieldAmount() { return yieldAmount; }
        public int bonusDraw() { return bonusDraw; }
    }

    private static final class SBuilding implements BuildingView {
        int id; BuildingType type; int row; int col; int owner; int defense; int capacity;
        final List<SAttachment> attachments = new ArrayList<SAttachment>();
        public int id() { return id; }
        public BuildingType type() { return type; }
        public int row() { return row; }
        public int col() { return col; }
        public int ownerPlayerIndex() { return owner; }
        public int defense() { return defense; }
        public List<AttachmentView> attachments() {
            return new ArrayList<AttachmentView>(attachments);
        }
        public int attachmentCapacity() { return capacity; }
    }

    private static final class STile implements TileView {
        int row; int col; TerrainType terrain; boolean explored; SBuilding building; int raiderStrength;
        public int row() { return row; }
        public int col() { return col; }
        public TerrainType terrain() { return terrain; }
        public boolean explored() { return explored; }
        public BuildingView building() { return building; }
        public int raiderStrength() { return raiderStrength; }
    }

    private final class SMap implements MapView {
        int rows; int cols; STile[][] grid;
        public int rows() { return rows; }
        public int cols() { return cols; }
        public TileView tile(int row, int col) { return grid[row][col]; }
        public List<BuildingView> buildingsOf(int playerIndex) {
            List<BuildingView> out = new ArrayList<BuildingView>();
            for (SBuilding b : allBuildingsOf(playerIndex)) out.add(b);
            return out;
        }
    }

    private static final class SPlayer implements PlayerView {
        final int index; final Faction faction; final String name; final boolean human;
        final Map<ResourceType, Integer> resources = new HashMap<ResourceType, Integer>();
        final List<SCard> hand = new ArrayList<SCard>();
        final Map<VictoryType, Double> progress = new HashMap<VictoryType, Double>();
        List<VictoryType> pursuable = new ArrayList<VictoryType>();
        int deckCount; int discardCount; int raidersCleared;

        SPlayer(int index, Faction faction, String name, boolean human) {
            this.index = index; this.faction = faction; this.name = name; this.human = human;
            resources.put(ResourceType.GOLD, 0);
            resources.put(ResourceType.COMMAND_POINTS, 0);
        }
        public int index() { return index; }
        public Faction faction() { return faction; }
        public String name() { return name; }
        public boolean isHuman() { return human; }
        public int resource(ResourceType type) {
            Integer v = resources.get(type); return v == null ? 0 : v;
        }
        public int deckCount() { return deckCount; }
        public int discardCount() { return discardCount; }
        public int handSize() { return hand.size(); }
        public List<CardView> hand() { return new ArrayList<CardView>(hand); }
        public double victoryProgress(VictoryType type) {
            Double v = progress.get(type); return v == null ? 0.0 : v;
        }
        public List<VictoryType> pursuableVictories() { return pursuable; }
    }

    private final class SState implements GameStateView {
        SMap map;
        final List<SPlayer> players = new ArrayList<SPlayer>();
        int currentPlayerIndex; int turnNumber; GamePhase phase = GamePhase.MAIN;
        boolean gameOver; int winnerIndex = -1; VictoryType winningVictory;
        final List<String> log = new ArrayList<String>();

        public MapView map() { return map; }
        public List<PlayerView> players() { return new ArrayList<PlayerView>(players); }
        public int currentPlayerIndex() { return currentPlayerIndex; }
        public PlayerView currentPlayer() { return players.get(currentPlayerIndex); }
        public int turnNumber() { return turnNumber; }
        public GamePhase phase() { return phase; }
        public boolean isGameOver() { return gameOver; }
        public int winnerPlayerIndex() { return winnerIndex; }
        public VictoryType winningVictory() { return winningVictory; }
        public List<String> recentLog() { return new ArrayList<String>(log); }
    }
}

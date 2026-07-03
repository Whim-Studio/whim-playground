package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.GamePhase;
import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.MapView;
import com.whim.cardwoven.api.Views.PlayerView;

/**
 * The whole-game model root. Holds the map, the players, the turn/phase cursor,
 * the winner, and a rolling event log. Implements the read-only
 * {@link GameStateView} the UI renders and exposes concrete accessors the engine
 * mutates.
 *
 * <p>Use {@link #create(Faction, long)} to build a fresh, fully-seeded game.</p>
 */
public final class GameState implements GameStateView {

    /** Map dimensions: landscape 10 wide (cols) x 8 tall (rows). */
    public static final int ROWS = 8;
    public static final int COLS = 10;

    private static final int LOG_CAP = 200;

    private final GridMap gridMap;
    private final List<PlayerState> players;
    private final Random random;

    private int currentPlayerIndex;
    private int turnNumber;
    private GamePhase phase;
    private boolean gameOver;
    private int winnerPlayerIndex;
    private VictoryType winningVictory;
    private final List<String> recentLog = new ArrayList<String>();

    private GameState(GridMap gridMap, List<PlayerState> players, Random random) {
        this.gridMap = gridMap;
        this.players = players;
        this.random = random;
        this.currentPlayerIndex = 0;
        this.turnNumber = 1;
        this.phase = GamePhase.DRAW;
        this.gameOver = false;
        this.winnerPlayerIndex = -1;
        this.winningVictory = null;
    }

    /**
     * Build a fresh game: an 8x10 map with seeded terrain and raiders, the human
     * faction as player 0 plus two AI opponents (the other two factions), each
     * with a shuffled starting deck, starting resources, and an opening hand
     * dealt to its base hand size.
     */
    public static GameState create(Faction human, long seed) {
        Random rng = new Random(seed);
        IdGenerator buildingIds = new IdGenerator(1);
        GridMap map = new GridMap(ROWS, COLS, rng, buildingIds);
        seedRaiders(map, rng);

        List<PlayerState> players = new ArrayList<PlayerState>();
        Faction[] order = playerFactions(human);
        for (int i = 0; i < order.length; i++) {
            Faction f = order[i];
            boolean isHuman = (i == 0);
            String name = isHuman ? "You (" + f.display() + ")" : f.display();
            Deck deck = new Deck(rng, CardLibrary.startingDeck(f));
            deck.shuffle();
            PlayerState p = new PlayerState(i, f, name, isHuman, deck,
                    new DiscardPile());
            // deal opening hand to base hand size
            int handTarget = p.profile().baseHandSize();
            for (int h = 0; h < handTarget; h++) {
                p.drawOne();
            }
            players.add(p);
        }

        GameState gs = new GameState(map, players, rng);
        gs.log("A new age dawns. " + human.display() + " takes the field.");
        return gs;
    }

    /** Human first, then the remaining two factions in enum order. */
    private static Faction[] playerFactions(Faction human) {
        Faction[] all = Faction.values();
        Faction[] out = new Faction[3];
        out[0] = human;
        int idx = 1;
        for (int i = 0; i < all.length; i++) {
            if (all[i] != human) {
                out[idx++] = all[i];
            }
        }
        return out;
    }

    /** Scatter Orcish raiders across a handful of land tiles. */
    private static void seedRaiders(GridMap map, Random rng) {
        int target = Math.max(4, (map.rows() * map.cols()) / 12);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = target * 20;
        while (placed < target && attempts < maxAttempts) {
            attempts++;
            int r = rng.nextInt(map.rows());
            int c = rng.nextInt(map.cols());
            Tile t = map.tileAt(r, c);
            if (t == null || t.terrain() == TerrainType.WATER
                    || t.raiderStrength() > 0) {
                continue;
            }
            t.setRaiderStrength(2 + rng.nextInt(4)); // 2..5
            placed++;
        }
    }

    // --- concrete accessors (engine) ---
    public GridMap gridMap() { return gridMap; }
    public List<PlayerState> playerStates() { return players; }

    public PlayerState playerAt(int index) {
        if (index < 0 || index >= players.size()) {
            return null;
        }
        return players.get(index);
    }

    public PlayerState current() { return players.get(currentPlayerIndex); }

    public Random random() { return random; }

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public void setWinner(int playerIndex, VictoryType victory) {
        this.winnerPlayerIndex = playerIndex;
        this.winningVictory = victory;
        this.gameOver = playerIndex >= 0;
    }

    /** Append a line to the rolling event log (oldest trimmed past the cap). */
    public void log(String message) {
        if (message == null) {
            return;
        }
        recentLog.add(message);
        while (recentLog.size() > LOG_CAP) {
            recentLog.remove(0);
        }
    }

    // --- GameStateView ---
    public MapView map() { return gridMap; }

    public List<PlayerView> players() {
        return Collections.<PlayerView>unmodifiableList(
                new ArrayList<PlayerView>(players));
    }

    public int currentPlayerIndex() { return currentPlayerIndex; }

    public PlayerView currentPlayer() { return current(); }

    public int turnNumber() { return turnNumber; }

    public GamePhase phase() { return phase; }

    public boolean isGameOver() { return gameOver; }

    public int winnerPlayerIndex() { return winnerPlayerIndex; }

    public VictoryType winningVictory() { return winningVictory; }

    public List<String> recentLog() {
        return Collections.unmodifiableList(new ArrayList<String>(recentLog));
    }
}

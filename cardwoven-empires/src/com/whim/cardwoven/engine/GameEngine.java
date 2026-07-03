package com.whim.cardwoven.engine;

import java.util.List;

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
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.TileView;
import com.whim.cardwoven.domain.Attachment;
import com.whim.cardwoven.domain.Building;
import com.whim.cardwoven.domain.Card;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.GridMap;
import com.whim.cardwoven.domain.PlayerState;
import com.whim.cardwoven.domain.Resources;

/**
 * The engine-backed {@link GameController} — the single seam the UI drives.
 *
 * It validates and applies every player action against the domain model, runs
 * the phase machine, drives AI opponents, applies economy yields, resolves
 * combat, seeds The Unfaithful's Sin cards, and re-evaluates all victory lanes
 * at the end of every turn. It imports only {@code api} and {@code domain}.
 *
 * Human actions are only legal on the human player's MAIN phase. Calling
 * {@link #endTurn()} finishes the human's turn (yield + discard), runs each AI
 * opponent's full turn, checks victory after each, and — if the game continues —
 * opens the human's next turn with a fresh hand.
 */
public final class GameEngine implements GameController {

    /** Default deterministic seed used when (re)starting via {@link #newGame}. */
    private static final long DEFAULT_SEED = 20260703L;

    private long seed = DEFAULT_SEED;

    private GameState state;
    private EngineStats stats;
    private EconomyCalculator economy;
    private TurnManager turns;
    private CombatResolver combat;
    private SinLogic sin;
    private VictoryMonitor victory;
    private AiPlayer aiBrain;

    /** Wrap an already-built {@link GameState} (from the domain factory). */
    public GameEngine(GameState initial) {
        wire(initial);
        openHumanTurn(false);
    }

    // ------------------------------------------------------------------
    // Wiring / lifecycle
    // ------------------------------------------------------------------

    private void wire(GameState s) {
        this.state = s;
        int players = s.playerStates().size();
        this.stats = new EngineStats(players);
        this.economy = new EconomyCalculator(s);
        this.turns = new TurnManager(s, economy);
        this.combat = new CombatResolver(s, stats);
        this.sin = new SinLogic(s);
        this.victory = new VictoryMonitor(s, stats);
        this.aiBrain = new AiPlayer(s, this);
    }

    /** Position the human at the start of a MAIN phase with a fresh hand. */
    private void openHumanTurn(boolean advanceTurnNumber) {
        PlayerState human = humanPlayer();
        state.setCurrentPlayerIndex(human.index());
        if (advanceTurnNumber) {
            state.setTurnNumber(state.turnNumber() + 1);
        }
        turns.drawPhase(human);
        state.setPhase(GamePhase.MAIN);
    }

    private PlayerState humanPlayer() {
        List<PlayerState> players = state.playerStates();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isHuman()) {
                return players.get(i);
            }
        }
        return players.get(0);
    }

    // ------------------------------------------------------------------
    // GameController
    // ------------------------------------------------------------------

    @Override
    public GameStateView state() {
        return state;
    }

    @Override
    public void newGame(Faction humanFaction) {
        GameState fresh = GameState.create(humanFaction, seed);
        wire(fresh);
        state.log("New game — you lead " + humanFaction.display());
        openHumanTurn(false);
    }

    @Override
    public ActionResult playBuilding(int cardId, int row, int col) {
        ActionResult guard = requireHumanMain();
        if (guard != null) {
            return guard;
        }
        return doPlayBuilding(state.current(), cardId, row, col);
    }

    @Override
    public ActionResult attachCard(int cardId, int buildingId) {
        ActionResult guard = requireHumanMain();
        if (guard != null) {
            return guard;
        }
        return doAttach(state.current(), cardId, buildingId);
    }

    @Override
    public ActionResult playCard(int cardId, int row, int col) {
        ActionResult guard = requireHumanMain();
        if (guard != null) {
            return guard;
        }
        return doPlayCard(state.current(), cardId, row, col);
    }

    @Override
    public ActionResult resolveCombat(int cardId, int row, int col) {
        ActionResult guard = requireHumanMain();
        if (guard != null) {
            return guard;
        }
        return doResolveCombat(state.current(), cardId, row, col);
    }

    @Override
    public ActionResult endTurn() {
        if (state.isGameOver()) {
            return ActionResult.fail("The game is already over");
        }
        PlayerState human = humanPlayer();

        // Finish the human's turn: combat already happened in MAIN.
        state.setCurrentPlayerIndex(human.index());
        state.setPhase(GamePhase.COMBAT);
        turns.yieldPhase(human);
        turns.discardPhase(human);
        turns.endPhase(human);
        if (victory.check()) {
            return ActionResult.ok(winnerMessage());
        }

        // Run each AI opponent's full turn.
        List<PlayerState> players = state.playerStates();
        for (int i = 0; i < players.size(); i++) {
            PlayerState p = players.get(i);
            if (p.isHuman()) {
                continue;
            }
            runAiTurn(p);
            if (victory.check()) {
                return ActionResult.ok(winnerMessage());
            }
        }

        // Open the human's next turn.
        openHumanTurn(true);
        return ActionResult.ok("Turn " + state.turnNumber() + " — your move");
    }

    private void runAiTurn(PlayerState ai) {
        state.setCurrentPlayerIndex(ai.index());
        turns.drawPhase(ai);
        state.setPhase(GamePhase.MAIN);
        aiBrain.playMainPhase(ai);
        state.setPhase(GamePhase.COMBAT);
        aiBrain.playCombatPhase(ai);
        turns.yieldPhase(ai);
        turns.discardPhase(ai);
        turns.endPhase(ai);
    }

    private ActionResult requireHumanMain() {
        if (state.isGameOver()) {
            return ActionResult.fail("The game is over");
        }
        PlayerState actor = state.current();
        if (!actor.isHuman()) {
            return ActionResult.fail("It is not your turn");
        }
        if (state.phase() != GamePhase.MAIN) {
            return ActionResult.fail("Actions are only allowed in the MAIN phase");
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Internal action implementations (shared by human & AI)
    // ------------------------------------------------------------------

    ActionResult doPlayBuilding(PlayerState actor, int cardId, int row, int col) {
        Card card = findInHand(actor, cardId);
        if (card == null) {
            return ActionResult.fail("Card not in hand");
        }
        if (card.type() != CardType.BUILDING) {
            return ActionResult.fail(card.name() + " is not a building");
        }
        GridMap map = state.gridMap();
        if (!inBounds(map, row, col)) {
            return ActionResult.fail("Tile is off the map");
        }
        TileView tile = map.tile(row, col);
        if (tile.building() != null) {
            return ActionResult.fail("Tile (" + row + "," + col + ") is occupied");
        }
        TerrainType terrain = tile.terrain();
        if (terrain == TerrainType.WATER) {
            return ActionResult.fail("Cannot build on open water");
        }
        BuildingType bt = card.buildingType();
        if (bt == BuildingType.PORT && !map.isWaterAdjacent(row, col)) {
            return ActionResult.fail("A Port must border a water tile");
        }
        int cost = effectiveBuildingCost(actor, card);
        Resources res = actor.resources();
        if (!res.canAfford(ResourceType.GOLD, cost)) {
            return ActionResult.fail("Need " + cost + " gold for " + card.name());
        }
        res.spend(ResourceType.GOLD, cost);
        Building b = map.placeBuilding(actor.index(), card, row, col);
        actor.removeFromHand(cardId);
        actor.discard().add(card);
        String msg = actor.name() + " built a " + bt.display() + " at (" + row + ","
                + col + ") for " + cost + " gold";
        state.log(msg);
        return ActionResult.ok(msg);
    }

    ActionResult doAttach(PlayerState actor, int cardId, int buildingId) {
        Card card = findInHand(actor, cardId);
        if (card == null) {
            return ActionResult.fail("Card not in hand");
        }
        if (card.type() != CardType.ATTACHMENT) {
            return ActionResult.fail(card.name() + " is not an attachment");
        }
        Building b = findOwnedBuilding(actor, buildingId);
        if (b == null) {
            return ActionResult.fail("You have no building #" + buildingId);
        }
        if (!b.hasCapacity()) {
            return ActionResult.fail(b.type().display() + " is at attachment capacity");
        }
        AttachmentType at = card.attachmentType();
        if (!Attachment.isLegal(at, b.type())) {
            return ActionResult.fail(at.display() + " cannot attach to a "
                    + b.type().display());
        }
        int cost = card.cost();
        Resources res = actor.resources();
        if (!res.canAfford(ResourceType.GOLD, cost)) {
            return ActionResult.fail("Need " + cost + " gold for " + card.name());
        }
        if (!b.addAttachment(new Attachment(card))) {
            return ActionResult.fail(b.type().display() + " could not take the attachment");
        }
        res.spend(ResourceType.GOLD, cost);
        actor.removeFromHand(cardId);
        String msg = actor.name() + " attached " + at.display() + " to a "
                + b.type().display();
        state.log(msg);
        return ActionResult.ok(msg);
    }

    ActionResult doPlayCard(PlayerState actor, int cardId, int row, int col) {
        Card card = findInHand(actor, cardId);
        if (card == null) {
            return ActionResult.fail("Card not in hand");
        }
        CardType t = card.type();
        if (t == CardType.SIN) {
            return ActionResult.fail("Sin cards are dead weight — discard them");
        }
        if (t == CardType.BUILDING) {
            return ActionResult.fail("Use playBuilding for buildings");
        }
        if (t == CardType.ATTACHMENT) {
            return ActionResult.fail("Use attachCard for attachments");
        }
        if (t == CardType.MILITARY) {
            return ActionResult.fail("Use resolveCombat for military cards");
        }
        Resources res = actor.resources();
        int cost = card.cost();
        if (!res.canAfford(ResourceType.GOLD, cost)) {
            return ActionResult.fail("Need " + cost + " gold for " + card.name());
        }

        if (t == CardType.EXPLORE) {
            GridMap map = state.gridMap();
            if (!inBounds(map, row, col)) {
                return ActionResult.fail("Explore target is off the map");
            }
            res.spend(ResourceType.GOLD, cost);
            int radius = card.value() > 0 ? card.value() : 1;
            int revealed = reveal(map, row, col, radius);
            actor.removeFromHand(cardId);
            actor.discard().add(card);
            String msg = actor.name() + " explored " + revealed + " tile(s) around ("
                    + row + "," + col + ")";
            state.log(msg);
            return ActionResult.ok(msg);
        }

        // ECONOMY: a one-shot resource injection (amount/kind defined by the card).
        res.spend(ResourceType.GOLD, cost);
        int payload = card.value();
        ResourceType gain = card.economyResource();
        if (gain == null) {
            gain = ResourceType.GOLD;
        }
        res.add(gain, payload);
        String msg = actor.name() + " played " + card.name() + " (+" + payload + " "
                + gain.display() + ")";
        state.log(msg);
        String sinMsg = sin.applySinCost(actor, card);
        actor.removeFromHand(cardId);
        actor.discard().add(card);
        if (sinMsg != null) {
            state.log(sinMsg);
        }
        return ActionResult.ok(msg);
    }

    ActionResult doResolveCombat(PlayerState actor, int cardId, int row, int col) {
        Card card = findInHand(actor, cardId);
        if (card == null) {
            return ActionResult.fail("Card not in hand");
        }
        if (card.type() != CardType.MILITARY) {
            return ActionResult.fail(card.name() + " is not a military card");
        }
        int cost = card.cost();
        Resources res = actor.resources();
        if (!res.canAfford(ResourceType.GOLD, cost)) {
            return ActionResult.fail("Need " + cost + " gold to deploy " + card.name());
        }
        ActionResult result = combat.resolve(actor, card, row, col);
        if (result.isSuccess()) {
            res.spend(ResourceType.GOLD, cost);
            actor.removeFromHand(cardId);
            actor.discard().add(card);
            String sinMsg = sin.applySinCost(actor, card);
            if (sinMsg != null) {
                state.log(sinMsg);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Card findInHand(PlayerState actor, int cardId) {
        List<Card> hand = actor.handCards();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).id() == cardId) {
                return hand.get(i);
            }
        }
        return null;
    }

    private Building findOwnedBuilding(PlayerState actor, int buildingId) {
        List<Building> mine = state.gridMap().buildingsOwnedBy(actor.index());
        for (int i = 0; i < mine.size(); i++) {
            if (mine.get(i).id() == buildingId) {
                return mine.get(i);
            }
        }
        return null;
    }

    private int effectiveBuildingCost(PlayerState actor, Card card) {
        int cost = card.cost() + actor.profile().buildingCostModifier();
        return cost < 0 ? 0 : cost;
    }

    private static boolean inBounds(GridMap map, int row, int col) {
        return row >= 0 && col >= 0 && row < map.rows() && col < map.cols();
    }

    /** Reveal every tile within Manhattan {@code radius} of (row,col). */
    private int reveal(GridMap map, int row, int col, int radius) {
        int count = 0;
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                if (Math.abs(r - row) + Math.abs(c - col) > radius) {
                    continue;
                }
                if (inBounds(map, r, c) && !map.tile(r, c).explored()) {
                    map.tileAt(r, c).setExplored(true);
                    count += 1;
                }
            }
        }
        return count;
    }

    private String winnerMessage() {
        int idx = state.winnerPlayerIndex();
        if (idx < 0) {
            return "Game over";
        }
        PlayerState w = state.playerAt(idx);
        VictoryType v = state.winningVictory();
        String how = v == null ? "" : (" by " + v.display() + " victory");
        return "Game over — " + w.name() + " wins" + how;
    }
}

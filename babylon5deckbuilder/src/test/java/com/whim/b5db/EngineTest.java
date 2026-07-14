package com.whim.b5db;

import com.whim.b5db.ai.HeuristicAgent;
import com.whim.b5db.engine.BasicCards;
import com.whim.b5db.engine.GameConfig;
import com.whim.b5db.engine.GameEngine;
import com.whim.b5db.engine.GameResult;
import com.whim.b5db.engine.GameState;
import com.whim.b5db.engine.Market;
import com.whim.b5db.engine.PlayerState;
import com.whim.b5db.engine.Rng;
import com.whim.b5db.engine.Seat;
import com.whim.b5db.model.Card;
import com.whim.b5db.model.CardType;
import com.whim.b5db.model.ContestType;
import com.whim.b5db.model.Effect;
import com.whim.b5db.model.Faction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for shuffle/draw, purchase, conflict and agenda resolution, plus determinism. */
public class EngineTest {

    private GameEngine engine() {
        return new GameEngine(new ArrayList<>(), new GameConfig());
    }

    private List<Seat> twoSeats() {
        return Arrays.asList(
                new Seat("A", Faction.NARN_REGIME, true),
                new Seat("B", Faction.CENTAURI_REPUBLIC, true));
    }

    @Test
    public void starterDeckHasTenCards() {
        assertEquals(10, BasicCards.starterDeck().size());
    }

    @Test
    public void drawReshufflesDiscardWhenDeckEmpties() {
        PlayerState p = new PlayerState("t", Faction.NARN_REGIME, false);
        Rng rng = new Rng(1L);
        p.drawDeck().addAll(BasicCards.starterDeck());
        int drawn = p.draw(10, rng);
        assertEquals(10, drawn);
        assertTrue(p.drawDeck().isEmpty());
        // Move all to discard and draw again -> forces a reshuffle.
        p.discard().addAll(p.hand());
        p.hand().clear();
        int drawn2 = p.draw(5, rng);
        assertEquals(5, drawn2);
        assertEquals(5, p.hand().size());
    }

    @Test
    public void purchaseSpendsInfluenceAndPlacesCard() {
        GameEngine engine = engine();
        GameState state = engine.createGame(twoSeats(), 42L);
        PlayerState p = state.current();
        p.addInfluence(10);
        Card chit = BasicCards.CREDIT_CHIT; // corridor pile, always available
        assertTrue(engine.purchase(state, p, chit));
        assertEquals(9, p.influence());
        assertTrue(p.discard().contains(chit));
    }

    @Test
    public void permanentPurchaseGoesToCommandRow() {
        GameEngine engine = engine();
        GameState state = engine.createGame(twoSeats(), 7L);
        PlayerState p = state.current();
        p.addInfluence(5);
        Card ship = BasicCards.PATROL_SHIP; // permanent FLEET in the corridor
        assertTrue(engine.purchase(state, p, ship));
        assertTrue(p.commandRow().contains(ship));
    }

    @Test
    public void conflictAwardsPrestigeWhenBeaten() {
        GameEngine engine = engine();
        Card conflict = new Card("c1", "Test Conflict", Faction.NON_ALIGNED, CardType.CONFLICT,
                1, 3, null, ContestType.MILITARY, 5, Collections.<Effect>emptyList(), "");
        Rng rng = new Rng(1L);
        Market market = new Market(Arrays.asList(conflict), BasicCards.corridorPiles(), rng);
        List<PlayerState> players = new ArrayList<>();
        PlayerState p = new PlayerState("t", Faction.NARN_REGIME, false);
        players.add(p);
        GameState state = new GameState(players, market, rng, 40);

        p.addPool(ContestType.MILITARY, 6); // beats difficulty 5
        engine.resolveConflicts(state, p);
        assertEquals(3, p.prestige());
        assertFalse(market.rim().contains(conflict));
    }

    @Test
    public void conflictNotBeatenLeavesRimUnchanged() {
        GameEngine engine = engine();
        Card conflict = new Card("c2", "Hard Conflict", Faction.NON_ALIGNED, CardType.CONFLICT,
                1, 3, null, ContestType.MILITARY, 9, Collections.<Effect>emptyList(), "");
        Rng rng = new Rng(1L);
        Market market = new Market(Arrays.asList(conflict), BasicCards.corridorPiles(), rng);
        List<PlayerState> players = new ArrayList<>();
        PlayerState p = new PlayerState("t", Faction.NARN_REGIME, false);
        players.add(p);
        GameState state = new GameState(players, market, rng, 40);

        p.addPool(ContestType.MILITARY, 3);
        engine.resolveConflicts(state, p);
        assertEquals(0, p.prestige());
        assertTrue(market.rim().contains(conflict));
    }

    @Test
    public void agendaEffectAppliesEachTurn() {
        GameEngine engine = engine();
        Card agenda = new Card("ag", "Ongoing Agenda", Faction.NARN_REGIME, CardType.AGENDA,
                1, 0, null, null, 0,
                Arrays.asList(new Effect(Effect.Type.GAIN_PRESTIGE, 1, null)), "");
        Rng rng = new Rng(3L);
        Market market = new Market(new ArrayList<>(), BasicCards.corridorPiles(), rng);
        List<PlayerState> players = new ArrayList<>();
        PlayerState p = new PlayerState("t", Faction.NARN_REGIME, false);
        p.commandRow().add(agenda);
        players.add(p);
        GameState state = new GameState(players, market, rng, 40);

        engine.beginTurn(state); // START applies command-row effects
        assertEquals(1, p.prestige());
    }

    @Test
    public void sameSeedProducesIdenticalResult() {
        GameEngine engine = new GameEngine(new ArrayList<>(), new GameConfig(20));
        GameResult r1 = engine.run(engine.createGame(twoSeats(), 99L),
                Arrays.asList(new HeuristicAgent(HeuristicAgent.Difficulty.HARD),
                        new HeuristicAgent(HeuristicAgent.Difficulty.HARD)));
        GameResult r2 = engine.run(engine.createGame(twoSeats(), 99L),
                Arrays.asList(new HeuristicAgent(HeuristicAgent.Difficulty.HARD),
                        new HeuristicAgent(HeuristicAgent.Difficulty.HARD)));
        assertEquals(r1.winnerFaction(), r2.winnerFaction());
        assertEquals(r1.turns(), r2.turns());
        assertTrue(Arrays.equals(r1.prestige(), r2.prestige()));
    }
}

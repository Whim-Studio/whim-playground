package com.tiwa.mahjong.setup;

import com.tiwa.mahjong.api.Wind;
import com.tiwa.mahjong.model.Player;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameStateTest {

    @Test
    public void dealingYields13_13_13_14() {
        GameState state = GameState.newHand(2026L);
        List<Player> players = state.getMutablePlayers();
        assertEquals(4, players.size());
        assertEquals(14, players.get(0).getConcealedTiles().size()); // dealer drew to 14
        assertEquals(13, players.get(1).getConcealedTiles().size());
        assertEquals(13, players.get(2).getConcealedTiles().size());
        assertEquals(13, players.get(3).getConcealedTiles().size());

        // 4*13 + 1 = 53 tiles consumed; 144 - 53 = 91 remain.
        assertEquals(91, state.getWall().tilesRemaining());
    }

    @Test
    public void dealerIsEastSeatZeroAndCurrent() {
        GameState state = GameState.newHand(2026L);
        assertEquals(0, state.getDealerIndex());
        assertEquals(0, state.getCurrentPlayerIndex());
        assertEquals(Wind.EAST, state.getRoundWind());
        assertEquals(Wind.EAST, state.getMutablePlayers().get(0).getSeatWind());
    }

    @Test
    public void seatsAreOrderedWindsEastSouthWestNorth() {
        GameState state = GameState.newHand(555L);
        List<Player> players = state.getMutablePlayers();
        assertEquals(Wind.EAST, players.get(0).getSeatWind());
        assertEquals(Wind.SOUTH, players.get(1).getSeatWind());
        assertEquals(Wind.WEST, players.get(2).getSeatWind());
        assertEquals(Wind.NORTH, players.get(3).getSeatWind());
    }

    @Test
    public void seatingOrdersByDiceDescending() {
        GameState state = GameState.newHand(555L);
        Seating seating = state.getSeating();
        // Seat 0 (East) holds the highest roll; totals are non-increasing across seats.
        int prev = Integer.MAX_VALUE;
        for (int seat = 0; seat < 4; seat++) {
            int total = seating.rollForSeat(seat).total();
            assertTrue("seat " + seat + " roll " + total + " must be <= previous " + prev, total <= prev);
            assertTrue(total >= 3 && total <= 18);
            prev = total;
        }
    }

    @Test
    public void roundWindRotatesClockwise() {
        GameState state = GameState.newHand(1L);
        assertEquals(Wind.EAST, state.getRoundWind());
        state.rotateRoundWind();
        assertEquals(Wind.SOUTH, state.getRoundWind());
    }

    @Test
    public void sameSeedGivesSameDeal() {
        GameState a = GameState.newHand(77L);
        GameState b = GameState.newHand(77L);
        assertEquals(a.getMutablePlayers().get(0).getConcealedTiles(),
                b.getMutablePlayers().get(0).getConcealedTiles());
    }

    @Test
    public void revealAndReplaceBonusKeepsHandSize() {
        GameState state = GameState.newHand(2026L);
        Player dealer = state.getMutablePlayers().get(0);
        int before = dealer.getConcealedTiles().size();
        // Find a bonus tile in the dealer's hand if present; otherwise the test is a no-op assertion.
        com.tiwa.mahjong.api.Tile bonus = null;
        for (com.tiwa.mahjong.api.Tile t : dealer.getConcealedTiles()) {
            if (t.isBonus()) {
                bonus = t;
                break;
            }
        }
        if (bonus != null) {
            int wallBefore = state.getWall().tilesRemaining();
            state.revealAndReplaceBonus(0, bonus);
            assertEquals(before, dealer.getConcealedTiles().size());
            assertEquals(1, dealer.getBonusTiles().size());
            assertEquals(wallBefore - 1, state.getWall().tilesRemaining());
        }
    }
}

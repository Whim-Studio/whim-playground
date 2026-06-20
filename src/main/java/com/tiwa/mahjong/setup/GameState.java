package com.tiwa.mahjong.setup;

import com.tiwa.mahjong.api.GameContext;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wind;
import com.tiwa.mahjong.model.Player;
import com.tiwa.mahjong.model.StandardWall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mutable game state implementing {@link GameContext}: the four seated players, the wall, the round
 * wind, the dealer/current seat indices, and the shared discard pile.
 *
 * <p>Build a ready-to-play hand with {@link #newHand(long)}: it rolls the seating dice, builds and
 * shuffles the wall, deals 13 tiles to each player (4+4+4+1) and draws the dealer up to 14, then
 * returns the state with the current player set to the dealer (East, seat 0).</p>
 */
public final class GameState implements GameContext {

    /** Tiles dealt to each non-dealer player; the dealer draws one extra to 14. */
    public static final int HAND_SIZE = 13;

    private final List<Player> players;
    private final StandardWall wall;
    private final Seating seating;
    private Wind roundWind;
    private final int dealerIndex;
    private int currentPlayerIndex;
    private final List<Tile> discardPile = new ArrayList<Tile>();

    private GameState(List<Player> players, StandardWall wall, Seating seating, Wind roundWind,
                      int dealerIndex, int currentPlayerIndex) {
        this.players = players;
        this.wall = wall;
        this.seating = seating;
        this.roundWind = roundWind;
        this.dealerIndex = dealerIndex;
        this.currentPlayerIndex = currentPlayerIndex;
    }

    /**
     * Builds a fresh hand deterministically from {@code seed}: dice -&gt; seating -&gt; wall build
     * -&gt; deal. The round wind starts East and the dealer (East) is seat 0 and current player.
     */
    public static GameState newHand(long seed) {
        Random rng = new Random(seed);

        Seating seating = Seating.determine(rng);

        List<Player> players = new ArrayList<Player>(4);
        for (int seat = 0; seat < 4; seat++) {
            players.add(new Player(seat, seating.windForSeat(seat)));
        }

        StandardWall wall = new StandardWall(rng);

        deal(players, wall);

        int dealerIndex = 0; // East
        return new GameState(players, wall, seating, Wind.EAST, dealerIndex, dealerIndex);
    }

    /** Deals 13 tiles to each player (4+4+4+1), then draws the dealer up to 14. */
    private static void deal(List<Player> players, StandardWall wall) {
        // Three rounds of four tiles each, in seat order starting from the dealer.
        for (int round = 0; round < 3; round++) {
            for (int seat = 0; seat < players.size(); seat++) {
                for (int i = 0; i < 4; i++) {
                    players.get(seat).addConcealedTile(wall.draw());
                }
            }
        }
        // Final single tile each: brings every hand to 13.
        for (int seat = 0; seat < players.size(); seat++) {
            players.get(seat).addConcealedTile(wall.draw());
        }
        // Dealer (East, seat 0) draws first to 14.
        players.get(0).addConcealedTile(wall.draw());
    }

    @Override
    public List<? extends com.tiwa.mahjong.api.PlayerView> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /** Concrete players (they ARE the PlayerViews), for setup/mutation by Task 1/2. */
    public List<Player> getMutablePlayers() {
        return players;
    }

    @Override
    public StandardWall getWall() {
        return wall;
    }

    @Override
    public Wind getRoundWind() {
        return roundWind;
    }

    /** Rotates the round wind clockwise (after the four hands of a round). */
    public void rotateRoundWind() {
        this.roundWind = roundWind.clockwise();
    }

    @Override
    public int getDealerIndex() {
        return dealerIndex;
    }

    @Override
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    @Override
    public void setCurrentPlayerIndex(int seatIndex) {
        if (seatIndex < 0 || seatIndex > 3) {
            throw new IllegalArgumentException("seatIndex must be 0..3");
        }
        this.currentPlayerIndex = seatIndex;
    }

    @Override
    public List<Tile> getDiscardPile() {
        return discardPile;
    }

    /** The opening seating/dice result for this hand. */
    public Seating getSeating() {
        return seating;
    }

    /**
     * Reveals a bonus tile (flower/season) from a player's concealed hand and draws a replacement
     * from the wall front, returning the replacement. Exposed so the (Task 2) loop can reuse it
     * during the deal/play. Returns null if no replacement is available (wall exhausted).
     */
    public Tile revealAndReplaceBonus(int seatIndex, Tile bonusTile) {
        Player player = players.get(seatIndex);
        if (!player.removeConcealedTile(bonusTile)) {
            throw new IllegalArgumentException("player " + seatIndex + " does not hold " + bonusTile);
        }
        player.revealBonusTile(bonusTile);
        if (wall.isEmpty()) {
            return null;
        }
        Tile replacement = wall.drawReplacement();
        player.addConcealedTile(replacement);
        return replacement;
    }
}

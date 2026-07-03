package com.whim.ttr.api;

import com.whim.ttr.domain.DestinationTicket;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.PlayerScore;

import java.util.List;

/**
 * The hard contract between the rules engine (Task 2) and the Swing UI (Task 3).
 *
 * <p>The UI reads state through {@link #state()} and drives the game exclusively
 * through the mutating methods here — it never reaches into the engine's
 * internals. All heavy graph work (tunnel resolution, DFS ticket scoring, the
 * longest-path calculation) lives behind this interface and off the Swing EDT.</p>
 *
 * <p>Every mutating method validates the current player and phase and returns an
 * {@link ActionOutcome}; an illegal request yields {@code success == false} with
 * an explanatory message and leaves game state untouched.</p>
 */
public interface GameEngine {

    /** The single shared, mutable game state aggregate (read by the UI). */
    GameState state();

    /** Id (0-based seat index) of the player whose turn it is. */
    int currentPlayerId();

    GamePhase phase();

    boolean isGameOver();

    // ---- Turn actions (a legal turn performs exactly one of these) ----------

    /**
     * Draw a train card. {@code faceUpIndex} in [0,{@link GameConstants#FACE_UP_SLOTS})
     * takes that market card; {@code -1} draws blind from the deck. Drawing a
     * face-up LOCOMOTIVE ends the draw action immediately (counts as both draws).
     */
    ActionOutcome drawTrainCard(int faceUpIndex);

    /**
     * Attempt to claim {@code routeId} paying {@code cards} from the current
     * player's hand. NORMAL/FERRY routes resolve immediately. For a TUNNEL this
     * returns {@link ActionOutcome#tunnel} and the engine holds a pending claim
     * that must be settled with {@link #confirmTunnel} or {@link #cancelTunnel}.
     */
    ActionOutcome beginClaimRoute(String routeId, List<CardColor> cards);

    /** Pay the additional tunnel cards; empty/insufficient list cancels the claim. */
    ActionOutcome confirmTunnel(List<CardColor> extraCards);

    /** Abort a pending tunnel claim and return all staged cards to the hand. */
    ActionOutcome cancelTunnel();

    /** Draw ticket options (does not yet commit them). See GameConstants. */
    List<DestinationTicket> offerTickets();

    /** Keep the chosen subset of a prior {@link #offerTickets()} (respecting minimums). */
    ActionOutcome keepTickets(List<DestinationTicket> kept);

    /** Build a train station in {@code cityId}, paying {@code cards}. */
    ActionOutcome buildStation(String cityId, List<CardColor> cards);

    /** Advance to the next player (and arm/settle endgame as needed). */
    ActionOutcome endTurn();

    // ---- Scoring ------------------------------------------------------------

    /**
     * Final per-player breakdown: route points, completed/failed ticket deltas,
     * unused-station bonus, and whether the player holds the European Express.
     * Ticket completion accounts for the station-borrowing rule.
     */
    List<PlayerScore> finalScores();
}

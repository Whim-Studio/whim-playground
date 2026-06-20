package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.GameContext;

/**
 * Advances turns over an {@link GameContext} (Section 2-3).
 *
 * <p>Turn order is counter-clockwise. Seats are numbered clockwise (East=0, South=1, West=2,
 * North=3), so the next player to act is at {@code (seat + 3) % 4} - one step counter-clockwise,
 * i.e. decreasing seat index. Tiles are drawn clockwise from the wall, which is the wall's own
 * concern ({@link com.tiwa.mahjong.api.Wall}).</p>
 *
 * <p>The per-turn sequence is Draw -&gt; Check for Mahjong -&gt; Discard. This controller owns turn
 * advancement and the turn-related guards; the concrete draw/discard mutations live behind Task 1's
 * model.</p>
 */
public final class TurnController {

    private final GameContext context;

    public TurnController(GameContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context;
    }

    /** The seat that will play immediately after {@code seat} (one step counter-clockwise). */
    public static int nextSeatCounterClockwise(int seat) {
        if (seat < 0 || seat > 3) {
            throw new IllegalArgumentException("seat must be 0-3, was " + seat);
        }
        return (seat + 3) % 4;
    }

    /** Advance the active player one step counter-clockwise and return the new active seat. */
    public int advanceTurn() {
        int next = nextSeatCounterClockwise(context.getCurrentPlayerIndex());
        context.setCurrentPlayerIndex(next);
        return next;
    }

    /**
     * Out-of-turn protection during a replacement draw (Section 4/8): while the active player draws a
     * kong/flower replacement they alone hold 14 tiles, so no other seat can legitimately go Mahjong.
     * Only the active player's Mahjong claim is acceptable mid-replacement.
     */
    public boolean isMahjongAcceptableDuringReplacementDraw(int claimantSeat) {
        return claimantSeat == context.getCurrentPlayerIndex();
    }
}

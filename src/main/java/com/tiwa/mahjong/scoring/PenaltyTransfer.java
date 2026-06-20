package com.tiwa.mahjong.scoring;

/**
 * The result of a penalty or immediate transfer (Section 8). The same per-player amount applies to
 * each of the three other players; {@link #getSubjectDelta()} is the net change for the subject of
 * the transfer (the offender for a false claim, or the revealer for a Flowers/Seasons payment).
 *
 * <p>Amounts are signed from each seat's perspective: positive = receives, negative = pays. The unit
 * (points or currency) is whatever the producing method documents.</p>
 */
public final class PenaltyTransfer {

    /** Number of opponents at a four-player table. */
    public static final int OTHER_PLAYERS = 3;

    private final long subjectDelta;
    private final long otherPlayerDelta;

    public PenaltyTransfer(long subjectDelta, long otherPlayerDelta) {
        this.subjectDelta = subjectDelta;
        this.otherPlayerDelta = otherPlayerDelta;
    }

    /** Net change for the subject (offender pays out, revealer receives). */
    public long getSubjectDelta() {
        return subjectDelta;
    }

    /** Change applied to EACH of the three other players. */
    public long getOtherPlayerDelta() {
        return otherPlayerDelta;
    }
}

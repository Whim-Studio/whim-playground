package com.tiwa.mahjong.scoring;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PenaltiesTest {

    private final Penalties penalties = new Penalties();

    /** False Mahjong (points game): offender loses 1000 to each other player, -3000 total. */
    @Test
    public void falseMahjongLoses3000() {
        WinContext ctx = WinContext.builder().pointsLimit(1000).build();
        PenaltyTransfer t = penalties.falseMahjongPoints(ctx);
        assertEquals(1000L, t.getOtherPlayerDelta());
        assertEquals(-3000L, t.getSubjectDelta());
    }

    /** Unlimited-money false claim: $1000 to each other player. */
    @Test
    public void falseMahjongUnlimitedMoney() {
        WinContext ctx = WinContext.builder().unlimitedMoney(true).build();
        PenaltyTransfer t = penalties.falseMahjong(ctx);
        assertEquals(1000L, t.getOtherPlayerDelta());
        assertEquals(-3000L, t.getSubjectDelta());
    }

    /** Money game: the loss to each other player is the money limit. */
    @Test
    public void falseMahjongMoneyLimit() {
        WinContext ctx = WinContext.builder().moneyLimit(500).build();
        PenaltyTransfer t = penalties.falseMahjong(ctx);
        assertEquals(500L, t.getOtherPlayerDelta());
        assertEquals(-1500L, t.getSubjectDelta());
    }

    /** Immediate Flowers/Seasons reveal: each other player pays $2, revealer nets +$6. */
    @Test
    public void flowerRevealImmediateTransfer() {
        PenaltyTransfer t = penalties.flowerReveal();
        assertEquals(-2L, t.getOtherPlayerDelta());
        assertEquals(6L, t.getSubjectDelta());
    }
}

package com.tiwa.mahjong.scoring;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PayoutCalculatorTest {

    private final PayoutCalculator pc = new PayoutCalculator();

    /** Worked examples from Section 7: floor(points * moneyLimit / pointsLimit). */
    @Test
    public void workedExamples() {
        assertEquals(2L, pc.payout(250, 10, 1000));   // 2.50 -> 2
        assertEquals(12L, pc.payout(250, 100, 2000)); // 12.50 -> 12
    }

    /** Unlimited-money game pays $1 per point. */
    @Test
    public void unlimitedMoney() {
        WinContext ctx = WinContext.builder().unlimitedMoney(true).build();
        assertEquals(250L, pc.payout(250, ctx));
    }

    /**
     * Settlement sums the exact point components first, then rounds the currency ONCE.
     * 150 + 150 = 300 -> floor(300*10/1000) = 3, whereas rounding each component first
     * (1.5 -> 1 twice) would wrongly give 2.
     */
    @Test
    public void settleRoundsOnce() {
        WinContext ctx = WinContext.builder().moneyLimit(10).pointsLimit(1000).build();
        assertEquals(3L, pc.settle(Arrays.asList(150L, 150L), ctx));
    }

    /** Round-down applies to negative balances too (toward a larger debt). */
    @Test
    public void negativeRoundsDown() {
        assertEquals(-3L, pc.payout(-250, 10, 1000)); // -2.5 -> -3
    }
}

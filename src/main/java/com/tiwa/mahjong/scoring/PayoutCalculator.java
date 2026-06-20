package com.tiwa.mahjong.scoring;

import java.util.List;

/**
 * Converts hand points into a currency payout, implementing Section 7 of Tiwa's Mah Jong Rulebook.
 *
 * <p>{@code payout = floor(points * MoneyLimit / PointsLimit)}. The multiply-before-divide integer
 * form avoids floating-point rounding bugs (250/1000 * 10 must round down to 2, not 3). In an
 * unlimited-money game each point is worth one currency unit.</p>
 *
 * <p>Per the rulebook, when a player has several components in one settlement (gains and losses
 * across different events), the exact point amounts are summed first and the conversion to money is
 * rounded ONCE — see {@link #settle(List, WinContext)}.</p>
 */
public final class PayoutCalculator {

    /** Money owed for a single hand-point amount, using the context's money/points limits. */
    public long payout(long points, WinContext ctx) {
        if (ctx.isUnlimitedMoney()) {
            return points; // $1 per point
        }
        return payout(points, ctx.getMoneyLimit(), ctx.getPointsLimit());
    }

    /** Low-level: {@code floor(points * moneyLimit / pointsLimit)}, correct for negative points. */
    public long payout(long points, long moneyLimit, int pointsLimit) {
        return Math.floorDiv(points * moneyLimit, (long) pointsLimit);
    }

    /**
     * Settle a player across multiple point components: sum the exact point deltas first, then
     * convert to money and round down ONCE (Section 7). Negative totals round toward negative
     * infinity (a larger debt), matching "round down".
     */
    public long settle(List<Long> pointComponents, WinContext ctx) {
        long totalPoints = 0L;
        for (Long component : pointComponents) {
            totalPoints += component;
        }
        return payout(totalPoints, ctx);
    }
}

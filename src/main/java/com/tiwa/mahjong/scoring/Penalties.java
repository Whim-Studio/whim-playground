package com.tiwa.mahjong.scoring;

/**
 * Penalty and immediate-transfer calculations from Section 8 of Tiwa's Mah Jong Rulebook.
 *
 * <p>All methods return a {@link PenaltyTransfer} whose amounts are signed per seat (positive =
 * receives, negative = pays). Each transfer involves the subject seat and the three other players.</p>
 */
public final class Penalties {

    /** $2, paid by every other player to the revealer of a Flower/Season. */
    private static final long FLOWER_PAYMENT = 2L;

    /**
     * False Mahjong declaration (also: a knowingly false fully-concealed claim, treated identically):
     * the offender pays the Points Limit to EACH other player. In points the default limit is 1000,
     * so the offender loses 3000 total. In a money game the loss to each other player is the money
     * limit; in an unlimited-money game it is $1000 to each other player.
     */
    public PenaltyTransfer falseMahjong(WinContext ctx) {
        return falseClaimTransfer(ctx);
    }

    /**
     * A knowingly false fully-concealed claim. Section 8 says this is treated identically to a false
     * Mahjong declaration.
     */
    public PenaltyTransfer falseConcealedClaim(WinContext ctx) {
        return falseClaimTransfer(ctx);
    }

    private PenaltyTransfer falseClaimTransfer(WinContext ctx) {
        long perPlayer;
        if (ctx.isUnlimitedMoney()) {
            perPlayer = 1000L; // $1000 to each other player
        } else {
            // Money game: the money limit per other player; pure-points game: the points limit.
            perPlayer = ctx.getMoneyLimit() > 0 ? ctx.getMoneyLimit() : ctx.getPointsLimit();
        }
        return new PenaltyTransfer(-perPlayer * PenaltyTransfer.OTHER_PLAYERS, perPlayer);
    }

    /**
     * Pure-points false Mahjong: the offender loses the Points Limit to each other player
     * (default -3000 total), regardless of any money configuration.
     */
    public PenaltyTransfer falseMahjongPoints(WinContext ctx) {
        long perPlayer = ctx.getPointsLimit();
        return new PenaltyTransfer(-perPlayer * PenaltyTransfer.OTHER_PLAYERS, perPlayer);
    }

    /**
     * Immediate Flowers/Seasons payment: every other player pays $2 to the revealer at once
     * (the revealer nets +$6, each other player -$2). Not deferred to settlement.
     */
    public PenaltyTransfer flowerReveal() {
        return new PenaltyTransfer(FLOWER_PAYMENT * PenaltyTransfer.OTHER_PLAYERS, -FLOWER_PAYMENT);
    }
}

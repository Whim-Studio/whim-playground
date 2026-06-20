package com.tiwa.mahjong;

import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.engine.Claim;
import com.tiwa.mahjong.engine.ClaimResolver;
import com.tiwa.mahjong.engine.ClaimType;
import com.tiwa.mahjong.model.Player;
import com.tiwa.mahjong.model.StandardMeld;
import com.tiwa.mahjong.model.StandardTile;
import com.tiwa.mahjong.scoring.Penalties;
import com.tiwa.mahjong.scoring.PenaltyTransfer;
import com.tiwa.mahjong.scoring.PayoutCalculator;
import com.tiwa.mahjong.scoring.ScoreCalculator;
import com.tiwa.mahjong.scoring.SpecialHand;
import com.tiwa.mahjong.scoring.WinContext;
import com.tiwa.mahjong.setup.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * End-to-end demonstration that wires Task 1 (model + setup), Task 2 (engine) and Task 3 (scoring)
 * into a single simulated round of Tiwa's Mah Jong, then settles the payouts.
 *
 * <p>This is intentionally headless and deterministic (seeded) so the output is reproducible.
 * Run with: {@code mvn -q exec... } or simply {@code java com.tiwa.mahjong.MahjongDemo} after
 * compiling the module.</p>
 */
public final class MahjongDemo {

    private static final int POINTS_LIMIT = 1000;
    private static final long MONEY_LIMIT = 10L; // $10 money limit (rulebook Section 7 example)

    private MahjongDemo() {
    }

    public static void main(String[] args) {
        long seed = 20260621L;

        line();
        System.out.println("TIWA'S MAH JONG - simulated round (seed=" + seed + ")");
        line();

        // ---- 1. Setup: dice -> seating -> wall -> deal (Task 1) --------------------------------
        GameState game = GameState.newHand(seed);
        System.out.println("Seating & winds (dealer = East, seat 0):");
        for (Player p : game.getMutablePlayers()) {
            System.out.println("  seat " + p.getSeatIndex() + " = " + p.getSeatWind()
                    + "  (" + p.getConcealedTiles().size() + " tiles)");
        }
        System.out.println("Round wind: " + game.getRoundWind()
                + " | wall tiles remaining after deal: " + game.getWall().tilesRemaining()
                + " | current player: seat " + game.getCurrentPlayerIndex());
        System.out.println("Dealing check -> dealer holds "
                + game.getMutablePlayers().get(0).getConcealedTiles().size()
                + " tiles, others hold 13 each.");

        // ---- 2. Claim resolution on a discard (Task 2) ----------------------------------------
        // Seat 1 discards. Seat 2 calls Pung, seat 3 calls Kong, seats 0 and 3 call Mahjong.
        // Priority: Mahjong > Kong > Pung; a multi-Mahjong tie is broken by turn order
        // (nearest counter-clockwise to the discarder). All within the 6-second window.
        int discarder = 1;
        List<Claim> claims = Arrays.asList(
                new Claim(2, ClaimType.PUNG, 1200L),
                new Claim(3, ClaimType.KONG, 800L),
                new Claim(0, ClaimType.MAHJONG, 2500L),
                new Claim(3, ClaimType.MAHJONG, 900L));
        ClaimResolver resolver = new ClaimResolver(); // default 6000 ms timeout
        Optional<Claim> winner = resolver.resolve(claims, discarder);

        line();
        System.out.println("Discard by seat " + discarder + " - competing claims: Pung(s2), Kong(s3), "
                + "Mahjong(s0), Mahjong(s3)");
        System.out.println("Claim window: " + ClaimResolver.CLAIM_TIMEOUT_SECONDS + "s ("
                + ClaimResolver.CLAIM_TIMEOUT_MILLIS + " ms)");
        if (winner.isPresent()) {
            System.out.println("=> Winning claim: " + winner.get().getType()
                    + " by seat " + winner.get().getSeatIndex()
                    + "  (Mahjong beats Kong/Pung; tie broken by counter-clockwise turn order)");
        } else {
            System.out.println("=> No valid claim within the window; tile is unclaimed.");
        }
        int winnerSeat = winner.isPresent() ? winner.get().getSeatIndex() : 0;

        // ---- 3. Score the winning hand (Task 3) ----------------------------------------------
        // Winner's hand: an exposed pung of Red Dragon (claimed) + concealed pungs of Char-5 and
        // Dot-5 (a Double Pung: same rank, two suits) + concealed pung of Bamboo-2, pair of Dot-9.
        WinContext win = WinContext.builder()
                .melds(buildWinningMelds())
                .pair(pair(Suit.DOTS, 9))
                .bonusTiles(new ArrayList<Tile>())
                .dealer(winnerSeat == game.getDealerIndex())
                .specialHand(SpecialHand.NONE)
                .pointsLimit(POINTS_LIMIT)
                .moneyLimit(MONEY_LIMIT)
                .build();

        ScoreCalculator scorer = new ScoreCalculator();
        PayoutCalculator payouts = new PayoutCalculator();

        int base = scorer.basePoints(win);
        int bonus = scorer.mahjongBonus(win);
        int doubles = scorer.countDoubles(win);
        long handPoints = scorer.computeHandPoints(win);
        long moneyPerLoser = payouts.payout(handPoints, win);

        line();
        System.out.println("Winning hand scoring (seat " + winnerSeat + "):");
        System.out.println("  base points (melds + flowers)      = " + base);
        System.out.println("  + Mahjong bonus (1% of " + POINTS_LIMIT + ")   = " + bonus);
        System.out.println("  doubles earned                     = " + doubles + "  (x" + (1L << doubles) + ")");
        System.out.println("  -> hand points (capped at " + POINTS_LIMIT + ")   = " + handPoints);
        System.out.println("  -> currency payout per loser       = $" + moneyPerLoser
                + "  (floor(" + handPoints + " x " + MONEY_LIMIT + " / " + POINTS_LIMIT + "))");

        // ---- 4. Penalties demonstration (Task 3) ---------------------------------------------
        Penalties penalties = new Penalties();
        PenaltyTransfer falseMj = penalties.falseMahjongPoints(win); // pure-points -1000 to each
        PenaltyTransfer flower = penalties.flowerReveal();
        line();
        System.out.println("Penalty examples:");
        System.out.println("  False Mahjong declaration: offender " + falseMj.getSubjectDelta()
                + " points total = pays " + falseMj.getOtherPlayerDelta()
                + " to EACH of " + PenaltyTransfer.OTHER_PLAYERS + " other players");
        System.out.println("  Flower/Season reveal: revealer +$" + flower.getSubjectDelta()
                + " (each other player -$" + (-flower.getOtherPlayerDelta()) + ", paid immediately)");

        // ---- 5. Settle the round (winner collects from the three losers) ----------------------
        line();
        System.out.println("Settlement (winner collects payout from each of the 3 other players):");
        long[] net = new long[4];
        for (int seat = 0; seat < 4; seat++) {
            if (seat == winnerSeat) {
                net[seat] = moneyPerLoser * 3L;
            } else {
                net[seat] = -moneyPerLoser;
            }
        }
        long sum = 0;
        for (int seat = 0; seat < 4; seat++) {
            System.out.println("  seat " + seat + ": " + (net[seat] >= 0 ? "+$" + net[seat] : "-$" + (-net[seat])));
            sum += net[seat];
        }
        System.out.println("  table nets to zero: " + (sum == 0));
        line();
        System.out.println("Done.");
    }

    private static List<Meld> buildWinningMelds() {
        List<Meld> melds = new ArrayList<Meld>();
        // Exposed pung of Red Dragon (claimed from a discard -> exposed honor pung).
        melds.add(StandardMeld.ofIdentical(MeldType.PUNG, new StandardTile(Suit.DRAGON, 1), false));
        // Concealed Double Pung: Characters-5 and Dots-5 (same rank, two suits).
        melds.add(StandardMeld.ofIdentical(MeldType.PUNG, new StandardTile(Suit.CHARACTERS, 5), true));
        melds.add(StandardMeld.ofIdentical(MeldType.PUNG, new StandardTile(Suit.DOTS, 5), true));
        // Concealed pung of Bamboo-2.
        melds.add(StandardMeld.ofIdentical(MeldType.PUNG, new StandardTile(Suit.BAMBOO, 2), true));
        return melds;
    }

    private static Meld pair(Suit suit, int rank) {
        return StandardMeld.ofIdentical(MeldType.PAIR, new StandardTile(suit, rank), true);
    }

    private static void line() {
        System.out.println("----------------------------------------------------------------");
    }
}

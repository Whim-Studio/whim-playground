package com.tiwa.mahjong.scoring;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.Suit;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.tiwa.mahjong.scoring.Stubs.chow;
import static com.tiwa.mahjong.scoring.Stubs.kong;
import static com.tiwa.mahjong.scoring.Stubs.pair;
import static com.tiwa.mahjong.scoring.Stubs.pung;
import static org.junit.Assert.assertEquals;

public class ScoreCalculatorTest {

    private final ScoreCalculator calc = new ScoreCalculator();

    /** Mahjong bonus is 1% of the limit, rounded down; 0 in unlimited games. */
    @Test
    public void mahjongBonus() {
        assertEquals(10, calc.mahjongBonus(WinContext.builder().pointsLimit(1000).build()));
        assertEquals(20, calc.mahjongBonus(WinContext.builder().pointsLimit(2000).build()));
        assertEquals(0, calc.mahjongBonus(WinContext.builder().unlimitedPoints(true).build()));
    }

    /**
     * Quick example 1 (uncapped, all-concealed). The rulebook illustrates base + bonus, then the
     * all-concealed x4 multiplier landing well under the 1000 limit. We use a realisable all-concealed
     * hand: one chow (avoids the No-Chows double) + two concealed simple kongs (8 each) + one concealed
     * honor kong (16) = base 32; (32 + 10) x4 = 168, no cap.
     */
    @Test
    public void allConcealedUncapped() {
        List<Meld> melds = Arrays.asList(
                chow(Suit.BAMBOO, 2, true),
                kong(Suit.DOTS, 2, true),
                kong(Suit.BAMBOO, 5, true),
                kong(Suit.DRAGON, 1, true));
        WinContext ctx = WinContext.builder()
                .melds(melds)
                .pair(pair(Suit.CHARACTERS, 9, true))
                .pointsLimit(1000)
                .build();

        assertEquals(32, calc.basePoints(ctx));
        assertEquals(2, calc.countDoubles(ctx)); // all concealed only
        assertEquals(168, calc.computeHandPoints(ctx));
    }

    /**
     * Quick example 2 (capped). All Honours is 10 doubles; combined with the all-concealed and
     * no-chows doubles the total explodes far past the 1000 limit and is capped at the limit.
     */
    @Test
    public void allHonoursCappedAtLimit() {
        List<Meld> melds = Arrays.asList(
                pung(Suit.WIND, 1, true),
                pung(Suit.WIND, 2, true),
                pung(Suit.DRAGON, 1, true),
                pung(Suit.DRAGON, 2, true));
        WinContext ctx = WinContext.builder()
                .melds(melds)
                .pair(pair(Suit.DRAGON, 3, true))
                .pointsLimit(1000)
                .build();

        assertEquals(1000, calc.computeHandPoints(ctx));
    }

    /** No-Chows adds one double on top of all-concealed: (base + bonus) x8. */
    @Test
    public void noChowsDouble() {
        List<Meld> melds = Arrays.asList(
                pung(Suit.DOTS, 2, true),    // 4
                pung(Suit.BAMBOO, 5, true),  // 4
                pung(Suit.CHARACTERS, 7, true), // 4
                pung(Suit.DOTS, 8, true));   // 4
        WinContext ctx = WinContext.builder()
                .melds(melds)
                .pair(pair(Suit.BAMBOO, 1, true))
                .build();
        // base 16 + 10 = 26; doubles: all concealed (2) + no chows (1) = 3 -> x8 = 208.
        assertEquals(3, calc.countDoubles(ctx));
        assertEquals(208, calc.computeHandPoints(ctx));
    }

    /** Special limit hands receive the bonus but apply NO additional doubles; score = the limit. */
    @Test
    public void specialLimitHandIgnoresDoubles() {
        WinContext ctx = WinContext.builder()
                .specialHand(SpecialHand.THIRTEEN_ORPHANS)
                .heavenlyHand(true) // would be +13 doubles for a normal hand; must be ignored here
                .pointsLimit(1000)
                .build();
        assertEquals(1000, calc.computeHandPoints(ctx));
    }

    /** Mahjong on the first tile is a flat Points-Limit win. */
    @Test
    public void mahjongOnFirstTileIsFlatLimit() {
        WinContext ctx = WinContext.builder()
                .specialHand(SpecialHand.MAHJONG_ON_FIRST_TILE)
                .pointsLimit(1000)
                .build();
        assertEquals(1000, calc.computeHandPoints(ctx));
    }

    /** Flowers/Seasons score 4 points each in the base. */
    @Test
    public void bonusTilesScoreFourEach() {
        WinContext ctx = WinContext.builder()
                .addMeld(chow(Suit.DOTS, 1, true))
                .addMeld(chow(Suit.DOTS, 4, true))
                .addMeld(chow(Suit.BAMBOO, 2, true))
                .addMeld(pung(Suit.WIND, 1, false)) // exposed honor pung = 4
                .pair(pair(Suit.CHARACTERS, 5, true))
                .addBonusTile(Stubs.tile(Suit.FLOWER, 1))
                .addBonusTile(Stubs.tile(Suit.SEASON, 2))
                .build();
        // base: chows 0 + exposed honor pung 4 + 2 bonus tiles * 4 = 12.
        assertEquals(12, calc.basePoints(ctx));
    }
}

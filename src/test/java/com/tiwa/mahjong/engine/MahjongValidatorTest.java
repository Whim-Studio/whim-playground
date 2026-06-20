package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MahjongValidatorTest {

    private final MahjongValidator validator = new MahjongValidator();

    private static Tile d(int r) {
        return Stubs.tile(Suit.DOTS, r);
    }

    private static Tile b(int r) {
        return Stubs.tile(Suit.BAMBOO, r);
    }

    private static Tile c(int r) {
        return Stubs.tile(Suit.CHARACTERS, r);
    }

    @Test
    public void fullyConcealedFourSetsPlusPairIsLegal() {
        // Three chows, one pung, one pair - all in the concealed tiles. 14 tiles total.
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(3),
                b(4), b(5), b(6),
                c(7), c(8), c(9),
                d(5), d(5), d(5),
                b(9), b(9)));
        Tile winning = hand.remove(hand.size() - 1); // win on the 2nd of the pair
        assertTrue(validator.isWinningHand(hand, Collections.<Meld>emptyList(), winning));
    }

    @Test
    public void winWithDeclaredMeldsIsLegal() {
        // One exposed pung declared; remaining concealed = 3 sets + pair (11 tiles incl. winning).
        Meld pung = new Stubs.StubMeld(MeldType.PUNG, false, c(1), c(1), c(1));
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(3),
                b(4), b(5), b(6),
                d(7), d(8), d(9),
                b(2)));
        Tile winning = b(2); // completes the pair
        assertTrue(validator.isWinningHand(hand, Arrays.<Meld>asList(pung), winning));
    }

    @Test
    public void kongDeclaredMeldCountsAsOneSet() {
        Meld kong = new Stubs.StubMeld(MeldType.KONG, true, c(1), c(1), c(1), c(1));
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(3),
                b(4), b(5), b(6),
                d(7), d(8), d(9),
                b(2)));
        Tile winning = b(2);
        assertTrue(validator.isWinningHand(hand, Arrays.<Meld>asList(kong), winning));
    }

    @Test
    public void noPairIsRejected() {
        // 15 tiles that cannot form 4 sets + 1 pair (wrong count too).
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(3),
                b(4), b(5), b(6),
                c(7), c(8), c(9),
                d(5), d(5), d(5),
                b(1), b(2), b(3)));
        Tile winning = c(1);
        assertFalse(validator.isWinningHand(hand, Collections.<Meld>emptyList(), winning));
    }

    @Test
    public void brokenStructureIsRejected() {
        // 14 tiles, correct count, but no valid decomposition.
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(4),
                b(4), b(6), b(8),
                c(1), c(3), c(9),
                d(5), d(7), d(9),
                b(1)));
        Tile winning = b(3);
        assertFalse(validator.isWinningHand(hand, Collections.<Meld>emptyList(), winning));
    }

    @Test
    public void exposedChowIsRejected() {
        // Chow declared as exposed (not concealed) - illegal; chows must be concealed.
        Meld exposedChow = new Stubs.StubMeld(MeldType.CHOW, false, d(1), d(2), d(3));
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                b(4), b(5), b(6),
                d(7), d(8), d(9),
                c(1), c(1), c(1),
                b(2)));
        Tile winning = b(2);
        assertFalse(validator.isWinningHand(hand, Arrays.<Meld>asList(exposedChow), winning));
    }

    @Test
    public void allPungsHandIsLegal() {
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(1), d(1),
                b(2), b(2), b(2),
                c(3), c(3), c(3),
                d(9), d(9), d(9),
                b(5), b(5)));
        Tile winning = hand.remove(hand.size() - 1);
        assertTrue(validator.isWinningHand(hand, Collections.<Meld>emptyList(), winning));
    }

    @Test
    public void bonusTileInHandIsRejected() {
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(
                d(1), d(2), d(3),
                b(4), b(5), b(6),
                c(7), c(8), c(9),
                d(5), d(5), d(5),
                Stubs.tile(Suit.FLOWER, 1)));
        Tile winning = d(5);
        assertFalse(validator.isWinningHand(hand, Collections.<Meld>emptyList(), winning));
    }
}

package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wind;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullyConcealedVerifierTest {

    private final FullyConcealedVerifier verifier = new FullyConcealedVerifier();

    @Test
    public void qualifiesWhenNeverClaimedAndAllMeldsConcealed() {
        Tile red = Stubs.tile(Suit.DRAGON, 1);
        Meld concealedPung = new Stubs.StubMeld(MeldType.PUNG, true, red, red, red);
        PlayerView p = new Stubs.StubPlayerView(0, Wind.EAST,
                Collections.<Tile>emptyList(),
                Arrays.<Meld>asList(concealedPung),
                Collections.<Tile>emptyList(),
                false);
        assertTrue(verifier.verify(p).isFullyConcealed());
    }

    @Test
    public void disqualifiedAfterAnyDiscardClaim() {
        PlayerView p = new Stubs.StubPlayerView(0, Wind.EAST,
                Collections.<Tile>emptyList(),
                Collections.<Meld>emptyList(),
                Collections.<Tile>emptyList(),
                true); // claimed a discard this hand
        assertFalse(verifier.verify(p).isFullyConcealed());
    }

    @Test
    public void disqualifiedByExposedMeld() {
        Tile red = Stubs.tile(Suit.DRAGON, 1);
        Meld exposedPung = new Stubs.StubMeld(MeldType.PUNG, false, red, red, red);
        PlayerView p = new Stubs.StubPlayerView(0, Wind.EAST,
                Collections.<Tile>emptyList(),
                Arrays.<Meld>asList(exposedPung),
                Collections.<Tile>emptyList(),
                false);
        assertFalse(verifier.verify(p).isFullyConcealed());
    }

    @Test
    public void bonusTilesDoNotDisqualify() {
        List<Tile> flowers = Arrays.asList(Stubs.tile(Suit.FLOWER, 1), Stubs.tile(Suit.SEASON, 2));
        PlayerView p = new Stubs.StubPlayerView(0, Wind.EAST,
                Collections.<Tile>emptyList(),
                Collections.<Meld>emptyList(),
                flowers,
                false);
        assertTrue(verifier.verify(p).isFullyConcealed());
    }
}

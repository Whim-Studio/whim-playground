package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wind;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class KongRulesTest {

    private static Tile t(Suit s, int r) {
        return Stubs.tile(s, r);
    }

    @Test
    public void kongFromDiscardRejected() {
        assertFalse(KongRules.canClaimKongFromDiscard());
    }

    @Test
    public void lateKongRejectedAfterDiscard() {
        // Own turn, before discard: allowed.
        assertTrue(KongRules.canDeclareKongNow(true, false));
        // Own turn, but already discarded: rejected.
        assertFalse(KongRules.canDeclareKongNow(true, true));
        // Not own turn: rejected.
        assertFalse(KongRules.canDeclareKongNow(false, false));
    }

    @Test
    public void kongOnLastTileIsDrawnGame() {
        Stubs.StubWall wall = new Stubs.StubWall(
                Arrays.<Tile>asList(t(Suit.DOTS, 5)), true);
        KongResult result = KongRules.drawReplacement(wall);
        assertTrue(result.isDrawnGame());
        assertNull(result.getReplacement());
    }

    @Test
    public void kongWithReplacementDrawsTile() {
        Tile replacement = t(Suit.BAMBOO, 3);
        Stubs.StubWall wall = new Stubs.StubWall(
                Arrays.<Tile>asList(replacement, t(Suit.DOTS, 1)), false);
        KongResult result = KongRules.drawReplacement(wall);
        assertFalse(result.isDrawnGame());
        assertSame(replacement, result.getReplacement());
    }

    @Test
    public void concealedKongNeedsFourMatchingConcealedTiles() {
        Tile west = t(Suit.WIND, Wind.WEST.rank());
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(west, west, west, west));
        PlayerView p = player(hand, Collections.<com.tiwa.mahjong.api.Meld>emptyList());
        assertTrue(KongRules.canDeclareConcealedKong(p, west));

        List<Tile> three = new ArrayList<Tile>(Arrays.asList(west, west, west));
        PlayerView p3 = player(three, Collections.<com.tiwa.mahjong.api.Meld>emptyList());
        assertFalse(KongRules.canDeclareConcealedKong(p3, west));
    }

    @Test
    public void exposedPungUpgradesWhenFourthDrawn() {
        Tile red = t(Suit.DRAGON, 1);
        Stubs.StubMeld exposedPung = new Stubs.StubMeld(MeldType.PUNG, false, red, red, red);
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(red));
        PlayerView p = player(hand, Arrays.<com.tiwa.mahjong.api.Meld>asList(exposedPung));
        assertTrue(KongRules.canUpgradeExposedPung(p, red));
    }

    @Test
    public void concealedPungCannotBeUpgradedAsExposed() {
        Tile red = t(Suit.DRAGON, 1);
        Stubs.StubMeld concealedPung = new Stubs.StubMeld(MeldType.PUNG, true, red, red, red);
        List<Tile> hand = new ArrayList<Tile>(Arrays.asList(red));
        PlayerView p = player(hand, Arrays.<com.tiwa.mahjong.api.Meld>asList(concealedPung));
        assertFalse(KongRules.canUpgradeExposedPung(p, red));
    }

    private static PlayerView player(List<Tile> hand, List<com.tiwa.mahjong.api.Meld> melds) {
        return new Stubs.StubPlayerView(0, Wind.EAST, hand, melds,
                Collections.<Tile>emptyList(), false);
    }
}

package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TileSetTest {

    @Test
    public void fullSetHas144Tiles() {
        List<Tile> tiles = TileFactory.buildFullSet();
        assertEquals(144, tiles.size());
    }

    @Test
    public void multiplicitiesAreCorrect() {
        List<Tile> tiles = TileFactory.buildFullSet();
        Map<Suit, Integer> bySuit = new HashMap<Suit, Integer>();
        Map<String, Integer> byTile = new HashMap<String, Integer>();
        for (Tile t : tiles) {
            bySuit.merge(t.getSuit(), 1, Integer::sum);
            byTile.merge(t.getSuit() + "-" + t.getRank(), 1, Integer::sum);
        }

        assertEquals(36, (int) bySuit.get(Suit.DOTS));      // 9 ranks x4
        assertEquals(36, (int) bySuit.get(Suit.BAMBOO));
        assertEquals(36, (int) bySuit.get(Suit.CHARACTERS));
        assertEquals(16, (int) bySuit.get(Suit.WIND));      // 4 winds x4
        assertEquals(12, (int) bySuit.get(Suit.DRAGON));    // 3 dragons x4
        assertEquals(4, (int) bySuit.get(Suit.FLOWER));     // 4 distinct x1
        assertEquals(4, (int) bySuit.get(Suit.SEASON));

        // Suited tiles appear 4 times, winds/dragons 4 times, bonus tiles once.
        assertEquals(4, (int) byTile.get(Suit.DOTS + "-5"));
        assertEquals(4, (int) byTile.get(Suit.WIND + "-1"));
        assertEquals(4, (int) byTile.get(Suit.DRAGON + "-3"));
        assertEquals(1, (int) byTile.get(Suit.FLOWER + "-2"));
        assertEquals(1, (int) byTile.get(Suit.SEASON + "-4"));
    }

    @Test
    public void tileClassificationFlags() {
        Tile dot1 = new StandardTile(Suit.DOTS, 1);
        Tile dot5 = new StandardTile(Suit.DOTS, 5);
        Tile dot9 = new StandardTile(Suit.DOTS, 9);
        Tile east = new StandardTile(Suit.WIND, 1);
        Tile red = new StandardTile(Suit.DRAGON, 1);
        Tile flower = new StandardTile(Suit.FLOWER, 1);

        assertTrue(dot1.isSuited());
        assertTrue(dot1.isTerminal());
        assertTrue(dot9.isTerminal());
        assertFalse(dot5.isTerminal());
        assertFalse(dot5.isHonor());

        assertTrue(east.isHonor());
        assertFalse(east.isTerminal());
        assertTrue(red.isHonor());
        assertTrue(flower.isBonus());
        assertFalse(flower.isSuited());
    }

    @Test
    public void equalityIsBySuitAndRank() {
        Tile a = new StandardTile(Suit.BAMBOO, 3);
        Tile b = new StandardTile(Suit.BAMBOO, 3);
        Tile c = new StandardTile(Suit.BAMBOO, 4);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }
}

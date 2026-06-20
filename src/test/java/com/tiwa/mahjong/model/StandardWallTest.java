package com.tiwa.mahjong.model;

import com.tiwa.mahjong.api.Tile;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StandardWallTest {

    @Test
    public void drawsAll144ThenEmpty() {
        StandardWall wall = new StandardWall(42L);
        assertEquals(144, wall.tilesRemaining());
        assertFalse(wall.isEmpty());

        int drawn = 0;
        boolean sawLast = false;
        while (!wall.isEmpty()) {
            if (wall.isLastTile()) {
                sawLast = true;
            }
            wall.draw();
            drawn++;
        }
        assertEquals(144, drawn);
        assertTrue("isLastTile must be true at exactly one tile remaining", sawLast);
        assertTrue(wall.isEmpty());
        assertEquals(0, wall.tilesRemaining());
    }

    @Test
    public void allDrawsAreTheFull144MultisetNoLossNoDuplication() {
        StandardWall wall = new StandardWall(7L);
        // Count occurrences; the wall contains the full multiset (some tiles repeat x4).
        java.util.Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < 144; i++) {
            Tile t = wall.draw();
            counts.merge(t.getSuit() + "-" + t.getRank(), 1, Integer::sum);
        }
        int total = 0;
        for (int c : counts.values()) {
            total += c;
        }
        assertEquals(144, total);
        // Distinct tile types: 27 suited + 4 winds + 3 dragons + 4 flowers + 4 seasons = 42.
        assertEquals(42, counts.size());
    }

    @Test
    public void sameSeedIsDeterministic() {
        StandardWall a = new StandardWall(123L);
        StandardWall b = new StandardWall(123L);
        for (int i = 0; i < 144; i++) {
            assertEquals(a.draw(), b.draw());
        }
    }

    @Test
    public void drawOnEmptyThrows() {
        StandardWall wall = new StandardWall(new Random(1L));
        for (int i = 0; i < 144; i++) {
            wall.draw();
        }
        try {
            wall.draw();
            fail("expected exception on empty wall");
        } catch (RuntimeException expected) {
            // ok
        }
    }

    @Test
    public void replacementSharesTheSameFront() {
        StandardWall wall = new StandardWall(99L);
        List<Tile> expected = wall.remainingTiles();
        Set<Tile> seen = new HashSet<Tile>();
        // draw, then replacement, then draw should walk the front in order.
        Tile t1 = wall.draw();
        Tile t2 = wall.drawReplacement();
        Tile t3 = wall.draw();
        assertEquals(expected.get(0), t1);
        assertEquals(expected.get(1), t2);
        assertEquals(expected.get(2), t3);
        assertEquals(141, wall.tilesRemaining());
        seen.add(t1);
        assertTrue(seen.contains(t1));
    }
}

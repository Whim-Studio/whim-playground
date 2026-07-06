package com.whim.starcommand.engine;

import com.whim.starcommand.model.Planet;
import com.whim.starcommand.model.UniqueArea;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for unique-area generation invariants. */
public class UniqueAreaGenTest {

    private UniqueArea gen(long seed, boolean boss) {
        Planet p = new Planet("Base-1", Planet.Kind.PIRATE_BASE);
        return new UniqueAreaGen(new Rng(seed)).generate(p, boss);
    }

    @Test
    public void entranceAndObjectivePlacedAtOppositeCorners() {
        for (long s = 1; s <= 30; s++) {
            UniqueArea a = gen(s, s % 2 == 0);
            assertEquals(UniqueArea.Room.Kind.ENTRANCE, a.at(a.rows - 1, 0).kind);
            assertEquals(UniqueArea.Room.Kind.OBJECTIVE, a.at(0, a.cols - 1).kind);
            assertEquals(a.rows - 1, a.pr);
            assertEquals(0, a.pc);
            assertTrue(a.at(a.rows - 1, 0).visited);
            assertTrue(a.at(a.rows - 1, 0).cleared);
        }
    }

    @Test
    public void hasExactlyOneObjectiveAndSomeEnemiesAndLoot() {
        UniqueArea a = gen(5L, true);
        int obj = 0, foes = 0, loot = 0;
        for (int r = 0; r < a.rows; r++)
            for (int c = 0; c < a.cols; c++) {
                UniqueArea.Room room = a.at(r, c);
                if (room.kind == UniqueArea.Room.Kind.OBJECTIVE) obj++;
                if (room.kind == UniqueArea.Room.Kind.ENEMY) foes++;
                if (room.kind == UniqueArea.Room.Kind.LOOT) { loot++; assertTrue(room.loot > 0); }
            }
        assertEquals(1, obj);
        assertEquals(4, foes); // boss areas seed four enemy rooms
        assertEquals(3, loot);
    }

    @Test
    public void objectiveSecuredTracksClearedFlag() {
        UniqueArea a = gen(2L, false);
        assertTrue(!a.objectiveSecured());
        a.at(0, a.cols - 1).cleared = true;
        assertTrue(a.objectiveSecured());
    }

    @Test
    public void revealMarksRoomsReachableThroughDoorways() {
        for (long s = 1; s <= 30; s++) {
            UniqueArea a = gen(s, false);
            assertTrue(a.at(a.rows - 1, 0).discovered); // the entrance itself
            // every revealed neighbour must be connected by a doorway
            int er = a.rows - 1, ec = 0;
            int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dd : d) {
                int nr = er + dd[0], nc = ec + dd[1];
                if (a.inBounds(nr, nc) && a.at(nr, nc).discovered)
                    assertTrue("revealed neighbour must be reachable", a.canMove(er, ec, nr, nc));
            }
        }
    }

    @Test
    public void objectiveIsAlwaysReachableFromEntrance() {
        for (long s = 1; s <= 60; s++) {
            UniqueArea a = gen(s, s % 2 == 0);
            boolean[][] seen = new boolean[a.rows][a.cols];
            java.util.Deque<int[]> q = new java.util.ArrayDeque<int[]>();
            seen[a.rows - 1][0] = true;
            q.add(new int[]{a.rows - 1, 0});
            int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            while (!q.isEmpty()) {
                int[] cur = q.poll();
                for (int[] dd : d) {
                    int nr = cur[0] + dd[0], nc = cur[1] + dd[1];
                    if (a.inBounds(nr, nc) && !seen[nr][nc] && a.canMove(cur[0], cur[1], nr, nc)) {
                        seen[nr][nc] = true;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
            assertTrue("objective must be reachable (seed " + s + ")", seen[0][a.cols - 1]);
            // maze spanning tree => every room reachable
            for (int r = 0; r < a.rows; r++)
                for (int c = 0; c < a.cols; c++)
                    assertTrue("all rooms reachable", seen[r][c]);
        }
    }
}

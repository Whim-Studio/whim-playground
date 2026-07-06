package com.whim.starcommand.engine;

import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.GroundUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Headless tests for the tactical ground/boarding engine. */
public class GroundCombatTest {

    private GroundCombat boardingBattle(long seed) {
        Rng rng = new Rng(seed);
        GroundCombat gc = new GroundCombat(rng, 10, 8);
        CharacterGen gen = new CharacterGen(rng);
        for (int i = 0; i < 4; i++) gc.addPlayer(gen.roll("Ally" + i, "Marine"), 0, i);
        for (int i = 0; i < 3; i++) gc.addEnemy("Pirate" + i, 9, i, 14, 52, 3, 6, 2, 3);
        return gc;
    }

    @Test
    public void movesAreBoundedByRangeAndBlockedTiles() {
        Rng rng = new Rng(1L);
        GroundCombat gc = new GroundCombat(rng, 6, 6);
        GroundUnit a = gc.addEnemy("A", 0, 0, 10, 50, 1, 2, 2, 1);
        assertTrue(gc.move(a, 2, 0));       // within move range
        assertTrue(!gc.move(a, 5, 5));      // too far
        gc.addEnemy("B", 3, 0, 10, 50, 1, 2, 2, 1);
        assertTrue(!gc.move(a, 3, 0));      // occupied
    }

    @Test
    public void boardingBattleTerminatesWithAValidResult() {
        for (long seed = 1; seed <= 40; seed++) {
            GroundCombat gc = boardingBattle(seed);
            int guard = 0;
            while (gc.result == GroundCombat.Result.ONGOING && guard++ < 300) {
                // greedy player policy: every ready unit attacks the nearest enemy in range,
                // otherwise steps toward it, then end the turn.
                for (GroundUnit u : new ArrayList<GroundUnit>(gc.units)) {
                    if (u.side != GroundUnit.Side.PLAYER || !u.alive() || u.acted) continue;
                    GroundUnit t = nearestEnemy(gc, u);
                    if (t != null && u.dist(t) <= u.attackRange) {
                        gc.attack(u, t, new ArrayList<String>());
                    } else if (t != null) {
                        stepToward(gc, u, t);
                    }
                }
                gc.endPlayerTurn();
            }
            assertTrue("must terminate", gc.result != GroundCombat.Result.ONGOING);
        }
    }

    @Test
    public void woundsWriteBackToCrew() {
        Rng rng = new Rng(3L);
        GroundCombat gc = new GroundCombat(rng, 6, 6);
        Character c = new CharacterGen(rng).roll("Hero", "Marine");
        GroundUnit u = gc.addPlayer(c, 0, 0);
        u.hp = 3;
        gc.writeBackWounds();
        assertEquals(3, c.hp);
        assertTrue(c.alive);
    }

    private GroundUnit nearestEnemy(GroundCombat gc, GroundUnit from) {
        GroundUnit best = null; int bd = Integer.MAX_VALUE;
        for (GroundUnit u : gc.units) {
            if (u.side != GroundUnit.Side.ENEMY || !u.alive()) continue;
            int d = from.dist(u);
            if (d < bd) { bd = d; best = u; }
        }
        return best;
    }

    private void stepToward(GroundCombat gc, GroundUnit u, GroundUnit t) {
        int nx = u.x + Integer.signum(t.x - u.x);
        if (!gc.move(u, nx, u.y)) gc.move(u, u.x, u.y + Integer.signum(t.y - u.y));
    }
}

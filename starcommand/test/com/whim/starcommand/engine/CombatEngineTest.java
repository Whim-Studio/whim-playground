package com.whim.starcommand.engine;

import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Headless tests for the Swing-independent game logic. */
public class CombatEngineTest {

    private Ship raider() {
        Ship e = Content.makeShip("Pirate Raider", 55, 25, 5, 2);
        e.weapons.add(new Weapon("Pulse", Weapon.Type.BEAM, 4, 9, 80, 0));
        return e;
    }

    @Test
    public void shieldsAbsorbBeforeHull() {
        Ship s = Content.makeShip("Test", 100, 30, 5, 2);
        boolean disabled = s.takeDamage(20);
        assertEquals(10, s.shield);
        assertEquals(100, s.hull);
        assertTrue(!disabled);
    }

    @Test
    public void hullZeroDisablesRatherThanDestroys() {
        Ship s = Content.makeShip("Test", 10, 0, 5, 2);
        boolean disabled = s.takeDamage(50);
        assertTrue(disabled);
        assertTrue(s.disabled);
        assertEquals(0, s.hull);
    }

    @Test
    public void startingScoutBeatsOpeningRaiderDeterministically() {
        int wins = 0;
        for (long seed = 1; seed <= 50; seed++) {
            Rng rng = new Rng(seed);
            Ship player = Content.startingShip();
            Character cap = new CharacterGen(rng).roll("C", "Marine");
            CombatEngine ce = new CombatEngine(rng, player, raider(), cap);
            int r = 0;
            while (ce.result == CombatEngine.Result.ONGOING && r < 60) {
                ce.round(CombatEngine.Action.FIRE_BEAM);
                r++;
            }
            if (ce.result != CombatEngine.Result.PLAYER_DESTROYED) wins++;
        }
        assertEquals("opening fight must be winnable with starting gear", 50, wins);
    }

    @Test
    public void characterStatsAreInValidRange() {
        Rng rng = new Rng(7L);
        CharacterGen gen = new CharacterGen(rng);
        for (int i = 0; i < 100; i++) {
            Character c = gen.roll("X", "Pilot");
            assertTrue(c.strength >= 3 && c.strength <= 20);
            assertTrue(c.maxHp > 0);
            assertTrue(c.statTotal() >= 18);
        }
    }
}

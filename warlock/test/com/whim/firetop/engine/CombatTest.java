package com.whim.firetop.engine;

import com.whim.firetop.model.Character;
import com.whim.firetop.model.Monster;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies opposed-2d6 combat and the luck combat modifiers. */
public class CombatTest {

    @Test
    public void playerWinsDealsBaseDamage() {
        // Player SKILL 12 (AS 14..24) always beats monster SKILL 1 (AS 3..13).
        Character c = new Character("Hero", 12, 20, 10);
        Monster m = new Monster("Weakling", 1, 10, "");
        Dice d = new Dice(21L);
        Combat.RoundResult r = Combat.resolveRound(c, m, d);
        assertEquals(Combat.Outcome.PLAYER_WINS, r.getOutcome());
        assertEquals("monster loses base 2", 8, m.getStamina());
        assertEquals("player untouched", 20, c.getStaminaCurrent());
    }

    @Test
    public void monsterWinsDealsBaseDamage() {
        // Monster SKILL 12 always beats player SKILL 1.
        Character c = new Character("Hero", 1, 20, 10);
        Monster m = new Monster("Brute", 12, 10, "");
        Dice d = new Dice(22L);
        Combat.RoundResult r = Combat.resolveRound(c, m, d);
        assertEquals(Combat.Outcome.MONSTER_WINS, r.getOutcome());
        assertEquals("player loses base 2", 18, c.getStaminaCurrent());
        assertEquals("monster untouched", 10, m.getStamina());
    }

    @Test
    public void tieDealsNoDamage() {
        // Equal skill: sweep seeds until a tie occurs, assert no damage on ties.
        boolean sawTie = false;
        for (long seed = 0; seed < 500 && !sawTie; seed++) {
            Character c = new Character("Hero", 8, 20, 10);
            Monster m = new Monster("Mirror", 8, 10, "");
            Dice d = new Dice(seed);
            Combat.RoundResult r = Combat.resolveRound(c, m, d);
            if (r.getOutcome() == Combat.Outcome.TIE) {
                sawTie = true;
                assertEquals("tie: monster unchanged", 10, m.getStamina());
                assertEquals("tie: player unchanged", 20, c.getStaminaCurrent());
                assertEquals("equal attack strengths on a tie",
                        r.getPlayerAttackStrength(), r.getMonsterAttackStrength());
            }
        }
        assertTrue("a tie should occur across many seeds", sawTie);
    }

    @Test
    public void luckToAttackLuckyAddsDamage() {
        // LUCK 12 => always Lucky => 2 extra damage on top of the base already dealt.
        Character c = new Character("Hero", 10, 20, 12);
        Monster m = new Monster("Foe", 8, 10, "");
        m.wound(Combat.BASE_DAMAGE); // base already applied -> 8
        Dice d = new Dice(1L);
        boolean lucky = Combat.applyLuckToAttack(c, m, d);
        assertTrue(lucky);
        assertEquals("4 total damage (2 base + 2 luck)", 6, m.getStamina());
    }

    @Test
    public void luckToAttackUnluckyGrazes() {
        // LUCK 0 => always Unlucky => monster recovers 1 (net 1 damage this round).
        Character c = new Character("Hero", 10, 20, 0);
        Monster m = new Monster("Foe", 8, 10, "");
        m.wound(Combat.BASE_DAMAGE); // -> 8
        Dice d = new Dice(2L);
        boolean lucky = Combat.applyLuckToAttack(c, m, d);
        assertFalse(lucky);
        assertEquals("net 1 damage (2 base - 1 recovered)", 9, m.getStamina());
    }

    @Test
    public void luckToDefenseLuckyReducesLoss() {
        // LUCK 12 => Lucky => recover 1 (net 1 lost this round).
        Character c = new Character("Hero", 10, 20, 12);
        c.loseStamina(Combat.BASE_DAMAGE); // -> 18
        Dice d = new Dice(4L);
        boolean lucky = Combat.applyLuckToDefense(c, d);
        assertTrue(lucky);
        assertEquals("net 1 lost (2 base - 1 recovered)", 19, c.getStaminaCurrent());
    }

    @Test
    public void luckToDefenseUnluckyIncreasesLoss() {
        // LUCK 0 => Unlucky => lose 1 more (net 3 lost this round).
        Character c = new Character("Hero", 10, 20, 0);
        c.loseStamina(Combat.BASE_DAMAGE); // -> 18
        Dice d = new Dice(6L);
        boolean lucky = Combat.applyLuckToDefense(c, d);
        assertFalse(lucky);
        assertEquals("net 3 lost (2 base + 1 extra)", 17, c.getStaminaCurrent());
    }
}

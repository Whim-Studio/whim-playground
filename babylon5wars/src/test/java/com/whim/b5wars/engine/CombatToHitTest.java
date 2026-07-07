package com.whim.b5wars.engine;

import static org.junit.Assert.assertTrue;

import com.whim.b5wars.data.CriticalEntry;
import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Dice;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponArc;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;

/** Verifies the to-hit modifier stack pushes the target number in the expected direction. */
public class CombatToHitTest {

    private CombatEngine combat;
    private Ship attacker;
    private Ship target;

    private Hex ahead(Hex from, Facing f, int n) {
        Hex h = from;
        for (int i = 0; i < n; i++) {
            h = h.neighbor(f);
        }
        return h;
    }

    @Before
    public void setUp() {
        combat = new CombatEngine(new Dice(1L), new ArrayList<CriticalEntry>());
        Weapon fwd = Fixtures.weapon("beam", WeaponArc.all(), new int[] {2, 5, 12}, 10, 5);
        ShipClass ac = Fixtures.shipClass("gun", 200, 8, 1, 8, 8, 4, 10, DefenseType.ARMOR,
                Fixtures.weapons(fwd));
        attacker = Fixtures.ship(ac, Side.A, new Hex(0, 0), Facing.F, 0);
        ShipClass tc = Fixtures.shipClass("tgt", 200, 8, 1, 8, 8, 4, 10, DefenseType.ARMOR,
                Fixtures.weapons());
        target = Fixtures.ship(tc, Side.B, ahead(new Hex(0, 0), Facing.F, 1), Facing.B, 0);
    }

    @Test
    public void longerRangeRaisesTargetNumber() {
        target.setPos(ahead(attacker.getPos(), Facing.F, 1));
        int near = combat.toHitTarget(attacker, 0, target);
        target.setPos(ahead(attacker.getPos(), Facing.F, 10)); // higher range bracket
        int far = combat.toHitTarget(attacker, 0, target);
        assertTrue("farther target is harder to hit (" + near + " -> " + far + ")", far > near);
    }

    @Test
    public void fasterTargetRaisesTargetNumber() {
        target.setSpeed(0);
        int slow = combat.toHitTarget(attacker, 0, target);
        target.setSpeed(10);
        int fast = combat.toHitTarget(attacker, 0, target);
        assertTrue("faster target is harder to hit (" + slow + " -> " + fast + ")", fast > slow);
    }

    @Test
    public void offensiveEwLowersTargetNumber() {
        attacker.setEwOffensive(0);
        target.setEwDefensive(0);
        int base = combat.toHitTarget(attacker, 0, target);
        attacker.setEwOffensive(6);
        int withEw = combat.toHitTarget(attacker, 0, target);
        assertTrue("offensive EW makes the shot easier (" + base + " -> " + withEw + ")",
                withEw < base);
    }
}

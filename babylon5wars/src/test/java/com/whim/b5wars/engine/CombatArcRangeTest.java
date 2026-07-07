package com.whim.b5wars.engine;

import static org.junit.Assert.assertFalse;
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

public class CombatArcRangeTest {

    private CombatEngine combat;
    private Ship attacker;

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
        Weapon fwd = Fixtures.weapon("beam", WeaponArc.forward(), new int[] {2, 5, 10}, 10, 5);
        ShipClass c = Fixtures.shipClass("gun", 200, 8, 1, 8, 8, 4, 10, DefenseType.ARMOR,
                Fixtures.weapons(fwd));
        attacker = Fixtures.ship(c, Side.A, new Hex(0, 0), Facing.F, 0);
    }

    private Ship targetAt(Hex pos) {
        ShipClass c = Fixtures.shipClass("tgt", 200, 8, 1, 8, 8, 4, 10, DefenseType.ARMOR,
                Fixtures.weapons());
        return Fixtures.ship(c, Side.B, pos, Facing.B, 0);
    }

    @Test
    public void inArcAndInRange() {
        Ship t = targetAt(ahead(attacker.getPos(), Facing.F, 3));
        assertTrue(combat.inArcAndRange(attacker, 0, t));
    }

    @Test
    public void outOfArcBehind() {
        Ship t = targetAt(ahead(attacker.getPos(), Facing.B, 3));
        assertFalse("target behind a forward-arc weapon", combat.inArcAndRange(attacker, 0, t));
    }

    @Test
    public void outOfRangeAhead() {
        Ship t = targetAt(ahead(attacker.getPos(), Facing.F, 12)); // maxRange 10
        assertFalse("beyond max range bracket", combat.inArcAndRange(attacker, 0, t));
    }
}

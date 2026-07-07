package com.whim.b5wars.engine;

import static org.junit.Assert.assertEquals;
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
import com.whim.b5wars.model.WeaponTrait;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CombatDamageTest {

    private Hex ahead(Hex from, Facing f, int n) {
        Hex h = from;
        for (int i = 0; i < n; i++) {
            h = h.neighbor(f);
        }
        return h;
    }

    private Ship attackerWith(List<Weapon> weapons) {
        ShipClass c = Fixtures.shipClass("gun", 200, 8, 1, 8, 8, 0, 10, DefenseType.ARMOR, weapons);
        return Fixtures.ship(c, Side.A, new Hex(0, 0), Facing.F, 0);
    }

    private Ship targetWith(int armor, int structure, List<Weapon> weapons) {
        ShipClass c = Fixtures.shipClass("tgt", 200, 8, 1, 8, 8, armor, structure,
                DefenseType.ARMOR, weapons);
        // Placed 2 hexes ahead of a Facing.F attacker at origin.
        return Fixtures.ship(c, Side.B, ahead(new Hex(0, 0), Facing.F, 2), Facing.B, 0);
    }

    @Test
    public void damageErodesArmorThenStructure() {
        CombatEngine combat = new CombatEngine(new Dice(7L), new ArrayList<CriticalEntry>());
        Weapon w = Fixtures.weapon("beam", WeaponArc.all(), new int[] {10}, 5, 8); // 8 damage
        Ship attacker = attackerWith(Fixtures.weapons(w));
        Ship target = targetWith(5, 10, Fixtures.weapons()); // armor 5 per facing

        int before = target.totalStructureRemaining();
        combat.resolveDamage(attacker, 0, target);
        // 8 damage - 5 armor absorbed = 3 penetrating structure.
        assertEquals(3, before - target.totalStructureRemaining());
    }

    @Test
    public void armorPiercingHalvesArmorEffect() {
        Weapon plain = Fixtures.weapon("beam", WeaponArc.all(), new int[] {10}, 5, 8);
        Weapon ap = Fixtures.weapon("lance", WeaponArc.all(), new int[] {10}, 5, 8,
                WeaponTrait.ARMOR_PIERCING);

        CombatEngine c1 = new CombatEngine(new Dice(7L), new ArrayList<CriticalEntry>());
        Ship a1 = attackerWith(Fixtures.weapons(plain));
        Ship t1 = targetWith(6, 20, Fixtures.weapons());
        int b1 = t1.totalStructureRemaining();
        c1.resolveDamage(a1, 0, t1);
        int plainPen = b1 - t1.totalStructureRemaining(); // 8 - 6 = 2

        CombatEngine c2 = new CombatEngine(new Dice(7L), new ArrayList<CriticalEntry>());
        Ship a2 = attackerWith(Fixtures.weapons(ap));
        Ship t2 = targetWith(6, 20, Fixtures.weapons());
        int b2 = t2.totalStructureRemaining();
        c2.resolveDamage(a2, 0, t2);
        int apPen = b2 - t2.totalStructureRemaining(); // 8 - (6/2=3) = 5

        assertTrue("AP penetrates more (" + plainPen + " vs " + apPen + ")", apPen > plainPen);
    }

    @Test
    public void interceptorReducesIncomingBallisticDamage() {
        Weapon missile = Fixtures.weapon("missile", WeaponArc.all(), new int[] {10}, 5, 8,
                WeaponTrait.BALLISTIC);
        Weapon pd = Fixtures.weapon("pdc", WeaponArc.all(), new int[] {5}, 5, 1,
                WeaponTrait.INTERCEPTOR);

        // Target WITHOUT interceptors.
        CombatEngine c1 = new CombatEngine(new Dice(7L), new ArrayList<CriticalEntry>());
        Ship a1 = attackerWith(Fixtures.weapons(missile));
        Ship t1 = targetWith(0, 30, Fixtures.weapons());
        int b1 = t1.totalStructureRemaining();
        c1.resolveDamage(a1, 0, t1);
        int noPd = b1 - t1.totalStructureRemaining(); // 8

        // Target WITH one interceptor mount.
        CombatEngine c2 = new CombatEngine(new Dice(7L), new ArrayList<CriticalEntry>());
        Ship a2 = attackerWith(Fixtures.weapons(missile));
        Ship t2 = targetWith(0, 30, Fixtures.weapons(pd));
        int b2 = t2.totalStructureRemaining();
        c2.resolveDamage(a2, 0, t2);
        int withPd = b2 - t2.totalStructureRemaining(); // 8 - 2 = 6

        assertTrue("interceptors reduce ballistic damage (" + noPd + " vs " + withPd + ")",
                withPd < noPd);
    }

    @Test
    public void reactorCritReducesThrust() {
        List<CriticalEntry> table = new ArrayList<CriticalEntry>();
        table.add(new CriticalEntry(1, 20, "REACTOR")); // any roll -> REACTOR
        CombatEngine combat = new CombatEngine(new Dice(3L), table);
        Weapon w = Fixtures.weapon("beam", WeaponArc.all(), new int[] {10}, 5, 8);
        Ship attacker = attackerWith(Fixtures.weapons(w));
        Ship target = targetWith(0, 2, Fixtures.weapons()); // low structure -> depletion -> crit
        target.setThrustAvailable(5);

        combat.resolveDamage(attacker, 0, target);
        assertEquals("REACTOR crit reduces thrust by REACTOR_CRIT_THRUST_LOSS",
                5 - CombatEngine.REACTOR_CRIT_THRUST_LOSS, target.getThrustAvailable());
    }

    @Test
    public void weaponCritDisablesAWeapon() {
        List<CriticalEntry> table = new ArrayList<CriticalEntry>();
        table.add(new CriticalEntry(1, 20, "WEAPON")); // any roll -> WEAPON
        CombatEngine combat = new CombatEngine(new Dice(3L), table);
        Weapon w = Fixtures.weapon("beam", WeaponArc.all(), new int[] {10}, 5, 8);
        Ship attacker = attackerWith(Fixtures.weapons(w));
        Ship target = targetWith(0, 2, Fixtures.weapons(
                Fixtures.weapon("tgun", WeaponArc.all(), new int[] {5}, 5, 3)));

        combat.resolveDamage(attacker, 0, target);
        assertTrue("target weapon 0 disabled by crit",
                target.getReloadReadyTurn(0) >= CombatEngine.WEAPON_DISABLED_TURN);
    }
}

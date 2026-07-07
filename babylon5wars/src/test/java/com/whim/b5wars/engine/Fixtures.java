package com.whim.b5wars.engine;

import com.whim.b5wars.model.DamageProfile;
import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Race;
import com.whim.b5wars.model.Section;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;
import com.whim.b5wars.model.Special;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponArc;
import com.whim.b5wars.model.WeaponTrait;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small deterministic model builders shared by the engine tests. */
final class Fixtures {

    private Fixtures() {
    }

    static Map<Facing, Integer> uniformArmor(int value) {
        Map<Facing, Integer> m = new EnumMap<Facing, Integer>(Facing.class);
        for (Facing f : Facing.values()) {
            m.put(f, Integer.valueOf(value));
        }
        return m;
    }

    static Map<Section, Integer> uniformStructure(int value) {
        Map<Section, Integer> m = new EnumMap<Section, Integer>(Section.class);
        for (Section s : Section.values()) {
            m.put(s, Integer.valueOf(value));
        }
        return m;
    }

    static Weapon weapon(String name, WeaponArc arc, int[] brackets, int baseToHit,
                         int damagePlus, WeaponTrait... traits) {
        Set<WeaponTrait> ts = new HashSet<WeaponTrait>();
        for (WeaponTrait t : traits) {
            ts.add(t);
        }
        // DamageProfile(count=0, sides=1, plus) rolls to exactly `plus` — deterministic damage.
        return new Weapon(name, name, arc, brackets, baseToHit,
                new DamageProfile(0, 1, damagePlus), 0, ts);
    }

    /** Ship class with uniform armor/structure and the given weapon list. */
    static ShipClass shipClass(String id, int points, int maxSpeed, int turnMode, int thrust,
                               int power, int armor, int structure, DefenseType defense,
                               List<Weapon> weapons) {
        return new ShipClass(id, id, Race.EARTH_ALLIANCE, points, maxSpeed, turnMode, thrust,
                power, 0, 4, 5, 6, uniformArmor(armor), uniformStructure(structure), defense,
                weapons, new ArrayList<Special>());
    }

    static List<Weapon> weapons(Weapon... w) {
        List<Weapon> list = new ArrayList<Weapon>();
        for (Weapon x : w) {
            list.add(x);
        }
        return list;
    }

    static Ship ship(ShipClass type, Side side, Hex pos, Facing facing, int speed) {
        return new Ship(type, side, pos, facing, speed);
    }
}

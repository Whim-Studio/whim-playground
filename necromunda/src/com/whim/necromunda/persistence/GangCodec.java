package com.whim.necromunda.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.whim.necromunda.engine.data.WeaponCatalogue;
import com.whim.necromunda.model.Armour;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.House;
import com.whim.necromunda.model.Stat;
import com.whim.necromunda.model.StatLine;
import com.whim.necromunda.model.Weapon;

/**
 * Maps a {@link Gang} to/from the plain-object JSON model of {@link Json}.
 *
 * <p>Weapons are stored by catalogue id and resolved back through
 * {@link WeaponCatalogue} on load. Only base stats are persisted (in-battle
 * modifiers are transient). The round-trip is stable:
 * {@code toJson(fromJson(x)).equals(x)} for any gang this codec produced.
 */
public final class GangCodec {

    private GangCodec() {
    }

    public static Map<String, Object> toJson(Gang gang) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("name", gang.name());
        root.put("house", gang.house().name());
        root.put("credits", Long.valueOf(gang.credits()));

        List<Object> territories = new ArrayList<Object>();
        for (String t : gang.territories()) {
            territories.add(t);
        }
        root.put("territories", territories);

        List<Object> fighters = new ArrayList<Object>();
        for (Fighter f : gang.roster()) {
            fighters.add(fighterToJson(f));
        }
        root.put("fighters", fighters);
        return root;
    }

    private static Map<String, Object> fighterToJson(Fighter f) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", f.id());
        m.put("name", f.name());
        m.put("type", f.type().name());

        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        for (Stat s : Stat.values()) {
            stats.put(s.name(), Long.valueOf(f.stats().base(s)));
        }
        m.put("stats", stats);

        m.put("armour", f.armour().name());

        List<Object> weapons = new ArrayList<Object>();
        for (Weapon w : f.weapons()) {
            weapons.add(w.id());
        }
        m.put("weapons", weapons);

        m.put("xp", Long.valueOf(f.experience()));
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Gang fromJson(Map<String, Object> root) {
        String name = (String) root.get("name");
        House house = House.valueOf((String) root.get("house"));
        Gang gang = new Gang(name, house);
        gang.setCredits(asInt(root.get("credits")));

        List<Object> territories = (List<Object>) root.get("territories");
        if (territories != null) {
            for (Object t : territories) {
                gang.territories().add((String) t);
            }
        }

        List<Object> fighters = (List<Object>) root.get("fighters");
        if (fighters != null) {
            for (Object o : fighters) {
                gang.add(fighterFromJson((Map<String, Object>) o));
            }
        }
        return gang;
    }

    @SuppressWarnings("unchecked")
    private static Fighter fighterFromJson(Map<String, Object> m) {
        String id = (String) m.get("id");
        String name = (String) m.get("name");
        FighterType type = FighterType.valueOf((String) m.get("type"));

        Map<String, Object> stats = (Map<String, Object>) m.get("stats");
        StatLine line = new StatLine();
        for (Stat s : Stat.values()) {
            line.setBase(s, asInt(stats.get(s.name())));
        }

        Fighter f = new Fighter(id, name, type, line);
        f.setArmour(Armour.valueOf((String) m.get("armour")));

        List<Object> weapons = (List<Object>) m.get("weapons");
        if (weapons != null) {
            for (Object wid : weapons) {
                Weapon w = WeaponCatalogue.byId((String) wid);
                if (w != null) {
                    f.addWeapon(w);
                }
            }
        }
        f.setExperience(asInt(m.get("xp")));
        return f;
    }

    private static int asInt(Object o) {
        return ((Number) o).intValue();
    }
}

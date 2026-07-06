package com.whim.starcommand.engine;

import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data-driven content: ships, weapons and crew roles are loaded from JSON
 * resources under {@code com/whim/starcommand/data/} rather than hardcoded, so
 * the game is tunable without recompiling. Parsed once and cached.
 */
public final class Content {

    private Content() { }

    private static final String DIR = "/com/whim/starcommand/data/";

    private static List<Object> weaponData;
    private static List<Object> shipData;
    private static String[] roleData;

    @SuppressWarnings("unchecked")
    private static synchronized void ensureLoaded() {
        if (weaponData != null) return;
        weaponData = (List<Object>) Json.parse(readResource(DIR + "weapons.json"));
        shipData = (List<Object>) Json.parse(readResource(DIR + "ships.json"));
        List<Object> roles = (List<Object>) Json.parse(readResource(DIR + "roles.json"));
        String[] arr = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) arr[i] = (String) roles.get(i);
        roleData = arr;
    }

    /** Crew roles the player can assign at character creation. */
    public static String[] roles() {
        ensureLoaded();
        return roleData.clone();
    }

    /** Weapons available for purchase, in listing order. */
    public static List<Weapon> weaponShop() {
        ensureLoaded();
        List<Weapon> list = new ArrayList<Weapon>();
        for (Object o : weaponData) list.add(toWeapon(asMap(o)));
        return list;
    }

    /** Ship classes (templates) that can be bought or traded up to. */
    public static List<Ship> shipShop() {
        ensureLoaded();
        List<Ship> list = new ArrayList<Ship>();
        for (Object o : shipData) {
            Map<String, Object> m = asMap(o);
            list.add(makeShip(asStr(m, "class"), asInt(m, "hull"),
                    asInt(m, "shield"), asInt(m, "engines"), asInt(m, "slots")));
        }
        return list;
    }

    public static int shipCost(String className) {
        ensureLoaded();
        for (Object o : shipData) {
            Map<String, Object> m = asMap(o);
            if (className.equals(asStr(m, "class"))) return asInt(m, "cost");
        }
        return 0;
    }

    public static Ship makeShip(String className, int hull, int shield, int engines, int slots) {
        return new Ship(className, hull, shield, engines, slots);
    }

    /** The starting player ship (the entry flagged {@code "start": true}). */
    public static Ship startingShip() {
        ensureLoaded();
        for (Object o : shipData) {
            Map<String, Object> m = asMap(o);
            if (asBool(m, "start")) {
                Ship s = makeShip(asStr(m, "class"), asInt(m, "hull"),
                        asInt(m, "shield"), asInt(m, "engines"), asInt(m, "slots"));
                Object w = m.get("weapons");
                if (w instanceof List) {
                    for (Object wn : (List<?>) w) {
                        Weapon wp = findWeapon((String) wn);
                        if (wp != null) s.weapons.add(wp);
                    }
                }
                return s;
            }
        }
        throw new IllegalStateException("no starting ship flagged in ships.json");
    }

    private static Weapon toWeapon(Map<String, Object> m) {
        return new Weapon(asStr(m, "name"), Weapon.Type.valueOf(asStr(m, "type")),
                asInt(m, "min"), asInt(m, "max"), asInt(m, "accuracy"), asInt(m, "cost"));
    }

    private static Weapon findWeapon(String name) {
        for (Object o : weaponData) {
            Map<String, Object> m = asMap(o);
            if (name.equals(asStr(m, "name"))) return toWeapon(m);
        }
        return null;
    }

    // --- tiny typed accessors over the parsed JSON maps ---

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) { return (Map<String, Object>) o; }

    private static int asInt(Map<String, Object> m, String key) {
        return ((Double) m.get(key)).intValue();
    }

    private static String asStr(Map<String, Object> m, String key) {
        return (String) m.get(key);
    }

    private static boolean asBool(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof Boolean && (Boolean) o;
    }

    private static String readResource(String path) {
        InputStream in = Content.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("missing data resource: " + path
                + " (run with the data files on the classpath)");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("failed reading " + path, e);
        } finally {
            try { in.close(); } catch (IOException ignore) { /* no-op */ }
        }
    }
}

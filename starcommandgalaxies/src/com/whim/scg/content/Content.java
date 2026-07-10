package com.whim.scg.content;

import com.whim.scg.api.Defs;
import com.whim.scg.api.Enums;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the seed content JSON ({@code com/whim/scg/data/*.json}) into the
 * immutable {@link Defs} templates. Files are read from the classpath so the
 * engine works whether run from {@code target/classes} or a jar.
 */
public final class Content {

    private final Map<String, Defs.WeaponDef> weapons = new LinkedHashMap<String, Defs.WeaponDef>();
    private final Map<String, Defs.ShipDef> ships = new LinkedHashMap<String, Defs.ShipDef>();
    private final Map<Enums.CrewRole, Defs.RoleDef> roles = new LinkedHashMap<Enums.CrewRole, Defs.RoleDef>();
    private final List<Defs.TechDef> tech = new ArrayList<Defs.TechDef>();

    public static Content load() {
        Content c = new Content();
        c.loadWeapons();
        c.loadShips();
        c.loadRoles();
        c.loadTech();
        return c;
    }

    // ---------------------------------------------------------------- accessors
    public Defs.WeaponDef weapon(String id) { return weapons.get(id); }
    public Defs.ShipDef ship(String id) { return ships.get(id); }
    public Defs.RoleDef role(Enums.CrewRole r) { return roles.get(r); }
    public List<Defs.TechDef> techDefs() { return tech; }
    public java.util.Collection<Defs.WeaponDef> allWeapons() { return weapons.values(); }
    public java.util.Collection<Defs.ShipDef> allShips() { return ships.values(); }

    // ---------------------------------------------------------------- loaders
    private void loadWeapons() {
        List<Object> arr = Json.asArray(Json.parse(read("weapons.json")));
        for (Object o : arr) {
            Map<String, Object> m = Json.asObject(o);
            Defs.WeaponDef w = new Defs.WeaponDef(
                    Json.str(m, "id", ""), Json.str(m, "name", "Weapon"),
                    weaponType(Json.str(m, "type", "LASER")),
                    Json.intVal(m, "damage", 1), Json.intVal(m, "chargeTicks", 10),
                    Json.intVal(m, "power", 1), Json.intVal(m, "cost", 0),
                    Json.bool(m, "piercesShields", false));
            weapons.put(w.id, w);
        }
    }

    private void loadShips() {
        List<Object> arr = Json.asArray(Json.parse(read("ships.json")));
        for (Object o : arr) {
            Map<String, Object> m = Json.asObject(o);
            List<Defs.RoomDef> rooms = new ArrayList<Defs.RoomDef>();
            for (Object ro : Json.asArray(m.get("rooms"))) {
                Map<String, Object> rm = Json.asObject(ro);
                rooms.add(new Defs.RoomDef(
                        roomType(Json.str(rm, "type", "CORRIDOR")),
                        Json.intVal(rm, "w", 1), Json.intVal(rm, "h", 1),
                        Json.intVal(rm, "maxPower", 0), Json.intVal(rm, "maxHp", 3)));
            }
            List<String> weaponIds = new ArrayList<String>();
            Object wl = m.get("weaponIds");
            if (wl != null) for (Object w : Json.asArray(wl)) weaponIds.add(String.valueOf(w));
            Defs.ShipDef s = new Defs.ShipDef(
                    Json.str(m, "id", ""), Json.str(m, "name", "Ship"),
                    faction(Json.str(m, "faction", "NEUTRAL")),
                    Json.intVal(m, "gridW", 8), Json.intVal(m, "gridH", 6),
                    Json.intVal(m, "maxHull", 30), Json.intVal(m, "reactor", 8),
                    Json.intVal(m, "cost", 0), rooms, weaponIds);
            ships.put(s.id, s);
        }
    }

    private void loadRoles() {
        List<Object> arr = Json.asArray(Json.parse(read("roles.json")));
        for (Object o : arr) {
            Map<String, Object> m = Json.asObject(o);
            Enums.CrewRole role = crewRole(Json.str(m, "role", "SECURITY"));
            roles.put(role, new Defs.RoleDef(role, Json.str(m, "title", role.name()), Json.intVal(m, "baseHp", 20)));
        }
    }

    private void loadTech() {
        List<Object> arr = Json.asArray(Json.parse(read("tech.json")));
        for (Object o : arr) {
            Map<String, Object> m = Json.asObject(o);
            tech.add(new Defs.TechDef(
                    techType(Json.str(m, "type", "WEAPONS")), Json.str(m, "name", "Tech"),
                    Json.intVal(m, "maxLevel", 3), Json.intVal(m, "baseCost", 200)));
        }
    }

    // ---------------------------------------------------------------- helpers
    private String read(String file) {
        String path = "com/whim/scg/data/" + file;
        InputStream in = Content.class.getClassLoader().getResourceAsStream(path);
        if (in == null) throw new Json.JsonException("missing resource: " + path);
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) >= 0) sb.append(buf, 0, n);
            r.close();
        } catch (Exception e) {
            throw new Json.JsonException("read failed: " + path + " (" + e.getMessage() + ")");
        }
        return sb.toString();
    }

    private static Enums.WeaponType weaponType(String s) {
        try { return Enums.WeaponType.valueOf(s); } catch (Exception e) { return Enums.WeaponType.LASER; }
    }
    private static Enums.RoomType roomType(String s) {
        try { return Enums.RoomType.valueOf(s); } catch (Exception e) { return Enums.RoomType.CORRIDOR; }
    }
    private static Enums.Faction faction(String s) {
        try { return Enums.Faction.valueOf(s); } catch (Exception e) { return Enums.Faction.NEUTRAL; }
    }
    private static Enums.CrewRole crewRole(String s) {
        try { return Enums.CrewRole.valueOf(s); } catch (Exception e) { return Enums.CrewRole.SECURITY; }
    }
    private static Enums.TechType techType(String s) {
        try { return Enums.TechType.valueOf(s); } catch (Exception e) { return Enums.TechType.WEAPONS; }
    }
}

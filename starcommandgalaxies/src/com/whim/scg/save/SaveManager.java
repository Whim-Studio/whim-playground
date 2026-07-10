package com.whim.scg.save;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Vec2;
import com.whim.scg.content.Content;
import com.whim.scg.content.Json;
import com.whim.scg.model.BoardingModel;
import com.whim.scg.model.CombatModel;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.GalaxyModel;
import com.whim.scg.model.GameState;
import com.whim.scg.model.ProjectileModel;
import com.whim.scg.model.RoomModel;
import com.whim.scg.model.ShipModel;
import com.whim.scg.model.StarSystemModel;
import com.whim.scg.model.TechModel;
import com.whim.scg.model.WeaponModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON save/load of full game state to {@code saves/} ({@code auto} + named slots).
 * Round-trips ship/crew/galaxy/tech/economy and (best-effort) any in-progress
 * combat or boarding.
 */
public final class SaveManager {
    private SaveManager() {}

    private static final Charset UTF8 = Charset.forName("UTF-8");

    // ---------------------------------------------------------------- public
    public static ActionResult save(GameState st, String slot) {
        if (st == null) return ActionResult.fail("no state");
        String name = safeSlot(slot);
        try {
            File dir = savesDir();
            if (!dir.exists() && !dir.mkdirs()) return ActionResult.fail("cannot create saves dir");
            File f = new File(dir, name + ".json");
            String json = Json.write(toMap(st));
            Writer w = new OutputStreamWriter(new FileOutputStream(f), UTF8);
            try { w.write(json); } finally { w.close(); }
            return ActionResult.ok("saved '" + name + "'");
        } catch (Exception e) {
            return ActionResult.fail("save failed: " + e.getMessage());
        }
    }

    public static GameState load(String slot, Content content) {
        String name = safeSlot(slot);
        File f = new File(savesDir(), name + ".json");
        if (!f.exists()) return null;
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF8));
            try {
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) >= 0) sb.append(buf, 0, n);
            } finally { r.close(); }
            Map<String, Object> m = Json.asObject(Json.parse(sb.toString()));
            return fromMap(m, content);
        } catch (Exception e) {
            return null;
        }
    }

    private static File savesDir() {
        File local = new File("saves");
        File nested = new File("starcommandgalaxies/saves");
        if (local.isDirectory()) return local;
        if (nested.isDirectory()) return nested;
        return local; // will be created
    }

    private static String safeSlot(String slot) {
        if (slot == null || slot.trim().isEmpty()) return "auto";
        return slot.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    // ---------------------------------------------------------------- to map
    private static Map<String, Object> toMap(GameState st) {
        Map<String, Object> m = obj();
        m.put("mode", st.mode.name());
        m.put("credits", st.credits);
        m.put("day", st.day);
        m.put("captainName", st.captainName);
        m.put("nextId", st.nextId);
        m.put("seed", (double) st.seed);
        m.put("bossDefeated", st.bossDefeated);
        m.put("paused", st.paused);
        m.put("log", new ArrayList<Object>(st.log));
        List<Object> techs = list();
        for (TechModel t : st.techTree) techs.add(techMap(t));
        m.put("tech", techs);
        m.put("ship", shipMap(st.playerShip));
        m.put("galaxy", galaxyMap(st.galaxy));
        m.put("combat", combatMap(st.combat));
        m.put("boarding", boardingMap(st.boarding));
        return m;
    }

    private static Map<String, Object> techMap(TechModel t) {
        Map<String, Object> m = obj();
        m.put("type", t.type.name());
        m.put("name", t.name);
        m.put("level", t.level);
        m.put("maxLevel", t.maxLevel);
        m.put("baseCost", t.baseCost);
        return m;
    }

    private static Map<String, Object> shipMap(ShipModel s) {
        if (s == null) return null;
        Map<String, Object> m = obj();
        m.put("name", s.name);
        m.put("faction", s.faction.name());
        m.put("hull", s.hull);
        m.put("maxHull", s.maxHull);
        m.put("shields", s.shields);
        m.put("maxShields", s.maxShields);
        m.put("reactor", s.reactor);
        m.put("oxygen", s.oxygen);
        m.put("gridW", s.gridW);
        m.put("gridH", s.gridH);
        m.put("shieldAccum", s.shieldAccum);
        List<Object> rooms = list();
        for (RoomModel r : s.rooms) rooms.add(roomMap(r));
        m.put("rooms", rooms);
        List<Object> crew = list();
        for (CrewModel c : s.crew) crew.add(crewMap(c));
        m.put("crew", crew);
        List<Object> weps = list();
        for (WeaponModel w : s.weapons) weps.add(weaponMap(w));
        m.put("weapons", weps);
        return m;
    }

    private static Map<String, Object> roomMap(RoomModel r) {
        Map<String, Object> m = obj();
        m.put("id", r.id);
        m.put("type", r.type.name());
        m.put("ox", r.origin != null ? r.origin.x : 0);
        m.put("oy", r.origin != null ? r.origin.y : 0);
        m.put("w", r.w);
        m.put("h", r.h);
        m.put("power", r.power);
        m.put("maxPower", r.maxPower);
        m.put("hp", r.hp);
        m.put("maxHp", r.maxHp);
        m.put("onFire", r.onFire);
        m.put("breached", r.breached);
        m.put("fireTime", r.fireTime);
        m.put("crewIds", intList(r.crewIds));
        return m;
    }

    private static Map<String, Object> crewMap(CrewModel c) {
        Map<String, Object> m = obj();
        m.put("id", c.id);
        m.put("name", c.name);
        m.put("faction", c.faction.name());
        m.put("role", c.role.name());
        m.put("hp", c.hp);
        m.put("maxHp", c.maxHp);
        m.put("happiness", c.happiness);
        m.put("level", c.level);
        m.put("xp", c.xp);
        List<Object> sk = list();
        for (int v : c.skills) sk.add(v);
        m.put("skills", sk);
        m.put("stationRoomId", c.stationRoomId);
        if (c.boardingPos != null) { m.put("bx", c.boardingPos.x); m.put("by", c.boardingPos.y); }
        m.put("actTimer", c.actTimer);
        return m;
    }

    private static Map<String, Object> weaponMap(WeaponModel w) {
        Map<String, Object> m = obj();
        m.put("slot", w.slot);
        m.put("defId", w.defId);
        m.put("name", w.name);
        m.put("type", w.type.name());
        m.put("damage", w.damage);
        m.put("chargeMax", w.chargeMax);
        m.put("charge", w.charge);
        m.put("powered", w.powered);
        m.put("reqPower", w.reqPower);
        m.put("targetRoomId", w.targetRoomId);
        m.put("piercesShields", w.piercesShields);
        m.put("cost", w.cost);
        return m;
    }

    private static Map<String, Object> galaxyMap(GalaxyModel g) {
        if (g == null) return null;
        Map<String, Object> m = obj();
        m.put("width", g.width);
        m.put("height", g.height);
        m.put("currentSystem", g.currentSystem);
        List<Object> sys = list();
        for (StarSystemModel s : g.systems) {
            Map<String, Object> sm = obj();
            sm.put("id", s.id);
            sm.put("name", s.name);
            sm.put("x", s.x);
            sm.put("y", s.y);
            sm.put("visited", s.visited);
            sm.put("hasStarport", s.hasStarport);
            sm.put("pendingEvent", s.pendingEvent.name());
            sm.put("isBoss", s.isBoss);
            sm.put("scanned", s.scanned);
            sm.put("links", intList(s.links));
            sys.add(sm);
        }
        m.put("systems", sys);
        return m;
    }

    private static Map<String, Object> combatMap(CombatModel cb) {
        if (cb == null) return null;
        Map<String, Object> m = obj();
        m.put("enemy", shipMap(cb.enemyShip));
        m.put("over", cb.over);
        m.put("playerWon", cb.playerWon);
        m.put("salvage", cb.salvage);
        m.put("boarded", cb.boarded);
        List<Object> ps = list();
        for (ProjectileModel p : cb.projectiles) {
            Map<String, Object> pm = obj();
            pm.put("px", p.pos != null ? p.pos.x : 0.0);
            pm.put("py", p.pos != null ? p.pos.y : 0.0);
            pm.put("vx", p.vel != null ? p.vel.x : 0.0);
            pm.put("vy", p.vel != null ? p.vel.y : 0.0);
            pm.put("type", p.type.name());
            pm.put("fromPlayer", p.fromPlayer);
            pm.put("targetRoomId", p.targetRoomId);
            pm.put("damage", p.damage);
            pm.put("piercesShields", p.piercesShields);
            ps.add(pm);
        }
        m.put("projectiles", ps);
        return m;
    }

    private static Map<String, Object> boardingMap(BoardingModel b) {
        if (b == null) return null;
        Map<String, Object> m = obj();
        m.put("gridW", b.gridW);
        m.put("gridH", b.gridH);
        List<Object> tiles = list();
        for (int x = 0; x < b.gridW; x++)
            for (int y = 0; y < b.gridH; y++)
                tiles.add(b.tiles[x][y].name());
        m.put("tiles", tiles);
        m.put("friendlyIds", friendlyIds(b));
        List<Object> hostiles = list();
        for (CrewModel c : b.hostiles) hostiles.add(crewMap(c));
        m.put("hostiles", hostiles);
        m.put("selectedCrewId", b.selectedCrewId);
        m.put("over", b.over);
        m.put("playerWon", b.playerWon);
        m.put("objective", b.objective);
        if (b.objectivePos != null) { m.put("objx", b.objectivePos.x); m.put("objy", b.objectivePos.y); }
        return m;
    }

    private static List<Object> friendlyIds(BoardingModel b) {
        List<Object> ids = list();
        for (CrewModel c : b.friendlies) ids.add(c.id);
        return ids;
    }

    // ---------------------------------------------------------------- from map
    private static GameState fromMap(Map<String, Object> m, Content content) {
        GameState st = new GameState();
        st.mode = mode(Json.str(m, "mode", "MENU"));
        st.credits = Json.intVal(m, "credits", 0);
        st.day = Json.intVal(m, "day", 1);
        st.captainName = Json.str(m, "captainName", "Captain");
        st.nextId = Json.intVal(m, "nextId", 1);
        st.seed = (long) Json.dbl(m, "seed", 1);
        st.bossDefeated = Json.bool(m, "bossDefeated", false);
        st.paused = Json.bool(m, "paused", false);
        Object log = m.get("log");
        if (log != null) for (Object s : Json.asArray(log)) st.logLine(String.valueOf(s));
        st.techTree.clear();
        Object tech = m.get("tech");
        if (tech != null) for (Object o : Json.asArray(tech)) st.techTree.add(techFrom(Json.asObject(o)));
        st.playerShip = shipFrom(objOrNull(m.get("ship")));
        st.galaxy = galaxyFrom(objOrNull(m.get("galaxy")));
        st.combat = combatFrom(objOrNull(m.get("combat")), st);
        st.boarding = boardingFrom(objOrNull(m.get("boarding")), st);
        return st;
    }

    private static TechModel techFrom(Map<String, Object> m) {
        TechModel t = new TechModel();
        t.type = techType(Json.str(m, "type", "WEAPONS"));
        t.name = Json.str(m, "name", "Tech");
        t.level = Json.intVal(m, "level", 0);
        t.maxLevel = Json.intVal(m, "maxLevel", 3);
        t.baseCost = Json.intVal(m, "baseCost", 200);
        return t;
    }

    private static ShipModel shipFrom(Map<String, Object> m) {
        if (m == null) return null;
        ShipModel s = new ShipModel();
        s.name = Json.str(m, "name", "Ship");
        s.faction = faction(Json.str(m, "faction", "NEUTRAL"));
        s.hull = Json.intVal(m, "hull", 30);
        s.maxHull = Json.intVal(m, "maxHull", 30);
        s.shields = Json.intVal(m, "shields", 0);
        s.maxShields = Json.intVal(m, "maxShields", 0);
        s.reactor = Json.intVal(m, "reactor", 8);
        s.oxygen = Json.intVal(m, "oxygen", 100);
        s.gridW = Json.intVal(m, "gridW", 8);
        s.gridH = Json.intVal(m, "gridH", 6);
        s.shieldAccum = Json.dbl(m, "shieldAccum", 0);
        Object rooms = m.get("rooms");
        if (rooms != null) for (Object o : Json.asArray(rooms)) s.rooms.add(roomFrom(Json.asObject(o)));
        Object crew = m.get("crew");
        if (crew != null) for (Object o : Json.asArray(crew)) s.crew.add(crewFrom(Json.asObject(o)));
        Object weps = m.get("weapons");
        if (weps != null) for (Object o : Json.asArray(weps)) s.weapons.add(weaponFrom(Json.asObject(o)));
        return s;
    }

    private static RoomModel roomFrom(Map<String, Object> m) {
        RoomModel r = new RoomModel();
        r.id = Json.intVal(m, "id", 0);
        r.type = roomType(Json.str(m, "type", "CORRIDOR"));
        r.origin = new GridPos(Json.intVal(m, "ox", 0), Json.intVal(m, "oy", 0));
        r.w = Json.intVal(m, "w", 1);
        r.h = Json.intVal(m, "h", 1);
        r.power = Json.intVal(m, "power", 0);
        r.maxPower = Json.intVal(m, "maxPower", 0);
        r.hp = Json.intVal(m, "hp", 3);
        r.maxHp = Json.intVal(m, "maxHp", 3);
        r.onFire = Json.bool(m, "onFire", false);
        r.breached = Json.bool(m, "breached", false);
        r.fireTime = Json.dbl(m, "fireTime", 0);
        Object cids = m.get("crewIds");
        if (cids != null) for (Object o : Json.asArray(cids)) r.crewIds.add((int) num(o));
        return r;
    }

    private static CrewModel crewFrom(Map<String, Object> m) {
        CrewModel c = new CrewModel();
        c.id = Json.intVal(m, "id", 0);
        c.name = Json.str(m, "name", "Crew");
        c.faction = faction(Json.str(m, "faction", "FEDERATION"));
        c.role = crewRole(Json.str(m, "role", "SECURITY"));
        c.hp = Json.intVal(m, "hp", 20);
        c.maxHp = Json.intVal(m, "maxHp", 20);
        c.happiness = Json.intVal(m, "happiness", 70);
        c.level = Json.intVal(m, "level", 1);
        c.xp = Json.intVal(m, "xp", 0);
        Object sk = m.get("skills");
        if (sk != null) {
            List<Object> arr = Json.asArray(sk);
            for (int i = 0; i < arr.size() && i < c.skills.length; i++) c.skills[i] = (int) num(arr.get(i));
        }
        c.stationRoomId = Json.intVal(m, "stationRoomId", -1);
        if (m.containsKey("bx")) c.boardingPos = new GridPos(Json.intVal(m, "bx", 0), Json.intVal(m, "by", 0));
        c.actTimer = Json.dbl(m, "actTimer", 0);
        return c;
    }

    private static WeaponModel weaponFrom(Map<String, Object> m) {
        WeaponModel w = new WeaponModel();
        w.slot = Json.intVal(m, "slot", 0);
        w.defId = Json.str(m, "defId", "");
        w.name = Json.str(m, "name", "Weapon");
        w.type = weaponType(Json.str(m, "type", "LASER"));
        w.damage = Json.intVal(m, "damage", 1);
        w.chargeMax = Json.intVal(m, "chargeMax", 10);
        w.charge = Json.dbl(m, "charge", 0);
        w.powered = Json.intVal(m, "powered", 0);
        w.reqPower = Json.intVal(m, "reqPower", 1);
        w.targetRoomId = Json.intVal(m, "targetRoomId", -1);
        w.piercesShields = Json.bool(m, "piercesShields", false);
        w.cost = Json.intVal(m, "cost", 0);
        return w;
    }

    private static GalaxyModel galaxyFrom(Map<String, Object> m) {
        if (m == null) return null;
        GalaxyModel g = new GalaxyModel();
        g.width = Json.intVal(m, "width", 1000);
        g.height = Json.intVal(m, "height", 640);
        g.currentSystem = Json.intVal(m, "currentSystem", 0);
        Object sys = m.get("systems");
        if (sys != null) for (Object o : Json.asArray(sys)) {
            Map<String, Object> sm = Json.asObject(o);
            StarSystemModel s = new StarSystemModel();
            s.id = Json.intVal(sm, "id", 0);
            s.name = Json.str(sm, "name", "System");
            s.x = Json.intVal(sm, "x", 0);
            s.y = Json.intVal(sm, "y", 0);
            s.visited = Json.bool(sm, "visited", false);
            s.hasStarport = Json.bool(sm, "hasStarport", false);
            s.pendingEvent = eventType(Json.str(sm, "pendingEvent", "NOTHING"));
            s.isBoss = Json.bool(sm, "isBoss", false);
            s.scanned = Json.bool(sm, "scanned", false);
            Object links = sm.get("links");
            if (links != null) for (Object l : Json.asArray(links)) s.links.add((int) num(l));
            g.systems.add(s);
        }
        return g;
    }

    private static CombatModel combatFrom(Map<String, Object> m, GameState st) {
        if (m == null) return null;
        CombatModel cb = new CombatModel();
        cb.playerShip = st.playerShip;
        cb.enemyShip = shipFrom(objOrNull(m.get("enemy")));
        cb.over = Json.bool(m, "over", false);
        cb.playerWon = Json.bool(m, "playerWon", false);
        cb.salvage = Json.intVal(m, "salvage", 0);
        cb.boarded = Json.bool(m, "boarded", false);
        Object ps = m.get("projectiles");
        if (ps != null) for (Object o : Json.asArray(ps)) {
            Map<String, Object> pm = Json.asObject(o);
            ProjectileModel p = new ProjectileModel();
            p.pos = new Vec2(Json.dbl(pm, "px", 0), Json.dbl(pm, "py", 0));
            p.vel = new Vec2(Json.dbl(pm, "vx", 0), Json.dbl(pm, "vy", 0));
            p.type = weaponType(Json.str(pm, "type", "LASER"));
            p.fromPlayer = Json.bool(pm, "fromPlayer", true);
            p.targetRoomId = Json.intVal(pm, "targetRoomId", -1);
            p.damage = Json.intVal(pm, "damage", 1);
            p.piercesShields = Json.bool(pm, "piercesShields", false);
            cb.projectiles.add(p);
        }
        return cb;
    }

    private static BoardingModel boardingFrom(Map<String, Object> m, GameState st) {
        if (m == null) return null;
        BoardingModel b = new BoardingModel();
        b.gridW = Json.intVal(m, "gridW", 6);
        b.gridH = Json.intVal(m, "gridH", 6);
        b.tiles = new Enums.TileType[b.gridW][b.gridH];
        Object tiles = m.get("tiles");
        List<Object> tl = tiles != null ? Json.asArray(tiles) : new ArrayList<Object>();
        int idx = 0;
        for (int x = 0; x < b.gridW; x++)
            for (int y = 0; y < b.gridH; y++)
                b.tiles[x][y] = idx < tl.size() ? tileType(String.valueOf(tl.get(idx++))) : Enums.TileType.FLOOR;
        // hostiles are self-contained
        Object hostiles = m.get("hostiles");
        if (hostiles != null) for (Object o : Json.asArray(hostiles)) b.hostiles.add(crewFrom(Json.asObject(o)));
        b.enemyShip = st.combat != null ? st.combat.enemyShip : null;
        // friendlies reconnect to the player ship crew by id
        Object fids = m.get("friendlyIds");
        if (fids != null && st.playerShip != null) {
            for (Object o : Json.asArray(fids)) {
                CrewModel c = st.playerShip.crewById((int) num(o));
                if (c != null) b.friendlies.add(c);
            }
        }
        b.selectedCrewId = Json.intVal(m, "selectedCrewId", -1);
        b.over = Json.bool(m, "over", false);
        b.playerWon = Json.bool(m, "playerWon", false);
        b.objective = Json.str(m, "objective", "Eliminate all hostiles");
        if (m.containsKey("objx")) b.objectivePos = new GridPos(Json.intVal(m, "objx", 0), Json.intVal(m, "objy", 0));
        return b;
    }

    // ---------------------------------------------------------------- helpers
    private static Map<String, Object> obj() { return new LinkedHashMap<String, Object>(); }
    private static List<Object> list() { return new ArrayList<Object>(); }

    private static List<Object> intList(List<Integer> in) {
        List<Object> out = list();
        for (Integer i : in) out.add(i);
        return out;
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private static Map<String, Object> objOrNull(Object o) {
        return o instanceof Map ? Json.asObject(o) : null;
    }

    private static Enums.Mode mode(String s) {
        try { return Enums.Mode.valueOf(s); } catch (Exception e) { return Enums.Mode.MENU; }
    }
    private static Enums.Faction faction(String s) {
        try { return Enums.Faction.valueOf(s); } catch (Exception e) { return Enums.Faction.NEUTRAL; }
    }
    private static Enums.CrewRole crewRole(String s) {
        try { return Enums.CrewRole.valueOf(s); } catch (Exception e) { return Enums.CrewRole.SECURITY; }
    }
    private static Enums.RoomType roomType(String s) {
        try { return Enums.RoomType.valueOf(s); } catch (Exception e) { return Enums.RoomType.CORRIDOR; }
    }
    private static Enums.WeaponType weaponType(String s) {
        try { return Enums.WeaponType.valueOf(s); } catch (Exception e) { return Enums.WeaponType.LASER; }
    }
    private static Enums.TechType techType(String s) {
        try { return Enums.TechType.valueOf(s); } catch (Exception e) { return Enums.TechType.WEAPONS; }
    }
    private static Enums.EventType eventType(String s) {
        try { return Enums.EventType.valueOf(s); } catch (Exception e) { return Enums.EventType.NOTHING; }
    }
    private static Enums.TileType tileType(String s) {
        try { return Enums.TileType.valueOf(s); } catch (Exception e) { return Enums.TileType.FLOOR; }
    }
}

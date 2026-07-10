package com.whim.scg.engine;

import com.whim.scg.api.Defs;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.content.Content;
import com.whim.scg.model.BoardingModel;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.GalaxyModel;
import com.whim.scg.model.GameState;
import com.whim.scg.model.RoomModel;
import com.whim.scg.model.ShipModel;
import com.whim.scg.model.StarSystemModel;
import com.whim.scg.model.TechModel;
import com.whim.scg.model.WeaponModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds fresh game state: player ship, crew, galaxy, enemy ships, boarding grids. */
final class Factory {

    private static final String[] STAR_NAMES = {
            "Aldebar", "Cygnus Reach", "Vela Drift", "Orin's Gate", "Halcyon",
            "Tanis", "Zephyr Belt", "Kestros", "Nyx Hollow", "Perihelion",
            "Sable Verge", "Ilium", "Corvus Fold", "Meridian", "Ashfall",
            "Tycho Rim", "Wren's Cross", "Solace" };

    private static final String[] CREW_NAMES = {
            "Vance", "Rell", "Okada", "Bright", "Sol", "Kade", "Nera", "Poll",
            "Ash", "Torv", "Lyle", "Marn", "Quill", "Dax", "Reyes", "Iko" };

    private final Content content;
    private final Random rng;

    Factory(Content content, Random rng) {
        this.content = content;
        this.rng = rng;
    }

    // ---------------------------------------------------------------- tech tree
    void initTech(GameState st) {
        st.techTree.clear();
        for (Defs.TechDef d : content.techDefs()) {
            st.techTree.add(new TechModel(d.type, d.name, d.maxLevel, d.baseCost));
        }
    }

    // ---------------------------------------------------------------- ships
    ShipModel buildShip(GameState st, Defs.ShipDef def, String name) {
        ShipModel ship = new ShipModel();
        ship.name = name != null && !name.isEmpty() ? name : def.name;
        ship.faction = def.faction;
        ship.maxHull = def.maxHull;
        ship.hull = def.maxHull;
        ship.reactor = def.reactor;
        ship.gridW = def.gridW;
        ship.gridH = def.gridH;
        ship.oxygen = 100;
        placeRooms(st, ship, def.rooms);
        int slot = 0;
        for (String wid : def.weaponIds) {
            Defs.WeaponDef wd = content.weapon(wid);
            if (wd == null) continue;
            ship.weapons.add(makeWeapon(slot++, wd));
        }
        // shields derive from a SHIELDS room presence
        RoomModel shieldRoom = ship.firstRoom(Enums.RoomType.SHIELDS);
        ship.maxShields = shieldRoom != null ? 4 : 0;
        ship.shields = ship.maxShields;
        return ship;
    }

    static WeaponModel makeWeapon(int slot, Defs.WeaponDef wd) {
        WeaponModel w = new WeaponModel();
        w.slot = slot;
        w.defId = wd.id;
        w.name = wd.name;
        w.type = wd.type;
        w.damage = wd.damage;
        w.chargeMax = wd.chargeTicks;
        w.charge = 0;
        w.powered = 0;
        w.reqPower = Math.max(1, wd.power);
        w.cost = wd.cost;
        w.piercesShields = wd.piercesShields;
        return w;
    }

    /** Row-major packing of rooms into the grid. */
    private void placeRooms(GameState st, ShipModel ship, List<Defs.RoomDef> defs) {
        boolean[][] occ = new boolean[ship.gridW][ship.gridH];
        for (Defs.RoomDef d : defs) {
            GridPos p = firstFit(occ, ship.gridW, ship.gridH, d.w, d.h);
            if (p == null) p = new GridPos(0, 0); // fallback (shouldn't happen with seed data)
            RoomModel r = new RoomModel(st.newId(), d.type, p, d.w, d.h, d.maxPower, d.maxHp);
            for (int x = p.x; x < p.x + d.w && x < ship.gridW; x++)
                for (int y = p.y; y < p.y + d.h && y < ship.gridH; y++) occ[x][y] = true;
            ship.rooms.add(r);
        }
    }

    private static GridPos firstFit(boolean[][] occ, int gw, int gh, int w, int h) {
        for (int y = 0; y + h <= gh; y++) {
            for (int x = 0; x + w <= gw; x++) {
                if (free(occ, x, y, w, h)) return new GridPos(x, y);
            }
        }
        return null;
    }

    private static boolean free(boolean[][] occ, int x, int y, int w, int h) {
        for (int i = x; i < x + w; i++)
            for (int j = y; j < y + h; j++)
                if (occ[i][j]) return false;
        return true;
    }

    // ---------------------------------------------------------------- crew
    void addStartingCrew(GameState st, ShipModel ship, String captainName) {
        Enums.CrewRole[] roles = {
                Enums.CrewRole.CAPTAIN, Enums.CrewRole.PILOT,
                Enums.CrewRole.ENGINEER, Enums.CrewRole.GUNNER };
        for (int i = 0; i < roles.length; i++) {
            String nm = i == 0 && captainName != null && !captainName.isEmpty()
                    ? captainName : CREW_NAMES[rng.nextInt(CREW_NAMES.length)];
            CrewModel c = makeCrew(st, roles[i], Enums.Faction.FEDERATION, nm, 30 + rng.nextInt(20));
            ship.crew.add(c);
        }
        autoAssign(ship);
    }

    CrewModel makeCrew(GameState st, Enums.CrewRole role, Enums.Faction faction, String name, int skillBase) {
        Defs.RoleDef rd = content.role(role);
        int hp = rd != null ? rd.baseHp : 20;
        CrewModel c = new CrewModel();
        c.id = st.newId();
        c.name = name;
        c.faction = faction;
        c.role = role;
        c.maxHp = hp;
        c.hp = hp;
        c.happiness = 70 + rng.nextInt(20);
        // spread a modest baseline over all stats, higher on primary
        for (Enums.StatType s : Enums.StatType.values()) c.setSkill(s, 10 + rng.nextInt(15));
        c.setSkill(role.primary(), skillBase + rng.nextInt(15));
        return c;
    }

    /** Assign each powered role-matching crew to its natural station. */
    void autoAssign(ShipModel ship) {
        for (CrewModel c : ship.crew) {
            RoomModel target = stationFor(ship, c.role);
            if (target != null) {
                c.stationRoomId = target.id;
                if (!target.crewIds.contains(c.id)) target.crewIds.add(c.id);
            }
        }
    }

    private RoomModel stationFor(ShipModel ship, Enums.CrewRole role) {
        Enums.RoomType want;
        switch (role) {
            case PILOT: want = Enums.RoomType.ENGINES; break;
            case ENGINEER: want = Enums.RoomType.ENGINES; break;
            case GUNNER: want = Enums.RoomType.WEAPONS; break;
            case SHIELD_TECH: want = Enums.RoomType.SHIELDS; break;
            case MEDIC: want = Enums.RoomType.MEDBAY; break;
            case SCIENCE: want = Enums.RoomType.SENSORS; break;
            case CAPTAIN: want = Enums.RoomType.BRIDGE; break;
            default: want = Enums.RoomType.WEAPONS; break;
        }
        RoomModel r = ship.firstRoom(want);
        if (r == null) r = ship.firstRoom(Enums.RoomType.BRIDGE);
        return r;
    }

    // ---------------------------------------------------------------- galaxy
    GalaxyModel buildGalaxy(GameState st) {
        GalaxyModel g = new GalaxyModel();
        g.width = 1000;
        g.height = 640;
        int n = 10 + rng.nextInt(5); // 10..14
        List<StarSystemModel> sys = g.systems;
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < STAR_NAMES.length; i++) names.add(STAR_NAMES[i]);
        for (int i = 0; i < n; i++) {
            StarSystemModel s = new StarSystemModel();
            s.id = i;
            s.name = names.isEmpty() ? ("System " + i) : names.remove(rng.nextInt(names.size()));
            s.x = 60 + rng.nextInt(g.width - 120);
            s.y = 50 + rng.nextInt(g.height - 100);
            sys.add(s);
        }
        // spanning tree: connect each node to a previously-added nearest-ish node
        for (int i = 1; i < n; i++) {
            int j = pickNearPrevious(sys, i);
            link(sys.get(i), sys.get(j));
        }
        // a few extra edges for loops
        int extra = n / 3;
        for (int e = 0; e < extra; e++) {
            int a = rng.nextInt(n), b = rng.nextInt(n);
            if (a != b && !sys.get(a).links.contains(b)) link(sys.get(a), sys.get(b));
        }
        // starting system: index 0, no event, visited
        StarSystemModel start = sys.get(0);
        start.visited = true;
        start.scanned = true;
        start.hasStarport = true;
        start.pendingEvent = Enums.EventType.NOTHING;
        g.currentSystem = 0;
        // boss = farthest system from start (by graph coords)
        int bossIdx = farthest(sys, start);
        StarSystemModel boss = sys.get(bossIdx);
        boss.isBoss = true;
        boss.pendingEvent = Enums.EventType.STORY;
        boss.name = "The Maw";
        // seed events + starports on the rest
        for (int i = 1; i < n; i++) {
            StarSystemModel s = sys.get(i);
            if (s.isBoss) continue;
            if (rng.nextInt(100) < 30) s.hasStarport = true;
            s.pendingEvent = randomEvent(s.hasStarport);
        }
        return g;
    }

    private int pickNearPrevious(List<StarSystemModel> sys, int i) {
        StarSystemModel s = sys.get(i);
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int j = 0; j < i; j++) {
            double d = dist2(s, sys.get(j));
            if (d < bd) { bd = d; best = j; }
        }
        return best;
    }

    private int farthest(List<StarSystemModel> sys, StarSystemModel from) {
        int best = 0;
        double bd = -1;
        for (int j = 0; j < sys.size(); j++) {
            double d = dist2(from, sys.get(j));
            if (d > bd) { bd = d; best = j; }
        }
        return best;
    }

    private static double dist2(StarSystemModel a, StarSystemModel b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private static void link(StarSystemModel a, StarSystemModel b) {
        if (!a.links.contains(b.id)) a.links.add(b.id);
        if (!b.links.contains(a.id)) b.links.add(a.id);
    }

    private Enums.EventType randomEvent(boolean hasStarport) {
        if (hasStarport && rng.nextInt(100) < 40) return Enums.EventType.MERCHANT;
        int roll = rng.nextInt(100);
        if (roll < 22) return Enums.EventType.PIRATE_AMBUSH;
        if (roll < 40) return Enums.EventType.DERELICT;
        if (roll < 58) return Enums.EventType.DISTRESS;
        if (roll < 74) return Enums.EventType.HAZARD;
        if (roll < 86) return Enums.EventType.MERCHANT;
        return Enums.EventType.NOTHING;
    }

    // ---------------------------------------------------------------- enemies
    ShipModel buildEnemyShip(GameState st, boolean boss) {
        Defs.ShipDef def = content.ship(boss ? "cruiser" : "raider");
        if (def == null) def = content.ship("raider");
        ShipModel e = buildShip(st, def, boss ? "The Maw" : "Corsair");
        e.faction = boss ? Enums.Faction.ALIEN_SWARM : Enums.Faction.PIRATE;
        // difficulty scaling by galaxy progress
        int visited = countVisited(st);
        int tier = Math.min(4, visited / 2);
        e.maxHull += tier * 4 + (boss ? 20 : 0);
        e.hull = e.maxHull;
        e.maxShields += (boss ? 4 : 0) + tier;
        e.shields = e.maxShields;
        // power the enemy's systems + weapons so it fights back
        for (RoomModel r : e.rooms) {
            if (r.type.isPowered()) r.power = Math.min(r.maxPower, r.maxPower > 0 ? 2 : 0);
        }
        int pw = e.reactor;
        for (WeaponModel w : e.weapons) {
            if (pw <= 0) break;
            w.powered = 1; pw--;
        }
        // enemy crew for boarding defence
        Enums.CrewRole[] roles = boss
                ? new Enums.CrewRole[]{Enums.CrewRole.SECURITY, Enums.CrewRole.SECURITY, Enums.CrewRole.GUNNER, Enums.CrewRole.CAPTAIN}
                : new Enums.CrewRole[]{Enums.CrewRole.SECURITY, Enums.CrewRole.GUNNER};
        for (int i = 0; i < roles.length; i++) {
            CrewModel c = makeCrew(st, roles[i], e.faction, "Hostile", 25 + tier * 5 + (boss ? 20 : 0));
            e.crew.add(c);
        }
        autoAssign(e);
        return e;
    }

    private int countVisited(GameState st) {
        int c = 0;
        if (st.galaxy != null) for (StarSystemModel s : st.galaxy.systems) if (s.visited) c++;
        return c;
    }

    // ---------------------------------------------------------------- boarding
    /** Build a boarding grid from the enemy ship; party enters at the teleporter row. */
    BoardingModel buildBoarding(GameState st, ShipModel enemy, List<CrewModel> party) {
        BoardingModel b = new BoardingModel();
        int gw = Math.max(6, enemy.gridW);
        int gh = Math.max(6, enemy.gridH + 2);
        b.gridW = gw;
        b.gridH = gh;
        b.enemyShip = enemy;
        b.tiles = new Enums.TileType[gw][gh];
        for (int x = 0; x < gw; x++) {
            for (int y = 0; y < gh; y++) {
                boolean border = x == 0 || y == 0 || x == gw - 1 || y == gh - 1;
                b.tiles[x][y] = border ? Enums.TileType.WALL : Enums.TileType.FLOOR;
            }
        }
        // mark enemy systems as SYSTEM tiles for flavour + objective
        GridPos bridgePos = null;
        for (RoomModel r : enemy.rooms) {
            if (r.origin == null) continue;
            int cx = Math.min(gw - 2, Math.max(1, r.origin.x + r.w / 2));
            int cy = Math.min(gh - 3, Math.max(1, r.origin.y + r.h / 2)) + 1;
            if (r.type.isPowered() && b.inBounds(new GridPos(cx, cy)))
                b.tiles[cx][cy] = Enums.TileType.SYSTEM;
            if (r.type == Enums.RoomType.BRIDGE) bridgePos = new GridPos(cx, cy);
        }
        if (bridgePos == null) bridgePos = new GridPos(gw / 2, 1);
        b.objectivePos = bridgePos;
        b.tiles[bridgePos.x][bridgePos.y] = Enums.TileType.SYSTEM;
        b.objective = "Clear hostiles or seize the bridge";

        // place party at the entry (teleporter) near the bottom
        int ex = Math.max(1, gw / 2 - party.size() / 2);
        int ey = gh - 2;
        for (int i = 0; i < party.size(); i++) {
            CrewModel c = party.get(i);
            int px = Math.min(gw - 2, ex + i);
            c.boardingPos = new GridPos(px, ey);
            b.friendlies.add(c);
        }
        if (!b.friendlies.isEmpty()) b.selectedCrewId = b.friendlies.get(0).id;

        // hostiles spread across the upper interior
        int hi = 0;
        for (CrewModel c : enemy.crew) {
            if (!c.alive()) continue;
            int hx = 1 + (hi * 2 + 1) % (gw - 2);
            int hy = 1 + (hi % Math.max(1, gh / 2));
            GridPos pos = new GridPos(hx, hy);
            if (!b.walkable(pos) || b.occupant(pos) != null) pos = new GridPos(1 + hi % (gw - 2), 1);
            c.boardingPos = pos;
            b.hostiles.add(c);
            hi++;
        }
        return b;
    }
}

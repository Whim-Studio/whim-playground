package com.whim.starcraft8.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.GameMap;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Projectile;
import com.whim.starcraft8.domain.Terrain;
import com.whim.starcraft8.domain.Unit;
import com.whim.starcraft8.domain.UnitType;
import com.whim.starcraft8.engine.WorldReader;

/**
 * Immutable-per-frame snapshot of everything the UI needs to paint. Built ONLY inside
 * {@code Simulation.readState(...)} (under the engine lock) by copying primitives and
 * type references out of the live domain objects. After {@link #snapshot} returns, the
 * UI never touches domain state again until the next frame — it paints from this on the EDT.
 */
final class RenderState {

    static final class RUnit {
        long id;
        UnitType type;
        int ownerId;
        double x, y;
        int hp, maxHp, shield, maxShield;
        boolean flyer;
    }

    static final class RBuilding {
        long id;
        BuildingType type;
        int ownerId;
        int tileX, tileY, w, h;
        int hp, maxHp;
        boolean producing;
        boolean underConstruction;
        double rallyX, rallyY;
        boolean hasRally;
    }

    static final class RProjectile {
        double x, y;
        Color color;
    }

    int mapW, mapH;
    Terrain[][] terrain;       // [tx][ty]
    int[][] resource;          // remaining amount for mineral/geyser tiles, else 0
    long tick;
    int winnerId = -1;

    int humanId;
    // human player economy
    int minerals, gas, supplyUsed, supplyCap;
    boolean humanFound;
    com.whim.starcraft8.domain.Race humanRace;

    final List<RUnit> units = new ArrayList<RUnit>();
    final List<RBuilding> buildings = new ArrayList<RBuilding>();
    final List<RProjectile> projectiles = new ArrayList<RProjectile>();

    /** Build a fresh snapshot from the live world. Must be called inside readState. */
    static RenderState snapshot(WorldReader reader, int humanId) {
        RenderState rs = new RenderState();
        rs.humanId = humanId;
        GameState gs = reader.state();
        GameMap map = gs.map();
        int w = map.width();
        int h = map.height();
        rs.mapW = w;
        rs.mapH = h;
        rs.tick = gs.tick();
        rs.winnerId = gs.winnerId();

        rs.terrain = new Terrain[w][h];
        rs.resource = new int[w][h];
        for (int tx = 0; tx < w; tx++) {
            for (int ty = 0; ty < h; ty++) {
                Terrain t = map.terrainAt(tx, ty);
                rs.terrain[tx][ty] = t;
                if (t == Terrain.MINERAL_FIELD || t == Terrain.GEYSER) {
                    rs.resource[tx][ty] = map.resourceAt(tx, ty);
                }
            }
        }

        Player human = gs.player(humanId);
        if (human != null) {
            rs.humanFound = true;
            rs.humanRace = human.race();
            rs.minerals = human.minerals();
            rs.gas = human.gas();
            rs.supplyUsed = human.supplyUsed();
            rs.supplyCap = human.supplyCap();
        }

        List<Unit> liveUnits = gs.units();
        for (int i = 0; i < liveUnits.size(); i++) {
            Unit u = liveUnits.get(i);
            if (!u.alive()) continue;
            RUnit r = new RUnit();
            r.id = u.id();
            r.type = u.type();
            r.ownerId = u.ownerId();
            r.x = u.x();
            r.y = u.y();
            r.hp = u.hp();
            r.maxHp = u.type().maxHp();
            r.shield = u.shield();
            r.maxShield = u.type().maxShield();
            r.flyer = u.type().isFlyer();
            rs.units.add(r);
        }

        List<Building> liveBuildings = gs.buildings();
        for (int i = 0; i < liveBuildings.size(); i++) {
            Building b = liveBuildings.get(i);
            if (!b.alive()) continue;
            RBuilding r = new RBuilding();
            r.id = b.id();
            r.type = b.type();
            r.ownerId = b.ownerId();
            r.tileX = b.tileX();
            r.tileY = b.tileY();
            r.w = b.type().widthTiles();
            r.h = b.type().heightTiles();
            r.hp = b.hp();
            r.maxHp = b.type().maxHp();
            r.underConstruction = b.state() == com.whim.starcraft8.domain.BuildState.UNDER_CONSTRUCTION;
            r.producing = b.state() == com.whim.starcraft8.domain.BuildState.PRODUCING
                    || !b.productionQueue().isEmpty();
            double rx = b.rallyX();
            double ry = b.rallyY();
            r.rallyX = rx;
            r.rallyY = ry;
            r.hasRally = rx > 0 || ry > 0;
            rs.buildings.add(r);
        }

        List<Projectile> liveProj = gs.projectiles();
        for (int i = 0; i < liveProj.size(); i++) {
            Projectile p = liveProj.get(i);
            if (p.done()) continue;
            RProjectile r = new RProjectile();
            r.x = p.x();
            r.y = p.y();
            r.color = p.color();
            rs.projectiles.add(r);
        }

        return rs;
    }
}

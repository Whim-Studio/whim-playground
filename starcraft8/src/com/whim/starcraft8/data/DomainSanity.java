package com.whim.starcraft8.data;

import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.Unit;

/** Standalone smoke test for the domain + data slice. */
public final class DomainSanity {
    private DomainSanity() {}

    public static void main(String[] args) {
        GameState gs = MapFactory.newSkirmish(Race.TERRAN, Race.ZERG);

        System.out.println("=== 8-Bit StarCraft domain sanity ===");
        System.out.println("Map: " + gs.map().width() + "x" + gs.map().height());
        System.out.println("Players: " + gs.players().size());
        for (int i = 0; i < gs.players().size(); i++) {
            Player p = gs.players().get(i);
            System.out.println("  Player " + p.id() + " race=" + p.race()
                + " ai=" + p.isAi()
                + " minerals=" + p.minerals() + " gas=" + p.gas()
                + " supply=" + p.supplyUsed() + "/" + p.supplyCap());
        }

        System.out.println("Total units: " + gs.units().size());
        for (int i = 0; i < gs.units().size(); i++) {
            Unit u = gs.units().get(i);
            System.out.println("  Unit#" + u.id() + " " + u.type().displayName()
                + " owner=" + u.ownerId()
                + " hp=" + u.hp() + " @(" + fmt(u.x()) + "," + fmt(u.y()) + ")");
        }

        System.out.println("Total buildings: " + gs.buildings().size());
        for (int i = 0; i < gs.buildings().size(); i++) {
            Building b = gs.buildings().get(i);
            System.out.println("  Building#" + b.id() + " " + b.type().displayName()
                + " owner=" + b.ownerId()
                + " hp=" + b.hp() + " state=" + b.state()
                + " @(" + b.tileX() + "," + b.tileY() + ")");
        }

        // Count resource tiles.
        int mineralTiles = 0;
        int geyserTiles = 0;
        for (int ty = 0; ty < gs.map().height(); ty++) {
            for (int tx = 0; tx < gs.map().width(); tx++) {
                switch (gs.map().terrainAt(tx, ty)) {
                    case MINERAL_FIELD: mineralTiles++; break;
                    case GEYSER: geyserTiles++; break;
                    default: break;
                }
            }
        }
        System.out.println("Mineral fields: " + mineralTiles + ", geysers: " + geyserTiles);

        System.out.println("UnitType constants: " + com.whim.starcraft8.domain.UnitType.values().length);
        System.out.println("BuildingType constants: " + com.whim.starcraft8.domain.BuildingType.values().length);
        System.out.println("OK");
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}

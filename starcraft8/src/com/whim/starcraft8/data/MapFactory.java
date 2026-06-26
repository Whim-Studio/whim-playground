package com.whim.starcraft8.data;

import com.whim.starcraft8.domain.Building;
import com.whim.starcraft8.domain.BuildState;
import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.GameMap;
import com.whim.starcraft8.domain.GameState;
import com.whim.starcraft8.domain.Player;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.Terrain;
import com.whim.starcraft8.domain.Unit;
import com.whim.starcraft8.domain.UnitState;
import com.whim.starcraft8.domain.UnitType;

import java.util.ArrayList;
import java.util.List;

/** Builds a ready-to-play 48x48 two-player skirmish. */
public final class MapFactory {
    private MapFactory() {}

    /** Human is player 0, AI is player 1. */
    public static GameState newSkirmish(Race human, Race ai) {
        GameMap map = new GameMap(Balance.MAP_WIDTH, Balance.MAP_HEIGHT);

        List<Player> players = new ArrayList<Player>();
        Player p0 = new Player(0, human, false);
        Player p1 = new Player(1, ai, true);
        players.add(p0);
        players.add(p1);

        GameState gs = new GameState(map, players);

        // Two diagonal start locations (town-hall top-left tile).
        setupBase(gs, p0, human, 5, 5);
        setupBase(gs, p1, ai, Balance.MAP_WIDTH - 9, Balance.MAP_HEIGHT - 8);

        return gs;
    }

    private static void setupBase(GameState gs, Player player, Race race, int hallX, int hallY) {
        GameMap map = gs.map();

        // Starting resources.
        player.addMinerals(Balance.START_MINERALS);
        player.addGas(Balance.START_GAS);
        player.setSupplyCap(Balance.START_SUPPLY_CAP);

        // Town hall (completed).
        BuildingType hallType = TechTree.townHall(race);
        Building hall = new Building(hallType, player.id(), hallX, hallY);
        hall.setState(BuildState.COMPLETE);
        hall.setBuildProgress(hallType.buildTicks());
        gs.buildings().add(hall);

        double hallCx = hallX + hallType.widthTiles() / 2.0;
        double hallCy = hallY + hallType.heightTiles() / 2.0;

        // Mineral line: an arc of patches a few tiles from the hall.
        int placed = 0;
        int[][] mineralOffsets = {
            {-2, -2}, {0, -2}, {2, -2}, {4, -1}, {-2, 0}, {5, 1}
        };
        for (int i = 0; i < mineralOffsets.length && placed < Balance.MINERALS_PER_BASE; i++) {
            int mx = hallX + mineralOffsets[i][0];
            int my = hallY + mineralOffsets[i][1];
            if (map.inBounds(mx, my) && map.terrainAt(mx, my) == Terrain.GROUND) {
                map.setTerrain(mx, my, Terrain.MINERAL_FIELD);
                map.setResourceAt(mx, my, Balance.MINERAL_FIELD_AMOUNT);
                placed++;
            }
        }

        // Geyser near the base.
        int[][] geyserOffsets = { {6, 3}, {-3, 3}, {3, 5} };
        int geysers = 0;
        for (int i = 0; i < geyserOffsets.length && geysers < Balance.GEYSERS_PER_BASE; i++) {
            int gx = hallX + geyserOffsets[i][0];
            int gy = hallY + geyserOffsets[i][1];
            if (map.inBounds(gx, gy) && map.terrainAt(gx, gy) == Terrain.GROUND) {
                map.setTerrain(gx, gy, Terrain.GEYSER);
                map.setResourceAt(gx, gy, Balance.GEYSER_AMOUNT);
                geysers++;
            }
        }

        // Starting workers, fanned out just below the hall.
        UnitType workerType = TechTree.worker(race);
        for (int i = 0; i < Balance.START_WORKERS; i++) {
            double wx = hallCx + (i - Balance.START_WORKERS / 2.0) + 0.5;
            double wy = hallCy + hallType.heightTiles();
            if (wx < 0.5) wx = 0.5;
            if (wy >= map.height() - 0.5) wy = map.height() - 0.5;
            Unit w = new Unit(workerType, player.id(), wx, wy);
            w.setState(UnitState.IDLE);
            gs.units().add(w);
        }

        // Account starting supply usage (workers).
        player.setSupplyUsed(Balance.START_WORKERS * workerType.supplyCost());
    }
}

package com.whim.cardwoven.engine;

import java.util.List;

import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Views.TileView;
import com.whim.cardwoven.domain.Building;
import com.whim.cardwoven.domain.FactionProfile;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.GridMap;
import com.whim.cardwoven.domain.PlayerState;
import com.whim.cardwoven.domain.Resources;

/**
 * Computes and applies each player's per-turn resource yields.
 *
 * Per owned building the yield is:
 *   - the building's own {@code goldYield()} / {@code commandYield()} (which the
 *     domain already derives from its base value plus attachment buffs),
 *   - a terrain modifier keyed off the tile it sits on,
 *   - a faction modifier (Babylon's per-building yield bonus).
 * Idol bonus draws are surfaced separately via {@link #bonusDrawsFor} and consumed
 * by the DRAW phase, not credited as a resource here.
 *
 * The only mutation is crediting the player's {@link Resources} in
 * {@link #applyYields}; everything else is read through the domain model.
 */
final class EconomyCalculator {

    private final GameState state;

    EconomyCalculator(GameState state) {
        this.state = state;
    }

    /**
     * Credits {@code player}'s resources with this turn's yields and returns a
     * short human-readable summary of what was produced.
     */
    String applyYields(PlayerState player) {
        int gold = 0;
        int command = 0;

        GridMap map = state.gridMap();
        List<Building> buildings = map.buildingsOwnedBy(player.index());
        FactionProfile profile = player.profile();

        for (int i = 0; i < buildings.size(); i++) {
            Building b = buildings.get(i);

            // Domain-computed base + attachment yields.
            gold += b.goldYield();
            command += b.commandYield();

            // Terrain modifier.
            TileView tile = map.tile(b.row(), b.col());
            if (tile != null) {
                gold += terrainGoldBonus(tile.terrain(), b.type());
                command += terrainCommandBonus(tile.terrain());
            }

            // Faction modifier: Babylon rewards every building.
            gold += profile.buildingYieldBonus();
        }

        Resources res = player.resources();
        if (gold != 0) {
            res.add(ResourceType.GOLD, gold);
        }
        if (command != 0) {
            res.add(ResourceType.COMMAND_POINTS, command);
        }
        return "+" + gold + " gold, +" + command + " command from "
                + buildings.size() + " building(s)";
    }

    /** Total extra cards this player draws this turn from Idol attachments. */
    int bonusDrawsFor(PlayerState player) {
        int draws = 0;
        List<Building> buildings = state.gridMap().buildingsOwnedBy(player.index());
        for (int i = 0; i < buildings.size(); i++) {
            draws += buildings.get(i).bonusDraw();
        }
        return draws;
    }

    private static int terrainGoldBonus(TerrainType terrain, BuildingType type) {
        if (terrain == null) {
            return 0;
        }
        switch (terrain) {
            case PLAINS:
                // Farms thrive on plains.
                return type == BuildingType.FARM ? 2 : 1;
            case WATER:
                // Ports exploit water tiles they neighbour.
                return type == BuildingType.PORT ? 2 : 0;
            default:
                return 0;
        }
    }

    private static int terrainCommandBonus(TerrainType terrain) {
        if (terrain == null) {
            return 0;
        }
        // Mountains project military/command power.
        return terrain == TerrainType.MOUNTAIN ? 1 : 0;
    }

    /** Whether the given faction gains a per-building yield bonus. */
    static boolean isBuildingFocused(Faction faction) {
        return faction == Faction.BABYLON;
    }
}

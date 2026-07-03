package com.whim.cardwoven.engine;

import com.whim.cardwoven.api.ActionResult;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.TileView;
import com.whim.cardwoven.domain.Building;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.GridMap;
import com.whim.cardwoven.domain.PlayerState;
import com.whim.cardwoven.domain.Tile;

/**
 * Deterministic combat: a MILITARY card's attack is compared against the target's
 * strength. Targets are either neutral Orcish raiders menacing a tile
 * (per-tile raiderStrength) or a rival player's building (defended by its
 * defense value). No randomness — same inputs always produce the same outcome.
 *
 * Mutates the map (raider strength, building removal) and records cumulative
 * tallies into {@link EngineStats} for victory tracking. Logs every outcome.
 */
final class CombatResolver {

    private final GameState state;
    private final EngineStats stats;

    CombatResolver(GameState state, EngineStats stats) {
        this.state = state;
        this.stats = stats;
    }

    /**
     * Resolve {@code attacker} committing {@code card} against the tile at
     * (row,col). The caller is responsible for removing/discarding the card.
     */
    ActionResult resolve(PlayerState attacker, CardView card, int row, int col) {
        GridMap map = state.gridMap();
        if (!inBounds(map, row, col)) {
            return ActionResult.fail("Target tile is off the map");
        }
        TileView tile = map.tile(row, col);
        int attack = card.attack();
        if (attack <= 0) {
            return ActionResult.fail(card.name() + " has no attack strength");
        }

        int raiders = tile.raiderStrength();
        BuildingView occupant = tile.building();
        boolean enemyBuilding = occupant != null
                && occupant.ownerPlayerIndex() != attacker.index();

        // Raiders take priority — they menace whoever is nearby.
        if (raiders > 0) {
            return fightRaiders(attacker, card, map, row, col, raiders, attack);
        }
        if (enemyBuilding) {
            return fightBuilding(attacker, card, map, row, col, occupant, attack);
        }
        return ActionResult.fail("No raiders or enemy building at ("
                + row + "," + col + ")");
    }

    private ActionResult fightRaiders(PlayerState attacker, CardView card,
                                      GridMap map, int row, int col,
                                      int raiders, int attack) {
        Tile tile = map.tileAt(row, col);
        if (attack >= raiders) {
            tile.setRaiderStrength(0);
            stats.raidersDefeated[attacker.index()] += 1;
            String msg = attacker.name() + "'s " + card.name()
                    + " crushed raiders (" + attack + " vs " + raiders + ") at ("
                    + row + "," + col + ")";
            state.log(msg);
            return ActionResult.ok(msg);
        }
        int left = raiders - attack;
        tile.setRaiderStrength(left);
        String msg = attacker.name() + "'s " + card.name() + " weakened raiders to "
                + left + " at (" + row + "," + col + ")";
        state.log(msg);
        return ActionResult.ok(msg);
    }

    private ActionResult fightBuilding(PlayerState attacker, CardView card,
                                       GridMap map, int row, int col,
                                       BuildingView target, int attack) {
        int defense = target.defense();
        if (attack >= defense) {
            Building concrete = map.tileAt(row, col).buildingConcrete();
            if (concrete != null) {
                map.removeBuilding(concrete);
            }
            stats.buildingsDestroyed[attacker.index()] += 1;
            String msg = attacker.name() + "'s " + card.name() + " razed a "
                    + target.type().display() + " (" + attack + " vs def " + defense
                    + ") at (" + row + "," + col + ")";
            state.log(msg);
            return ActionResult.ok(msg);
        }
        String msg = attacker.name() + "'s " + card.name() + " was repelled by a "
                + target.type().display() + " (" + attack + " vs def " + defense + ")";
        state.log(msg);
        // Attack still "happened" (card spent); report as a non-fatal outcome.
        return ActionResult.ok(msg);
    }

    private static boolean inBounds(GridMap map, int row, int col) {
        return row >= 0 && col >= 0 && row < map.rows() && col < map.cols();
    }
}

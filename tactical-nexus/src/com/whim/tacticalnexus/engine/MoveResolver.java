package com.whim.tacticalnexus.engine;

import com.whim.tacticalnexus.domain.Door;
import com.whim.tacticalnexus.domain.Enemy;
import com.whim.tacticalnexus.domain.Entity;
import com.whim.tacticalnexus.domain.GemType;
import com.whim.tacticalnexus.domain.GridMap;
import com.whim.tacticalnexus.domain.KeyColor;
import com.whim.tacticalnexus.domain.KeyItem;
import com.whim.tacticalnexus.domain.Player;
import com.whim.tacticalnexus.domain.Position;
import com.whim.tacticalnexus.domain.StairDirection;
import com.whim.tacticalnexus.domain.Staircase;
import com.whim.tacticalnexus.domain.StatGem;
import com.whim.tacticalnexus.state.GameState;

/**
 * Pure movement/interaction resolver. Given a {@link GameState} and a movement
 * vector, computes the resulting state and a description, without RNG or Swing.
 *
 * <p>The vector components are expected in {-1,0,1}. The resolver inspects the
 * single target cell adjacent to the player and applies the contract's
 * interaction rules.
 */
public final class MoveResolver {

    private MoveResolver() {
    }

    public static MoveOutcome resolve(GameState state, int dRow, int dCol) {
        Player player = state.player();
        GridMap floor = state.currentFloor();
        Position from = player.position();
        Position target = from.translate(dRow, dCol);

        // Out of bounds → blocked.
        if (!floor.inBounds(target)) {
            return blocked(state, "Blocked: edge of the map.");
        }

        Entity entity = floor.at(target);

        // Empty cell → simply move.
        if (entity == null) {
            GameState next = state.withPlayer(player.withPosition(target));
            return new MoveOutcome(next, true, "Moved.");
        }

        if (entity instanceof com.whim.tacticalnexus.domain.Wall) {
            return blocked(state, "Blocked: a wall.");
        }

        if (entity instanceof Door) {
            return resolveDoor(state, floor, player, target, (Door) entity);
        }

        if (entity instanceof KeyItem) {
            return resolveKey(state, floor, player, target, (KeyItem) entity);
        }

        if (entity instanceof StatGem) {
            return resolveGem(state, floor, player, target, (StatGem) entity);
        }

        if (entity instanceof Enemy) {
            return resolveEnemy(state, floor, player, target, (Enemy) entity);
        }

        if (entity instanceof Staircase) {
            return resolveStair(state, player, (Staircase) entity);
        }

        // Unknown/blocking entity → treat as blocked, never advance.
        if (entity.blocksMovement()) {
            return blocked(state, "Blocked.");
        }
        // Unknown non-blocking entity → step onto it.
        GameState next = state.withPlayer(player.withPosition(target));
        return new MoveOutcome(next, true, "Moved.");
    }

    private static MoveOutcome resolveDoor(GameState state, GridMap floor, Player player,
                                           Position target, Door door) {
        KeyColor color = door.color();
        if (player.keyCount(color) < 1) {
            return blocked(state, "Locked: need a " + colorName(color) + " key.");
        }
        // Spend one key, open the door in place. Player does NOT advance.
        Player after = player.addKey(color, -1);
        GridMap opened = floor.without(target);
        GameState next = state
                .withPlayer(after)
                .withFloor(state.floorIndex(), opened);
        return new MoveOutcome(next, true, "Opened a " + colorName(color) + " door.");
    }

    private static MoveOutcome resolveKey(GameState state, GridMap floor, Player player,
                                          Position target, KeyItem key) {
        KeyColor color = key.color();
        Player after = player.addKey(color, 1).withPosition(target);
        GridMap cleared = floor.without(target);
        GameState next = state
                .withPlayer(after)
                .withFloor(state.floorIndex(), cleared);
        return new MoveOutcome(next, true, "Picked up a " + colorName(color) + " key.");
    }

    private static MoveOutcome resolveGem(GameState state, GridMap floor, Player player,
                                          Position target, StatGem gem) {
        int amount = gem.amount();
        GemType type = gem.gem();
        Player boosted;
        String what;
        if (type == GemType.HP) {
            boosted = player.addStats(amount, 0, 0);
            what = "+" + amount + " HP";
        } else if (type == GemType.ATK) {
            boosted = player.addStats(0, amount, 0);
            what = "+" + amount + " ATK";
        } else { // DEF
            boosted = player.addStats(0, 0, amount);
            what = "+" + amount + " DEF";
        }
        Player after = boosted.withPosition(target);
        GridMap cleared = floor.without(target);
        GameState next = state
                .withPlayer(after)
                .withFloor(state.floorIndex(), cleared);
        return new MoveOutcome(next, true, "Gem: " + what + ".");
    }

    private static MoveOutcome resolveEnemy(GameState state, GridMap floor, Player player,
                                            Position target, Enemy enemy) {
        CombatResult combat = CombatCalculator.resolve(player, enemy);
        if (!combat.canFight()) {
            return blocked(state, "Cannot harm " + enemy.name() + " (ATK too low).");
        }
        if (!combat.survivable()) {
            return blocked(state, "Fighting " + enemy.name() + " would be lethal.");
        }
        Player after = player
                .withHp(player.hp() - combat.hpLost())
                .addGold(enemy.goldReward())
                .addExp(enemy.expReward())
                .withPosition(target);
        GridMap cleared = floor.without(target);
        GameState next = state
                .withPlayer(after)
                .withFloor(state.floorIndex(), cleared);
        return new MoveOutcome(next, true,
                "Defeated " + enemy.name() + " (-" + combat.hpLost() + " HP, +"
                        + enemy.goldReward() + "g, +" + enemy.expReward() + "xp).");
    }

    private static MoveOutcome resolveStair(GameState state, Player player, Staircase stair) {
        StairDirection dir = stair.direction();
        int delta = (dir == StairDirection.UP) ? 1 : -1;
        int destIndex = state.floorIndex() + delta;

        if (destIndex < 0 || destIndex >= state.floors().size()) {
            return blocked(state, "No floor that way.");
        }

        GridMap destFloor = state.floors().get(destIndex);
        // Land on the opposite-direction stair on the destination floor.
        StairDirection opposite = (dir == StairDirection.UP)
                ? StairDirection.DOWN : StairDirection.UP;
        Position landing = destFloor.findStair(opposite);
        if (landing == null) {
            // Fallback: any same-direction stair, else keep current position.
            landing = destFloor.findStair(dir);
        }
        if (landing == null) {
            landing = player.position();
        }

        GameState next = state
                .withFloorIndex(destIndex)
                .withPlayer(player.withPosition(landing));
        String way = (dir == StairDirection.UP) ? "up" : "down";
        return new MoveOutcome(next, true, "Took the stairs " + way + " to floor " + destIndex + ".");
    }

    private static MoveOutcome blocked(GameState state, String message) {
        return new MoveOutcome(state, false, message);
    }

    private static String colorName(KeyColor c) {
        switch (c) {
            case YELLOW: return "yellow";
            case BLUE:   return "blue";
            case RED:    return "red";
            default:     return c.name().toLowerCase();
        }
    }
}
